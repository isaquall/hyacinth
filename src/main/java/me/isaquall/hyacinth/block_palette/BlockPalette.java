package me.isaquall.hyacinth.block_palette;

import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
    A block palette represents a group of blocks that produce the same color.
 */
public record BlockPalette(Identifier id, String name, int color, List<BlockState> states) {

    public static final Map<Identifier, BlockPalette> BLOCK_PALETTES = new HashMap<>();
}
