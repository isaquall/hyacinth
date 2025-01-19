package me.isaquall.hyacinth.dithering;

import me.isaquall.hyacinth.block_palette.BlockPalette;
import net.minecraft.block.BlockState;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.Map;

public interface DitheringStrategy { // TODO javadoc

    BufferedImage dither(BufferedImage in, @Nullable DitheringMatrix matrix, Map<BlockPalette, BlockState> palettes, boolean staircasing);
}
