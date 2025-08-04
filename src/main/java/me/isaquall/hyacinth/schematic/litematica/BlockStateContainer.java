package me.isaquall.hyacinth.schematic.litematica;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.ApiStatus;

/*
    From: https://github.com/sakura-ryoko/litematica/blob/1.21.8/src/main/java/fi/dy/masa/litematica/schematic/container/LitematicaBlockStateContainer.java
 */
@ApiStatus.Internal
public class BlockStateContainer {

    public static final BlockState AIR_BLOCK_STATE = Blocks.AIR.getDefaultState();
    protected final Vec3i size;
    protected final int sizeX;
    protected final int sizeY;
    protected final int sizeZ;
    protected final int sizeLayer;
    protected final long totalVolume;
    protected BitArray storage;
    protected BlockStatePalette palette;
    protected int bits;

    public BlockStateContainer(int sizeX, int sizeY, int sizeZ, int length) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.sizeLayer = sizeX * sizeZ;
        this.totalVolume = (long) this.sizeX * (long) this.sizeY * (long) this.sizeZ;
        this.size = new Vec3i(this.sizeX, this.sizeY, this.sizeZ);

        // take log base 2 of length, then round up
        this.setBits((int) Math.ceil(Math.log(length) / Math.log(2)));
    }

    public BlockState get(int x, int y, int z) {
        BlockState state = this.palette.getBlockState(this.storage.getAt(this.getIndex(x, y, z)));
        return state == null ? AIR_BLOCK_STATE : state;
    }

    public void set(int x, int y, int z, BlockState state) {
        int id = this.palette.idFor(state);
        this.storage.setAt(this.getIndex(x, y, z), id);
    }

    protected int getIndex(int x, int y, int z) {
        return (y * this.sizeLayer) + z * this.sizeX + x;
    }

    protected void setBits(int bitsIn) {
        if (bitsIn != this.bits) {
            this.bits = bitsIn;

            this.palette = new BlockStatePalette(this.bits);
            this.palette.idFor(AIR_BLOCK_STATE);
            this.storage = new BitArray(this.bits, this.totalVolume);
        }
    }

    public BlockStatePalette getPalette() {
        return this.palette;
    }

    public long[] getBackingLongArray() {
        return this.storage.getBackingLongArray();
    }
}
