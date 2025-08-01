package me.isaquall.hyacinth.dithering.algorithm;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.isaquall.hyacinth.block_palette.BlockPalette;
import net.minecraft.block.BlockState;

import java.awt.image.BufferedImage;
import java.util.Map;

public abstract class BaseDitheringAlgorithm implements DitheringAlgorithm {

    protected Int2ObjectArrayMap<BlockPalette> colors;
    protected int height;
    protected int width;
    protected Pixel[][] mapMatrix;

    // Caching + cache invalidation variables
    protected static final Int2ObjectOpenHashMap<int[]> colorMatchCache = new Int2ObjectOpenHashMap<>(); // maps exact RGB int to { matchRGB, matchBrightness }
    protected static int lastColorHash;
    protected static boolean lastStaircasing;
    protected static boolean lastBetterColor;

    static {
        colorMatchCache.defaultReturnValue(new int[]{-1, -1});
    }

    @Override
    public DitheringResult dither(BufferedImage in, Map<BlockPalette, BlockState> palettes, boolean staircasing, boolean betterColor) {
        // Generate available colors
        colors = generateColors(palettes);

        if (colors.hashCode() != lastColorHash || lastBetterColor != betterColor || lastStaircasing != staircasing) {
            colorMatchCache.clear();
            lastColorHash = colors.hashCode();
            lastBetterColor = betterColor;
            lastStaircasing = staircasing;
            System.out.println("Cache invalidated.");
        }

        width = in.getWidth();
        height = in.getHeight();
        mapMatrix = new Pixel[width][height];
        return null;
    }
}
