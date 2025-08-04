package me.isaquall.hyacinth.schematic.litematica;

import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.collection.Int2ObjectBiMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/*
    From: https://github.com/sakura-ryoko/litematica/blob/1.21.8/src/main/java/fi/dy/masa/litematica/schematic/container/LitematicaBlockStatePaletteHashMap.java
 */
@ApiStatus.Internal
class BlockStatePalette {

    private final Int2ObjectBiMap<BlockState> statePaletteMap;

    public BlockStatePalette(int bits) {
        this.statePaletteMap = Int2ObjectBiMap.create(1 << bits);
    }

    public int idFor(BlockState state) {
        int i = this.statePaletteMap.getRawId(state);

        if (i == -1) {
            i = this.statePaletteMap.add(state);
        }

        return i;
    }

    @Nullable
    public BlockState getBlockState(int indexKey) {
        return this.statePaletteMap.get(indexKey);
    }

    public int getPaletteSize() {
        return this.statePaletteMap.size();
    }

    public boolean setMapping(List<BlockState> list) {
        this.statePaletteMap.clear();

        for (BlockState blockState : list) {
            this.statePaletteMap.add(blockState);
        }

        return true;
    }

    public NbtList writeToNBT() {
        NbtList tagList = new NbtList();

        for (int id = 0; id < this.statePaletteMap.size(); ++id) {
            BlockState state = this.statePaletteMap.get(id);

            if (state == null) {
                state = LitematicaBlockStateContainer.AIR_BLOCK_STATE;
            }

            NbtCompound tag = NbtHelper.fromBlockState(state);
            tagList.add(tag);
        }

        return tagList;
    }

}
