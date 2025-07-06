package me.isaquall.hyacinth.block_palette;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.block.BlockState;

import java.util.List;

/*
    A block palette represents a grouping of block states that produce the same color.
 */
public class BlockPalette {

    private String translatableName;
    private int color;
    private List<BlockState> states;

    public static final Int2ObjectArrayMap<BlockPalette> BLOCK_PALETTES = new Int2ObjectArrayMap<>();

    public BlockPalette() { }

    public BlockPalette(String translatableName, int color, List<BlockState> states) {
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

    public List<BlockState> states() {
        return states;
    }
}
