package me.isaquall.hyacinth;

import java.util.List;

public class ColorUtils {

    public static final int[] SCALES = { 180, 220, 255 }; // low, normal, high

    public static int[] getVariations(int rgb) {
        int[] variations = new int[3];
        int[] RGBTriple = getRGBTriple(rgb);
        for (int variation = 0; variation < 3; variation++) {
            variations[variation] = getRGBInt(
                    Math.clamp((long) RGBTriple[0] * (long) SCALES[variation] / 255L, 0, 255),
                    Math.clamp((long) RGBTriple[1] * (long) SCALES[variation] / 255L, 0, 255),
                    Math.clamp((long) RGBTriple[2] * (long) SCALES[variation] / 255L, 0, 255));
        }
        return variations;
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

    public static int findClosestColor(int rgb, List<Integer> colors) {
        double smallestDifference = Double.MAX_VALUE;
        int bestMatch = 0;
        for (int color : colors) {
            double difference = colorDifference(RGB2LAB(getRGBTriple(rgb)), RGB2LAB(getRGBTriple(color))); // TODO maybe memoize?
            if (difference < smallestDifference) {
                bestMatch = color;
                smallestDifference = difference;
            }
        }
        return bestMatch;
    }

    private static double colorDifference(int[] rgb, int[] lab) {
        return (rgb[0] - lab[0]) * (rgb[0] - lab[0]) + (rgb[1] - lab[1]) * (rgb[1] - lab[1]) + (rgb[2] - lab[2]) * (rgb[2] - lab[2]);
    }

    // Copied from: https://github.com/redstonehelper/MapConverter/blob/main/MapConverter.java#L496
    private static int[] RGB2LAB(int[] rgb) {
        double[] values = new double[3];
        for (int i = 0; i < 3; i++) {
            double V = rgb[i] / 255.0;
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
        return lab;
    }

}
