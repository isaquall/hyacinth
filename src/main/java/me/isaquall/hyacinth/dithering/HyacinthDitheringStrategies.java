package me.isaquall.hyacinth.dithering;

import com.google.common.primitives.Ints;
import me.isaquall.hyacinth.ColorUtils;
import me.isaquall.hyacinth.block_palette.BlockPalette;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HyacinthDitheringStrategies {

    public static final Map<Identifier, DitheringStrategy> DITHERING_STRATEGIES = new HashMap<>();

    static {
        DITHERING_STRATEGIES.put(Identifier.of("hyacinth", "dithering_strategy/default"), (in, ditheringMatrix, palettes, staircasing) -> {
            // Generate available colors
            List<Integer> colors = new ArrayList<>();
            for (BlockPalette palette : palettes.keySet()) {
                if (palettes.get(palette) != Blocks.BARRIER.getDefaultState()) {
                    if (staircasing) {
                        colors.addAll(Ints.asList(ColorUtils.getVariations(palette.color())));
                    } else {
                        colors.add(palette.color());
                    }
                }
            }

            int width = in.getWidth();
            int height = in.getHeight();
            int[][] mapMatrix = new int[width][height];

            if (colors.isEmpty()) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        in.setRGB(x, y, 0); // TODO background color here
                    }
                }
                return in;
            }

            for (int y = 0; y < height; y++) { // Note: All of this is in RGB not ARGB
                for (int x = 0; x < width; x++) {
                    int original = in.getRGB(x, y);
                    int match = ColorUtils.findClosestColor(original, colors);
                    in.setRGB(x, y, ColorHelper.fullAlpha(match));
                    mapMatrix[x][y] = match;

                    if (ditheringMatrix != null) {
                        int[] matchRGB = ColorUtils.getRGBTriple(match);
                        int[] originalRGB = ColorUtils.getRGBTriple(original);
                        double[] difference = new double[3];
                        for (int channel = 0; channel < 3; channel++) {
                            difference[channel] = originalRGB[channel] - matchRGB[channel];
                        }

                        List<int[]> matrix = ditheringMatrix.matrix();
                        int scaleFactor = ditheringMatrix.scaleFactor();
                        for (int[] ints : matrix) {
                            int xOffset = ints[0];
                            int yOffset = ints[1];
                            if (x + xOffset < width && x + xOffset >= 0 && y + yOffset < height && y + yOffset >= 0) {
                                int[] nextRGB = ColorUtils.getRGBTriple(in.getRGB(x + xOffset, y + yOffset));
                                for (int channel = 0; channel < 3; channel++) {
                                    nextRGB[channel] = (int) Math.clamp(nextRGB[channel] + difference[channel] * ints[2] / scaleFactor, 0, 255);
                                }
                                in.setRGB(x + xOffset, y + yOffset, ColorHelper.fullAlpha(ColorUtils.getRGBInt(nextRGB[0], nextRGB[1], nextRGB[2])));
                            }
                        }
                    }
                }
            }

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (colors.contains(mapMatrix[x][y])) {
                        in.setRGB(x, y, ColorHelper.fullAlpha(mapMatrix[x][y]));
                    } else {
                        System.out.println("Invalid color.");
                        in.setRGB(x, y, Color.PINK.getRGB()); // TODO better error handling here
                    }
                }
            }
            return in;
        });
    }
}
