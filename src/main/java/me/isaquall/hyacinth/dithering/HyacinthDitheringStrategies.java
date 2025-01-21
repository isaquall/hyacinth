package me.isaquall.hyacinth.dithering;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import me.isaquall.hyacinth.ColorUtils;
import me.isaquall.hyacinth.block_palette.BlockPalette;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HyacinthDitheringStrategies {

    public static final Map<Identifier, DitheringStrategy> DITHERING_STRATEGIES = new HashMap<>();

    static {
        DITHERING_STRATEGIES.put(Identifier.of("hyacinth", "dithering_strategy/default"), (in, ditheringMatrix, palettes, staircasing) -> {
            // Generate available colors
            Int2ObjectArrayMap<BlockPalette> colors = new Int2ObjectArrayMap<>();
            for (BlockPalette palette : palettes.keySet()) {
                if (palettes.get(palette) != Blocks.BARRIER.getDefaultState()) {
                    colors.put(palette.color(), palette);
                }
            }

            int width = in.getWidth();
            int height = in.getHeight();
            DitheringStrategy.Pixel[][] mapMatrix = new DitheringStrategy.Pixel[width][height];

            if (colors.isEmpty()) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        in.setRGB(x, y, 0); // TODO background color here
                    }
                }
                return new DitheringStrategy.DitheringResult(new DitheringStrategy.Pixel[0][], in);
            }

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    // Convert ARGB to RGB
                    int original = in.getRGB(x, y);
                    original = ColorUtils.getRGBInt(ColorHelper.getRed(original), ColorHelper.getGreen(original), ColorHelper.getBlue(original));

                    int[] matchInfo = ColorUtils.findClosestColor(original, colors.keySet(), staircasing);
                    int match = ColorUtils.scaleRGB(matchInfo[0], matchInfo[1]);
                    in.setRGB(x, y, ColorHelper.fullAlpha(match));
                    mapMatrix[x][y] = new DitheringStrategy.Pixel(matchInfo[0], matchInfo[1], palettes.get(colors.get(matchInfo[0])));

                    if (ditheringMatrix.matrix() != null) {
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
                    if (colors.keySet().contains(mapMatrix[x][y].color())) {
                        in.setRGB(x, y, ColorHelper.fullAlpha(ColorUtils.scaleRGB(mapMatrix[x][y].color(), mapMatrix[x][y].brightness())));
                    } else {
                        System.out.println("Invalid color.");
                        in.setRGB(x, y, Color.PINK.getRGB()); // TODO better error handling here
                    }
                }
            }
            return new DitheringStrategy.DitheringResult(mapMatrix, in);
        });
    }
}
