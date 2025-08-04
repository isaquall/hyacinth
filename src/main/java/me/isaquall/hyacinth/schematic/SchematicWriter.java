package me.isaquall.hyacinth.schematic;

import me.isaquall.hyacinth.block_palette.BlockPalette;
import me.isaquall.hyacinth.datagen.HyacinthBlockTagProvider;
import me.isaquall.hyacinth.dithering.algorithm.DitheringAlgorithm;
import me.isaquall.hyacinth.schematic.litematica.LitematicaSchematicWriter;
import net.fabricmc.fabric.api.tag.convention.v2.TagUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class SchematicWriter {

    public static void createSchematic(HashMap<BlockPalette, BlockState> selectedBlocks, DitheringAlgorithm.Pixel[][] pixels, SupportMode supportMode, StaircaseMode staircaseMode) {
        TerrainSlice[] slices = new TerrainSlice[pixels.length];

        for (int x = 0; x < pixels.length; x++) {
            DitheringAlgorithm.Pixel[] slice = pixels[x];
            TerrainSlice terrainSlice = new TerrainSlice(x, slice);
            slices[x] = terrainSlice;
            terrainSlice.writeTerrain(supportMode, staircaseMode);
        }

        File dir = new File(FabricLoader.getInstance().getGameDir().resolve("schematics").toString());
        dir.mkdirs();
        File schematicFile = new File(dir, "hyacinth-output.litematic");

        try (FileOutputStream os = new FileOutputStream(schematicFile)) {
            NbtCompound schematic = LitematicaSchematicWriter.createSchematic(slices, selectedBlocks);
            NbtIo.writeCompressed(schematic, os);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class TerrainSlice {

        public final ArrayList<PlannedBlock>[] blocks;
        private final DitheringAlgorithm.Pixel[] slice;
        public final int length;
        public final int x;

        public TerrainSlice(int x, DitheringAlgorithm.Pixel[] slice) {
            this.blocks = new ArrayList[slice.length + 1];
            this.x = x;
            this.slice = slice;
            for (int i = 0; i < blocks.length; i++) {
                blocks[i] = new ArrayList<>();
            }
            this.length = slice.length;
        }

        public void writeTerrain(SupportMode supportMode, StaircaseMode staircaseMode) {
            int currentHeight = 0;
            for (int z = slice.length - 1; z >= 0; z--) {
                ArrayList<PlannedBlock> plannedBlocks = blocks[z + 1];
                DitheringAlgorithm.Pixel pixel = slice[z];
                BlockState blockState = pixel.blockState();
                int brightness = pixel.brightness();
                PlannedBlock block = new PlannedBlock(blockState, currentHeight);
                plannedBlocks.add(block);

                switch (supportMode) {
                    case ALWAYS -> plannedBlocks.add(new PlannedBlock(Blocks.COBBLESTONE.getDefaultState(), currentHeight - 1));
                    case ONLY_REQUIRED -> {
                        if (TagUtil.isIn(HyacinthBlockTagProvider.REQUIRES_SUPPORT, blockState.getBlock())) plannedBlocks.add(new PlannedBlock(Blocks.COBBLESTONE.getDefaultState(), currentHeight - 1));
                    }
                }

                if (staircaseMode != StaircaseMode.FLAT) {
                    if (brightness == 180) {
                        currentHeight++;
                    } else if (brightness == 255) {
                        currentHeight--;
                    }
                }
            }

            // add one at the beginning (slices are traversed backwards) for proper shading
            blocks[0].add(new PlannedBlock(Blocks.COBBLESTONE.getDefaultState(), currentHeight));

            // fix negative height
            int minHeight = -1;
            if (supportMode == SupportMode.NEVER) minHeight = 0;
            for (ArrayList<PlannedBlock> plannedBlocks : blocks) {
                for (PlannedBlock block : plannedBlocks) {
                    if (block.height() < minHeight) {
                        minHeight = block.height();
                    }
                }
            }

            if (minHeight < 0) {
                for (ArrayList<PlannedBlock> plannedBlocks : blocks) {
                    for (PlannedBlock block : plannedBlocks) {
                        block.height(block.height() - minHeight);
                    }
                }
            }

            if (staircaseMode == StaircaseMode.VALLEY) groundTerrain(0, blocks.length);
        }

        // startZ inclusive, endZ exclusive
        private void groundTerrain(int startZ, int endZ) {
            if (startZ >= endZ) {
                return;
            }

            // find lowest block
            int lowestHeightZ = -1;
            int lowestHeight = Integer.MAX_VALUE;
            for (int z = startZ; z < endZ; z++) {
                int height = lowestHeightAt(z);
                if (height < lowestHeight) {
                    lowestHeight = height;
                    lowestHeightZ = z;
                }
            }
            // explore in both directions, but stay within [startZ, endZ[
            int currentHeight = lowestHeight;

            int includeLowZ = lowestHeightZ; // inclusive
            int includeHighZ = lowestHeightZ + 1; // exclusive
            // explore towards positive z (south)
            for (int z = lowestHeightZ + 1; z < endZ; z++) {
                PlannedBlock currentBlock = getHighestBlockAt(z);
                // cut before blocks go down again
                if (currentBlock.height() < currentHeight) {
                    break;
                }
                // include this block
                currentHeight = currentBlock.height();
                includeHighZ = z + 1;
            }
            // explore towards negative z (north)
            currentHeight = lowestHeight;
            for (int z = lowestHeightZ - 1; z >= startZ; z--) {
                PlannedBlock currentBlock = getHighestBlockAt(z);
                PlannedBlock previousBlock = getHighestBlockAt(z + 1);
                // cut before blocks go down again
                if (currentBlock.height() < currentHeight) {
                    break;
                }
                // include this block
                currentHeight = currentBlock.height();
                includeLowZ = z;
            }

            // move explored blocks down
            changeHeight(includeLowZ, includeHighZ, -lowestHeight);

            // recursively ground the of [startZ, endZ[
            groundTerrain(startZ, includeLowZ);
            groundTerrain(includeHighZ, endZ);
        }

        // startZ inclusive, endZ exclusive
        private void changeHeight(int minZ, int maxZ, int heightOffset) {
            for (int i = minZ; i < maxZ; i++) {
                for (PlannedBlock block : blocks[i]) {
                    block.height(block.height() + heightOffset);
                }
            }
        }

        private PlannedBlock getHighestBlockAt(int z) {
            int height = highestHeightAt(z);
            for (PlannedBlock block : blocks[z]) {
                if (block.height() == height) {
                    return block;
                }
            }
            return null;
        }

        private int lowestHeightAt(int z) {
            int lowestHeight = Integer.MAX_VALUE;
            for (PlannedBlock block : blocks[z]) {
                if (block.height() < lowestHeight) {
                    lowestHeight = block.height();
                }
            }
            return lowestHeight;
        }

        private int highestHeightAt(int z) {
            int highestHeight = Integer.MIN_VALUE;
            for (PlannedBlock block : blocks[z]) {
                if (block.height() > highestHeight) {
                    highestHeight = block.height();
                }
            }
            return highestHeight;
        }

        public int highestHeight() {
            int highestHeight = Integer.MIN_VALUE;
            for (ArrayList<PlannedBlock> list : blocks) {
                for (PlannedBlock block : list) {
                    if (block.height() > highestHeight) {
                        highestHeight = block.height();
                    }
                }
            }
            return highestHeight;
        }
    }

    public static class PlannedBlock {
        private BlockState blockState;
        private int height;

        public PlannedBlock(BlockState blockState, int height) {
            this.blockState = blockState;
            this.height = height;
        }

        public BlockState blockState() {
            return blockState;
        }

        public PlannedBlock blockState(BlockState blockState) {
            this.blockState = blockState;
            return this;
        }

        public int height() {
            return height;
        }

        public PlannedBlock height(int height) {
            this.height = height;
            return this;
        }
    }
}
