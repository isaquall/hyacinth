package me.isaquall.hyacinth.client;

import me.isaquall.hyacinth.block_palette.BlockPalette;
import me.isaquall.hyacinth.dithering.DitheringStrategy;
import me.isaquall.hyacinth.dithering.algorithm.DitheringAlgorithm;
import me.isaquall.hyacinth.schematic.StaircaseMode;
import me.isaquall.hyacinth.schematic.SupportMode;
import me.isaquall.hyacinth.util.ImageUtils;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/*
    Keeps track of all tasks to be applied to the image and stores the result
 */

public class MapartPipeline {

    private final HashMap<BlockPalette, BlockState> selectedBlocks;
    private final List<Function<BufferedImage, BufferedImage>> tasks;

    private BufferedImage baseImage = null;
    private DitheringStrategy ditheringStrategy = DitheringStrategy.DITHERING_STRATEGIES.get(Identifier.of("hyacinth", "dithering_strategy/floyd_steinberg"));
    private int mapWidth = 1;
    private int mapHeight = 1;
    private SupportMode supportMode = SupportMode.ONLY_REQUIRED;
    private StaircaseMode staircaseMode = StaircaseMode.CLASSIC;
    private boolean betterColor = true;
    private DitheringAlgorithm.Pixel[][] pixels = null;
    private BufferedImage finishedImage = null;

    public MapartPipeline() {
        selectedBlocks = new HashMap<>();
        tasks = new ArrayList<>();
    }

    public @Nullable BufferedImage process() {
        long time = System.currentTimeMillis();
        if (finishedImage != null) finishedImage.flush();
        if (baseImage == null) return null;
        this.finishedImage = ImageUtils.cloneBufferedImage(baseImage);
        for (Function<BufferedImage, BufferedImage> task : tasks) {
            finishedImage = task.apply(finishedImage);
        }
        DitheringAlgorithm.DitheringResult ditheringResult = ditheringStrategy.ditheringAlgorithm().dither(finishedImage, selectedBlocks, (staircaseMode == StaircaseMode.VALLEY || staircaseMode == StaircaseMode.CLASSIC), betterColor);
        pixels = ditheringResult.pixels();
        this.finishedImage = ditheringResult.image();
        System.out.println("Process completed in " + (System.currentTimeMillis() - time) + "ms.");
        return finishedImage;
    }

//    public void exportToLitematica() {
//        SchematicWriter.createSchematic(pixels, file.getName(), supportMode, staircaseMode);
//    }

    public DitheringStrategy ditheringStrategy() {
        return ditheringStrategy;
    }

    public void ditheringStrategy(DitheringStrategy ditheringStrategy) {
        this.ditheringStrategy = ditheringStrategy;
    }

    public Map<BlockPalette, BlockState> selectedBlocks() {
        return selectedBlocks;
    }

    public void mapWidth(int width) {
        this.mapWidth = width;
    }

    public int mapWidth() {
        return mapWidth;
    }

    public void mapHeight(int height) {
        this.mapHeight = height;
    }

    public int mapHeight() {
        return mapHeight;
    }

    public SupportMode supportMode() {
        return supportMode;
    }

    public void supportMode(SupportMode supportMode) {
        this.supportMode = supportMode;
    }

    public StaircaseMode staircaseMode() {
        return staircaseMode;
    }

    public void staircaseMode(StaircaseMode staircaseMode) {
        this.staircaseMode = staircaseMode;
    }

    public boolean betterColor() {
        return betterColor;
    }

    public void betterColor(boolean betterColor) {
        this.betterColor = betterColor;
    }

    public List<Function<BufferedImage, BufferedImage>> tasks() {
        return tasks;
    }

    public BufferedImage finishedImage() {
        return this.finishedImage;
    }

    public BufferedImage baseImage() {
        return baseImage;
    }

    public void baseImage(BufferedImage baseImage) {
        this.baseImage = baseImage;
    }
}
