package me.isaquall.hyacinth.block_palette;

import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;

/*
    A block palette represents a grouping of block states that produce the same color.
 */
public record BlockPalette(Identifier id, String translatableName, int color, LinkedHashSet<BlockState> states) {

    public static final Map<Identifier, BlockPalette> BLOCK_PALETTES = new HashMap<>();

    @Override
    public int hashCode() {
        return Objects.hash(id); // for some reason if we hash the other entries, it breaks hashCode contract??
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BlockPalette palette = (BlockPalette) o;
        return Objects.equals(id, palette.id);
    }
}
