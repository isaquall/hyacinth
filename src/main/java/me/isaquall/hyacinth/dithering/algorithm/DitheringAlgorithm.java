package me.isaquall.hyacinth.dithering.algorithm;

import me.isaquall.hyacinth.block_palette.BlockPalette;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public interface DitheringAlgorithm {

    Map<Identifier, Class<? extends DitheringAlgorithm>> DITHERING_ALGORITHMS = new HashMap<>(Map.of(
            Identifier.of("hyacinth", "dithering_algorithm/matrix"), MatrixDitheringAlgorithm.class)
    );

    DitheringResult dither(BufferedImage in, Map<BlockPalette, BlockState> palettes, boolean staircasing);

    record Pixel(int color, int brightness, BlockState block) {

    }

    record DitheringResult(Pixel[][] pixels, BufferedImage image) {

    }
}
