package me.isaquall.hyacinth.client;

import fi.dy.masa.litematica.util.FileType;
import me.isaquall.hyacinth.block_palette.BlockPalette;
import me.isaquall.hyacinth.dithering.DitheringStrategy;
import me.isaquall.hyacinth.dithering.algorithm.DitheringAlgorithm;
import me.isaquall.hyacinth.resizing_strategy.ResizingStrategy;
import me.isaquall.hyacinth.schematic.SchematicWriter;
import me.isaquall.hyacinth.schematic.SupportMode;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/*
    Keeps track of all tasks to be applied to the image
 */

public class MapartPipeline {

    private final HashMap<BlockPalette, BlockState> selectedBlocks;
    private final List<Function<BufferedImage, BufferedImage>> tasks;

    private File file;
    private ResizingStrategy resizingStrategy = ResizingStrategy.RESIZING_STRATEGIES.get(Identifier.of("hyacinth", "resizing_strategy/scale_smooth"));
    private DitheringStrategy ditheringStrategy = DitheringStrategy.DITHERING_STRATEGIES.get(Identifier.of("hyacinth", "dithering_strategy/floyd_steinberg"));
    private int mapWidth = 1;
    private int mapHeight = 1;
    private FileType exportType = FileType.LITEMATICA_SCHEMATIC;
    private SupportMode supportMode = SupportMode.ONLY_REQUIRED;

    public MapartPipeline() {
        selectedBlocks = new HashMap<>();
        tasks = new ArrayList<>();

        Function<BufferedImage, BufferedImage> resizingTask = image -> resizingStrategy.resize(image, mapWidth * 128, mapHeight * 128);
        tasks.add(resizingTask);
    }

    public @Nullable BufferedImage process() {
        if (file == null) return null;

        long time = System.currentTimeMillis();
        BufferedImage image;
        try {
            ImageIO.setUseCache(false); // TODO profile this?
            image = ImageIO.read(file);
            for (Function<BufferedImage, BufferedImage> task : tasks) {
                image = task.apply(image);
            }
            DitheringAlgorithm.DitheringResult ditheringResult = ditheringStrategy.ditheringAlgorithm().dither(image, selectedBlocks, true);
            image = ditheringResult.image();
            SchematicWriter.createSchematic(ditheringResult.pixels(), file.getName(), supportMode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Process completed in " + (System.currentTimeMillis() - time) + "ms.");
        return image;
    }

    public void resizingStrategy(ResizingStrategy resizingStrategy) {
        this.resizingStrategy = resizingStrategy;
    }

    public ResizingStrategy resizingStrategy() {
        return resizingStrategy;
    }

    public DitheringStrategy ditheringStrategy() {
        return ditheringStrategy;
    }

    public void ditheringStrategy(DitheringStrategy ditheringStrategy) {
        this.ditheringStrategy = ditheringStrategy;
    }

    public void openFile(File file) {
        this.file = file;
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

    public FileType exportType() {
        return exportType;
    }

    public void exportType(FileType exportType) {
        this.exportType = exportType;
    }

    public SupportMode supportMode() {
        return supportMode;
    }

    public void supportMode(SupportMode supportMode) {
        this.supportMode = supportMode;
    }

    public List<Function<BufferedImage, BufferedImage>> tasks() {
        return tasks;
    }
}
