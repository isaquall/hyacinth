package me.isaquall.hyacinth;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.block.MapColor;

public class ColorUtils {

    public static final Int2IntOpenHashMap RGB_TO_LAB = new Int2IntOpenHashMap();
    private static final MapColor.Brightness[] BRIGHTNESS = MapColor.Brightness.values();

    public static int[] getVariations(int rgb) {
        int[] variations = new int[3];
        for (int variation = 0; variation < 3; variation++) {
            variations[variation] = scaleRGB(rgb, BRIGHTNESS[variation].brightness);
        }
        return variations;
    }

    public static int scaleRGB(int rgb, int scale) {
        int[] RGBTriple = getRGBTriple(rgb);
        return getRGBInt(
                Math.clamp((long) RGBTriple[0] * (long) scale / 255L, 0, 255),
                Math.clamp((long) RGBTriple[1] * (long) scale / 255L, 0, 255),
                Math.clamp((long) RGBTriple[2] * (long) scale / 255L, 0, 255));
    }

    public static int[] getRGBTriple(int rgb) {
        int[] array = new int[3];
        array[0] = (rgb >> 16) & 0xFF;
        array[1] = (rgb >> 8) & 0xFF;
        array[2] = rgb & 0xFF;
        return array;
    }

    public static int getRGBInt(int r, int g, int b) {
        return 256 * 256 * r + 256 * g + b;
    }

    public static int[] findClosestColor(int rgb, IntSet colors, boolean staircasing) {
        double smallestDifference = Double.MAX_VALUE;
        int[] bestMatch = { 0, 0 };
        for (int color : colors) {
            if (staircasing) {
                int[] variations = getVariations(color);
                for (int variation = 0; variation < 3; variation++) {
                    double difference = colorDifference(RGB2LAB(rgb), RGB2LAB(variations[variation]));
                    if (difference < smallestDifference) {
                        bestMatch = new int[]{ color, BRIGHTNESS[variation].brightness };
                        smallestDifference = difference;
                    }
                }
            } else {
                double difference = colorDifference(RGB2LAB(rgb), RGB2LAB(color));
                if (difference < smallestDifference) {
                    bestMatch = new int[]{ color, 255 };
                    smallestDifference = difference;
                }
            }
        }
        return bestMatch;
    }

    private static double colorDifference(int[] rgb, int[] lab) {
        return (rgb[0] - lab[0]) * (rgb[0] - lab[0]) + (rgb[1] - lab[1]) * (rgb[1] - lab[1]) + (rgb[2] - lab[2]) * (rgb[2] - lab[2]);
    }

    // Copied from: https://github.com/redstonehelper/MapConverter/blob/main/MapConverter.java#L496
    private static int[] RGB2LAB(int rgb) {
        if (RGB_TO_LAB.containsKey(rgb)) {
            return getRGBTriple(RGB_TO_LAB.get(rgb));
        } else {
            int[] RGBTriple = getRGBTriple(rgb);
            double[] values = new double[3];
            for (int i = 0; i < 3; i++) {
                double V = RGBTriple[i] / 255.0;
                double v = Math.pow(V, 2.2);
                if (V <= 0.04045) {
                    v = V / 12.92;
                } else {
                    v = Math.pow((V + 0.055) / 1.055, 2.4);
                }
                values[i] = v;
            }
            double[] XYZ = new double[3];
            XYZ[0] = 0.4360747 * values[0] + 0.3850649 * values[1] + 0.1430804 * values[2];
            XYZ[1] = 0.2225045 * values[0] + 0.7168786 * values[1] + 0.0606169 * values[2];
            XYZ[2] = 0.0139322 * values[0] + 0.0971045 * values[1] + 0.7141733 * values[2];

            XYZ[0] = XYZ[0] / 0.96422;
            XYZ[1] = XYZ[1] / 1.0;
            XYZ[2] = XYZ[2] / 0.82521;
            double[] fVals = new double[3];
            for (int i = 0; i < 3; i++) {
                double f;
                double val = XYZ[i];
                double valr = val;
                if (valr > (216.0 / 24389.0)) {
                    f = Math.pow(valr, 1.0 / 3.0);
                } else {
                    f = ((24389.0 / 27.0) * valr + 16.0) / 116.0;
                }
                fVals[i] = f;
            }
            // lab values, moved into [0,255]
            int[] lab = new int[3];
            lab[0] = (int) (2.55 * (116 * fVals[1] - 16));
            lab[1] = 128 + (int) (500 * (fVals[0] - fVals[1]));
            lab[2] = 128 + (int) (200 * (fVals[1] - fVals[2]));
            RGB_TO_LAB.put(rgb, getRGBInt(lab[0], lab[1], lab[2]));
            return lab;
        }
    }
}
