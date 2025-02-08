package me.isaquall.hyacinth.dithering.algorithm;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import io.github.cottonmc.jankson.JanksonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import me.isaquall.hyacinth.ColorUtils;
import me.isaquall.hyacinth.Hyacinth;
import me.isaquall.hyacinth.block_palette.BlockPalette;
import me.isaquall.hyacinth.ui.HyacinthToast;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MatrixDitheringAlgorithm implements DitheringAlgorithm {

    private static final JanksonOps OPS = JanksonOps.INSTANCE;
    private static final Jankson JANKSON = Hyacinth.JANKSON;

    private final List<int[]> matrix = new ArrayList<>();
    private final int scaleFactor;

    public MatrixDitheringAlgorithm(String id, JsonObject algorithmEntry) {
        scaleFactor = algorithmEntry.getInt("scale_factor", 1);
        if (algorithmEntry.get("matrix") instanceof JsonArray) {
            OPS.getList(algorithmEntry.get("matrix")).getOrThrow(message -> {
                error(id, message);
                throw new RuntimeException("Hyacinth failed to read a dithering algorithm." + message);
            }).accept(element -> {
                try {
                    JsonObject object = JANKSON.load(element.toJson());
                    matrix.add(new int[]{object.getInt("x", 0), object.getInt("y", 0), object.getInt("value", 0)});
                } catch (SyntaxError e) {
                    error(id, e.toString());
                    throw new RuntimeException("Hyacinth failed to read a dithering algorithm." + e);
                }
            });
        }
    }

    public DitheringResult dither(BufferedImage in, Map<BlockPalette, BlockState> palettes, boolean staircasing) {
        // Generate available colors
        Int2ObjectArrayMap<BlockPalette> colors = new Int2ObjectArrayMap<>();
        for (BlockPalette palette : palettes.keySet()) {
            if (palettes.get(palette) != Blocks.BARRIER.getDefaultState()) {
                colors.put(palette.color(), palette);
            }
        }

        int width = in.getWidth();
        int height = in.getHeight();
        Pixel[][] mapMatrix = new Pixel[width][height];

        if (colors.isEmpty()) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    in.setRGB(x, y, 0); // TODO background color here
                }
            }
            return new DitheringResult(new Pixel[0][], in);
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Convert ARGB to RGB
                int original = in.getRGB(x, y);
                original = ColorUtils.getRGBInt(ColorHelper.getRed(original), ColorHelper.getGreen(original), ColorHelper.getBlue(original));

                int[] matchInfo = ColorUtils.findClosestColor(original, colors.keySet(), staircasing);
                int match = ColorUtils.scaleRGB(matchInfo[0], matchInfo[1]);
                in.setRGB(x, y, ColorHelper.fullAlpha(match));
                mapMatrix[x][y] = new Pixel(matchInfo[0], matchInfo[1], palettes.get(colors.get(matchInfo[0])));

                if (!matrix.isEmpty()) {
                    int[] matchRGB = ColorUtils.getRGBTriple(match);
                    int[] originalRGB = ColorUtils.getRGBTriple(original);
                    double[] difference = new double[3];
                    for (int channel = 0; channel < 3; channel++) {
                        difference[channel] = originalRGB[channel] - matchRGB[channel];
                    }

                    for (int[] entry : matrix) {
                        int xOffset = entry[0];
                        int yOffset = entry[1];
                        if (x + xOffset < width && x + xOffset >= 0 && y + yOffset < height && y + yOffset >= 0) {
                            int[] nextRGB = ColorUtils.getRGBTriple(in.getRGB(x + xOffset, y + yOffset));
                            for (int channel = 0; channel < 3; channel++) {
                                nextRGB[channel] = (int) Math.clamp(nextRGB[channel] + difference[channel] * entry[2] / scaleFactor, 0, 255);
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
        return new DitheringResult(mapMatrix, in);
    }

    private static void error(String id, String message) {
        MinecraftClient.getInstance().getToastManager().add(new HyacinthToast(List.of(
                Text.translatable("hyacinth.error"),
                Text.translatable("hyacinth.failed_to_create_matrix_dithering_algorithm", id),
                Text.of(message))));
    }
}
