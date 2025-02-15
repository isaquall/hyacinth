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

    public static void createSchematic(DitheringAlgorithm.Pixel[][] pixels, String schematicName, SupportMode mode) {
        TerrainSlice[] slices = new TerrainSlice[pixels.length];
        LitematicaSchematic schematic = LitematicaSchematicMixin.newLitematicaSchematic(null);
        schematic.getMetadata().setAuthor(MinecraftClient.getInstance().player.getName().getString());
        schematic.getMetadata().setName(schematicName);
        schematic.getMetadata().setRegionCount(1);
        Box box = new Box(new BlockPos(0, 0, 0), new BlockPos(pixels.length, pixels[0].length, 130), "map");
        schematic.getMetadata().setTotalVolume(PositionUtils.getTotalVolume(List.of(box)));
        schematic.getMetadata().setEnclosingSize(PositionUtils.getEnclosingAreaSize(List.of(box)));
        ((LitematicaSchematicMixin) schematic).getSubRegionSizes().put("map", new BlockPos(pixels.length, pixels[0].length, 130));
        ((LitematicaSchematicMixin) schematic).getSubRegionPositions().put("map", new BlockPos(0, 0, 0));
        ((LitematicaSchematicMixin) schematic).getTileEntities().put("map", new HashMap<>());
        schematic.getMetadata().setSchematicVersion(7);
        schematic.getMetadata().setMinecraftDataVersion(LitematicaSchematic.MINECRAFT_DATA_VERSION);
        schematic.getMetadata().setFileType(FileType.LITEMATICA_SCHEMATIC);
        schematic.getMetadata().setTotalBlocks(pixels.length*pixels[0].length*2);
        SchematicHolder.getInstance().addSchematic(schematic, false);
        schematic.getMetadata().setTimeModifiedToNow();

        LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(Math.abs(box.getSize().getX()), Math.abs(box.getSize().getY()), Math.abs(box.getSize().getZ()));
        ((LitematicaSchematicMixin) schematic).getBlockContainers().put("map", container);

        for (int x = 0; x < pixels.length; x++) {
            DitheringAlgorithm.Pixel[] slice = pixels[x];
            TerrainSlice terrainSlice = new TerrainSlice(slice);
            slices[x] = terrainSlice;
            terrainSlice.writeTerrain(container, x, mode);
        }

        File schemDir = new File(MinecraftClient.getInstance().runDirectory + File.separator + "schematics" + File.separator);
        schematic.writeToFile(schemDir, schematicName, false);
        SchematicHolder.getInstance().getOrLoad(new File(schemDir + schematicName));
    }

    private static class TerrainSlice {

        private final ArrayList<PlannedBlock>[] blocks;
        private final DitheringAlgorithm.Pixel[] slice;
        private final int length;

        public TerrainSlice(DitheringAlgorithm.Pixel[] slice) {
            this.length = slice.length + 2;
            this.blocks = new ArrayList[length];
            this.slice = slice;
            for (int i = 0; i < length; i++) {
                blocks[i] = new ArrayList<>();
            }
        }

        public void writeTerrain(LitematicaBlockStateContainer container, int x, SupportMode mode) {
            int currentHeight = 0;
            for (int z = slice.length - 1; z >= 0; z--) { // TODO add support for larger maps
                ArrayList<PlannedBlock> plannedBlocks = blocks[z + 1];
                DitheringAlgorithm.Pixel pixel = slice[z];
                BlockState blockState = pixel.blockState();
                int brightness = pixel.brightness();
                PlannedBlock block = new PlannedBlock(blockState, currentHeight);
                plannedBlocks.add(block);
                switch (mode) {
                    case ALWAYS -> plannedBlocks.add(new PlannedBlock(Blocks.COBBLESTONE.getDefaultState(), currentHeight - 1));
                    case ONLY_REQUIRED -> {
                        if (TagUtil.isIn(HyacinthBlockTagProvider.REQUIRES_SUPPORT, blockState.getBlock())) plannedBlocks.add(new PlannedBlock(Blocks.COBBLESTONE.getDefaultState(), currentHeight - 1));
                    }
                }
                if (brightness == 180) {
                    currentHeight++;
                } else if (brightness == 255) {
                    currentHeight--;
                }
            }

            // add one at the end for proper shading
            blocks[0].add(new PlannedBlock(Blocks.COBBLESTONE.getDefaultState(), currentHeight));

            // fix negative height
            int minHeight = 0;
            for (int z = 0; z < length; z++) {
                for (PlannedBlock block : blocks[z]) {
                    if (block.height() < minHeight) {
                        minHeight = block.height();
                    }
                }
            }

            if (minHeight < 0) {
                for (int z = 0; z < length; z++) {
                    for (PlannedBlock block : blocks[z]) {
                        block.height(block.height() - minHeight);
                    }
                }
            }

            for (int z = 0; z < length; z++) {
                for (PlannedBlock block : blocks[z]) {
                    container.set(x, block.height(), z, block.blockState());
                }
            }
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
