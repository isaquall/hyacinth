package me.isaquall.hyacinth.schematic;

import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.litematica.util.PositionUtils;
import me.isaquall.hyacinth.dithering.algorithm.DitheringAlgorithm;
import me.isaquall.hyacinth.mixin.LitematicaSchematicMixin;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SchematicWriter {

    public static void createSchematic(DitheringAlgorithm.Pixel[][] pixels) {
        TerrainSlice[] slices = new TerrainSlice[pixels.length];
        LitematicaSchematic schematic = LitematicaSchematicMixin.newLitematicaSchematic(null);
//        schematic.setSubRegionPositions(boxes, area.getEffectiveOrigin());
//        schematic.setSubRegionSizes(boxes);
        schematic.getMetadata().setAuthor(MinecraftClient.getInstance().player.getName().getString());
        schematic.getMetadata().setName("Mapart Schematic");
        schematic.getMetadata().setRegionCount(1);

        Box box = new Box(new BlockPos(0, 0, 0), new BlockPos(129, 129, 129), "map");
        schematic.getMetadata().setTotalVolume(PositionUtils.getTotalVolume(List.of(box)));
        schematic.getMetadata().setEnclosingSize(PositionUtils.getEnclosingAreaSize(List.of(box)));
        ((LitematicaSchematicMixin) schematic).getSubRegionSizes().put("map", new BlockPos(129, 129, 129));
        ((LitematicaSchematicMixin) schematic).getSubRegionPositions().put("map", new BlockPos(0, 0, 0));
        ((LitematicaSchematicMixin) schematic).getTileEntities().put("map", new HashMap<>());

        schematic.getMetadata().setSchematicVersion(7);
        schematic.getMetadata().setMinecraftDataVersion(LitematicaSchematic.MINECRAFT_DATA_VERSION);
        schematic.getMetadata().setFileType(FileType.LITEMATICA_SCHEMATIC);
        LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(Math.abs(box.getSize().getX()), Math.abs(box.getSize().getY()), Math.abs(box.getSize().getZ()));

        ((LitematicaSchematicMixin) schematic).getBlockContainers().put("map", container);

        schematic.getMetadata().setTotalBlocks(129*129*2);

        SchematicHolder.getInstance().addSchematic(schematic, false);

        for (int x = 0; x < pixels.length; x++) {
            DitheringAlgorithm.Pixel[] slice = pixels[x];
            TerrainSlice terrainSlice = new TerrainSlice(slice);
            slices[x] = terrainSlice;
            terrainSlice.writeTerrain(container, x);
        }
    }

    private static class TerrainSlice {

        private final ArrayList<PlannedBlock>[] blocks = new ArrayList[130];
        private final DitheringAlgorithm.Pixel[] slice;

        public TerrainSlice(DitheringAlgorithm.Pixel[] slice) {
            this.slice = slice;
            for (int i = 0; i < 130; i++) {
                blocks[i] = new ArrayList<>();
            }
        }

        public void writeTerrain(LitematicaBlockStateContainer container, int x) {
            int currentHeight = 0;
            for (int z = 127; z >= 0; z--) { // TODO add support for larger maps
                ArrayList<PlannedBlock> plannedBlocks = blocks[z];
                DitheringAlgorithm.Pixel pixel = slice[z];
                BlockState blockState = pixel.blockState();
                int brightness = pixel.brightness();
                PlannedBlock block = new PlannedBlock(blockState, currentHeight);
                plannedBlocks.add(block);
                plannedBlocks.add(new PlannedBlock(Blocks.COBBLESTONE.getDefaultState(), currentHeight - 1));
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
            for (int z = 0; z < 130; z++) {
                for (PlannedBlock block : blocks[z]) {
                    if (block.height() < minHeight) {
                        minHeight = block.height();
                    }
                }
            }

            if (minHeight < 0) {
                for (int z = 0; z < 130; z++) {
                    for (PlannedBlock block : blocks[z]) {
                        block.height(block.height() - minHeight);
                    }
                }
            }

            for (int z = 0; z < 130; z++) {
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
