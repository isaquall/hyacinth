package me.isaquall.hyacinth.block_palette;

import blue.endless.jankson.annotation.SerializedName;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.block.BlockState;

import java.util.LinkedHashSet;

/*
    A block palette represents a grouping of block states that produce the same color.
 */
public class BlockPalette {

    private String translatableName;
    private int color;
    @SerializedName("blockstates")
    private LinkedHashSet<BlockState> states;

    public static final Int2ObjectArrayMap<BlockPalette> BLOCK_PALETTES = new Int2ObjectArrayMap<>();

    public BlockPalette() { }

    public BlockPalette(String translatableName, int color, LinkedHashSet<BlockState> states) {
        this.translatableName = translatableName;
        this.color = color;
        this.states = states;
    }

    public String translatableName() {
        return translatableName;
    }

    public int color() {
        return color;
    }

    public LinkedHashSet<BlockState> states() {
        return states;
    }
}
