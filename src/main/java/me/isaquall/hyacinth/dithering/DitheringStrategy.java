package me.isaquall.hyacinth.dithering;

import me.isaquall.hyacinth.block_palette.BlockPalette;
import net.minecraft.block.BlockState;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.Map;

public interface DitheringStrategy { // TODO javadoc

    DitheringResult dither(BufferedImage in, DitheringMatrix matrix, Map<BlockPalette, BlockState> palettes, boolean staircasing);

    record Pixel(int color, int brightness, BlockState block) {

    }

    record DitheringResult(Pixel[][] pixels, BufferedImage image) {

    }
}
