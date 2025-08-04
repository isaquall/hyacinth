package me.isaquall.hyacinth.schematic.litematica;

import me.isaquall.hyacinth.block_palette.BlockPalette;
import me.isaquall.hyacinth.schematic.SchematicWriter;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtLongArray;

import java.util.HashMap;
import java.util.Map;

/*
    Adapted from: https://github.com/sakura-ryoko/litematica/blob/1.21.8/src/main/java/fi/dy/masa/litematica/schematic/LitematicaSchematic.java
 */
public class LitematicaSchematicWriter {

    public static NbtCompound createSchematic(SchematicWriter.TerrainSlice[] slices, HashMap<BlockPalette, BlockState> selectedBlocks) {
        int highestBlock = Integer.MIN_VALUE;
        for (SchematicWriter.TerrainSlice slice : slices) {
            if (slice.highestHeight() > highestBlock) {
                highestBlock = slice.highestHeight();
            }
        }
        int uniqueBlocks = 0;
        for (Map.Entry<BlockPalette, BlockState> pair : selectedBlocks.entrySet()) {
            if (pair.getValue() != Blocks.BARRIER.getDefaultState()) {
                uniqueBlocks++;
            }
        }
        BlockStateContainer container = new BlockStateContainer(slices.length, highestBlock + 1, slices[0].length + 1, uniqueBlocks);

        int totalBlocks = 0;
        for (SchematicWriter.TerrainSlice slice : slices) {
            for (int z = 0; z < slice.blocks.length; z++) {
                for (SchematicWriter.PlannedBlock block : slice.blocks[z]) {
                    container.set(slice.x, block.height(), z, block.blockState());
                    totalBlocks++;
                }
            }
        }

        NbtCompound nbt = new NbtCompound();
        nbt.putInt("MinecraftDataVersion", SharedConstants.getGameVersion().getSaveVersion().getId());
        nbt.putInt("Version", 7);
        nbt.putInt("SubVersion", 1);
        nbt.put("Metadata", writeMetadata(container, totalBlocks));
        nbt.put("Regions", writeSubregion(container));
        return nbt;
    }

    public static NbtCompound writeMetadata(BlockStateContainer container, int totalBlocks) {
        NbtCompound nbt = new NbtCompound();

        nbt.putString("Name", "Hyacinth Map");
        nbt.putString("Author", MinecraftClient.getInstance().player.getName().getString());
//      nbt.putString("Description", this.description);

        nbt.putInt("RegionCount", 1);
        nbt.putInt("TotalVolume", (int) container.totalVolume);
        nbt.putInt("TotalBlocks", totalBlocks);
        nbt.putLong("TimeCreated", System.currentTimeMillis());

//      nbt.putLong("TimeModified", this.timeModified);

        nbt.put("EnclosingSize", writePosition(container.sizeX, container.sizeY, container.sizeZ));
//      nbt.putIntArray("PreviewImageData", this.thumbnailPixelData);
        return nbt;
    }

    public static NbtCompound writeSubregion(BlockStateContainer container) {
        NbtCompound tag = new NbtCompound();
        tag.put("BlockStatePalette", container.getPalette().writeToNBT());
        tag.put("BlockStates", new NbtLongArray(container.getBackingLongArray()));

        tag.put("Position", writePosition(0, 0, 0));
        tag.put("Size", writePosition(container.sizeX, container.sizeY, container.sizeZ));
        NbtCompound wrapper = new NbtCompound();
        wrapper.put("map", tag);
        return wrapper;
    }

    public static NbtCompound writePosition(int x, int y, int z) {
        NbtCompound tag = new NbtCompound();
        tag.putInt("x", x);
        tag.putInt("y", y);
        tag.putInt("z", z);
        return tag;
    }
}
