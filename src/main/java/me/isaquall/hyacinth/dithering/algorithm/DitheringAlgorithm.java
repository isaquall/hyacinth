package me.isaquall.hyacinth.dithering.algorithm;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import me.isaquall.hyacinth.block_palette.BlockPalette;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public interface DitheringAlgorithm {

    Map<Identifier, Class<? extends DitheringAlgorithm>> DITHERING_ALGORITHMS = new HashMap<>(Map.of(
            Identifier.of("hyacinth", "dithering_algorithm/matrix"), MatrixDitheringAlgorithm.class,
            Identifier.of("hyacinth", "dithering_algorithm/noise"), NoiseDitheringAlgorithm.class));

    DitheringResult dither(BufferedImage in, Map<BlockPalette, BlockState> palettes, boolean staircasing, boolean betterColor);

    record Pixel(int color, int brightness, BlockState blockState) { // TODO maybe use MapColor here?

    }

    record DitheringResult(Pixel[][] pixels, BufferedImage image) {

    }

    default Int2ObjectArrayMap<BlockPalette> generateColors(Map<BlockPalette, BlockState> palettes) {
        Int2ObjectArrayMap<BlockPalette> colors = new Int2ObjectArrayMap<>();
        for (BlockPalette palette : palettes.keySet()) {
            if (palettes.get(palette) != Blocks.BARRIER.getDefaultState()) {
                colors.put(palette.color(), palette);
            }
        }
        return colors;
    }
}
