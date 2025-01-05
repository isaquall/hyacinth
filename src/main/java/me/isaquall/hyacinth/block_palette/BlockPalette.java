package me.isaquall.hyacinth.block_palette;

import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/*
    A block palette represents a grouping of block states that produce the same color.
 */
public record BlockPalette(Identifier id, String name, int color, LinkedHashSet<BlockState> states) {

    public static final Map<Identifier, BlockPalette> BLOCK_PALETTES = new HashMap<>();
}
