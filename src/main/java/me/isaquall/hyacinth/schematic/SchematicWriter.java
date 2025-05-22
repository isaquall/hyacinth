package me.isaquall.hyacinth.schematic;

import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.litematica.util.PositionUtils;
import me.isaquall.hyacinth.datagen.HyacinthBlockTagProvider;
import me.isaquall.hyacinth.dithering.algorithm.DitheringAlgorithm;
import me.isaquall.hyacinth.mixin.LitematicaSchematicMixin;
import net.fabricmc.fabric.api.tag.convention.v2.TagUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SchematicWriter {

    public static void createSchematic(DitheringAlgorithm.Pixel[][] pixels, String schematicName, SupportMode supportMode, StaircaseMode staircaseMode) {
        TerrainSlice[] slices = new TerrainSlice[pixels.length];
        LitematicaSchematic schematic = LitematicaSchematicMixin.newLitematicaSchematic(null);
        schematic.getMetadata().setAuthor(MinecraftClient.getInstance().player.getName().getString());
        schematic.getMetadata().setName(schematicName);
        schematic.getMetadata().setRegionCount(1);

        int length = pixels.length;
        int width = pixels[0].length + 1; // Account for noobline

        Box box = new Box(new BlockPos(0, 0, 0), new BlockPos(length, 130, width), "map");
        schematic.getMetadata().setTotalVolume(PositionUtils.getTotalVolume(List.of(box)));
        schematic.getMetadata().setEnclosingSize(PositionUtils.getEnclosingAreaSize(List.of(box)));
        ((LitematicaSchematicMixin) schematic).getSubRegionSizes().put("map", new BlockPos(length, 130, width));
        ((LitematicaSchematicMixin) schematic).getSubRegionPositions().put("map", new BlockPos(0, 0, 0));
        ((LitematicaSchematicMixin) schematic).getTileEntities().put("map", new HashMap<>());
        schematic.getMetadata().setSchematicVersion(7);
        schematic.getMetadata().setMinecraftDataVersion(LitematicaSchematic.MINECRAFT_DATA_VERSION);
        schematic.getMetadata().setFileType(FileType.LITEMATICA_SCHEMATIC);
        SchematicHolder.getInstance().addSchematic(schematic, false);
        schematic.getMetadata().setTimeModifiedToNow();

        LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(Math.abs(box.getSize().getX()), Math.abs(box.getSize().getY()), Math.abs(box.getSize().getZ()));
        ((LitematicaSchematicMixin) schematic).getBlockContainers().put("map", container);

        int totalBlocks = 0;

        for (int x = 0; x < pixels.length; x++) {
            DitheringAlgorithm.Pixel[] slice = pixels[x];
            TerrainSlice terrainSlice = new TerrainSlice(slice);
            slices[x] = terrainSlice;
            totalBlocks += terrainSlice.writeTerrain(container, x, supportMode, staircaseMode);
        }

        schematic.getMetadata().setTotalBlocks(totalBlocks);

        File schemDir = new File(MinecraftClient.getInstance().runDirectory + File.separator + "schematics" + File.separator);
        schematic.writeToFile(schemDir, schematicName, false);
        SchematicHolder.getInstance().getOrLoad(new File(schemDir + schematicName));
    }

    private static class TerrainSlice {

        private final ArrayList<PlannedBlock>[] blocks;
        private final DitheringAlgorithm.Pixel[] slice;

        public TerrainSlice(DitheringAlgorithm.Pixel[] slice) {
            this.blocks = new ArrayList[slice.length + 1]; // This accounts for the noobline
            this.slice = slice;
            for (int i = 0; i < blocks.length; i++) {
                blocks[i] = new ArrayList<>();
            }
        }

        public int writeTerrain(LitematicaBlockStateContainer container, int x, SupportMode supportMode, StaircaseMode staircaseMode) {
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
            int minHeight = 0;
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

            int totalBlocks = 0;
            for (int z = 0; z < blocks.length; z++) {
                for (PlannedBlock block : blocks[z]) {
                    container.set(x, block.height(), z, block.blockState());
                    totalBlocks++;
                }
            }
            return totalBlocks;
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

        private int highestHeightAt(int z) { // TODO uhhhh is this necessary
            int highestHeight = Integer.MIN_VALUE;
            for (PlannedBlock block : blocks[z]) {
                if (block.height() > highestHeight) {
                    highestHeight = block.height();
                }
            }
            return highestHeight;
        }
    }

    private static class PlannedBlock {
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
