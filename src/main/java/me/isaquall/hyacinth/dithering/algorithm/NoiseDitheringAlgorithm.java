package me.isaquall.hyacinth.dithering.algorithm;

import blue.endless.jankson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import me.isaquall.hyacinth.ColorUtils;
import me.isaquall.hyacinth.block_palette.BlockPalette;
import me.isaquall.hyacinth.client.HyacinthClient;
import me.isaquall.hyacinth.mixin.SpriteContentsAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

import java.awt.image.BufferedImage;
import java.util.Map;

public class NoiseDitheringAlgorithm implements DitheringAlgorithm {

    private final Identifier noiseTexture;
    private final int sampleSize;

    public NoiseDitheringAlgorithm(Identifier id, JsonObject algorithmEntry) {
        this.noiseTexture = Identifier.of(algorithmEntry.get(String.class, "noise"));
        this.sampleSize = algorithmEntry.getInt("sample_size", 4);
    }

    @Override
    public DitheringResult dither(BufferedImage in, Map<BlockPalette, BlockState> palettes, boolean staircasing) {
        Sprite sprite = HyacinthClient.NOISE_MANAGER.getAtlas().getSprite(noiseTexture);
        NativeImage noise = ((SpriteContentsAccessor) sprite.getContents()).getImage();
        Int2ObjectArrayMap<BlockPalette> colors = generateColors(palettes);

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
                int[] originalRGB = ColorUtils.getRGBTriple(original);

                int[] matchInfo = ColorUtils.findClosestColor(original, colors.keySet(), staircasing);
                int match = ColorUtils.scaleRGB(matchInfo[0], matchInfo[1]);
                int[] matchRGB = ColorUtils.getRGBTriple(match);
                mapMatrix[x][y] = new Pixel(matchInfo[0], matchInfo[1], palettes.get(colors.get(matchInfo[0])));

                double[] difference = new double[3];
                for (int channel = 0; channel < 3; channel++) {
                    difference[channel] = originalRGB[channel] - matchRGB[channel];
                }

                float[][][] sample = NoiseSample.create(noise, sampleSize, x, y).normalize();

                for (int noiseX = 0; noiseX < sampleSize; noiseX++) {
                    for (int noiseY = 0; noiseY < sampleSize; noiseY++) {
                        if (noiseX == 0 && noiseY == 0) continue;
                        if (x + noiseX < width && x + noiseX >= 0 && y + noiseY < height && y + noiseY >= 0) {
                            int oldRGB = in.getRGB(x + noiseX, y + noiseY);
                            int[] oldRGBTriple = {ColorHelper.getRed(oldRGB), ColorHelper.getGreen(oldRGB), ColorHelper.getBlue(oldRGB)};
                            int[] newRGB = new int[3];
                            for (int channel = 0; channel < 3; channel++) {
                                newRGB[channel] = (int) Math.clamp(oldRGBTriple[channel] + (difference[channel] * sample[noiseX][noiseY][channel]), 0, 255);
                            }
                            in.setRGB(x + noiseX, y + noiseY, ColorHelper.fullAlpha(ColorUtils.getRGBInt(newRGB[0], newRGB[1], newRGB[2])));
                        }
                    }
                }
                in.setRGB(x, y, ColorHelper.fullAlpha(match));
            }
        }
        return new DitheringResult(mapMatrix, in);
    }

    private static class NoiseSample {

        private final int[][] colors;
        private final int sampleSize;

        NoiseSample(int[][] colors, int sampleSize) {
            this.colors = colors;
            this.sampleSize = sampleSize;
        }

        public static NoiseSample create(NativeImage noise, int sampleSize, int posX, int posY) {
            int[][] colors = new int[sampleSize][sampleSize];
            for (int x = 0; x < sampleSize; x++) {
                for (int y = 0; y < sampleSize; y++) {
                    int argb = noise.getColorArgb(wrap((noise.getWidth() - 1), (posX + x)), wrap((noise.getHeight() - 1), (posY + y))); // Wrap around if we need to
                    int rgb = ColorUtils.getRGBInt(ColorHelper.getRed(argb), ColorHelper.getGreen(argb), ColorHelper.getBlue(argb));
                    colors[x][y] = rgb;
                }
            }
            return new NoiseSample(colors, sampleSize);
        }

        private static int wrap(int bound, int value) {
            return (value == 0) ? 0 : bound % value;
        }

        public float[][][] normalize() {
            int[] sums = new int[3]; // 0: sum of red channel, 1: sum of green channel, 2: sum of blue channel
            for (int[] color : colors) {
                for (int j : color) {
                    int[] rgb = ColorUtils.getRGBTriple(j);
                    for (int channel = 0; channel < 3; channel++) {
                        sums[channel] += rgb[channel];
                    }
                }
            }

            float[][][] normalized = new float[sampleSize][sampleSize][3];
            for (int x = 0; x < colors.length; x++) {
                for (int y = 0; y < colors[x].length; y++) {
                    int[] rgb = ColorUtils.getRGBTriple(colors[x][y]);
                    for (int channel = 0; channel < 3; channel++) {
                        normalized[x][y][channel] = (float) rgb[channel] / sums[channel];
                    }
                }
            }
            return normalized;
        }

        public int[][] colors() {
            return colors;
        }
    }
}
