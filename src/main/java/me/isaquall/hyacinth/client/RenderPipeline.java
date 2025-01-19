package me.isaquall.hyacinth.client;

import me.isaquall.hyacinth.block_palette.BlockPalette;
import me.isaquall.hyacinth.dithering.DitheringMatrix;
import me.isaquall.hyacinth.dithering.DitheringStrategy;
import me.isaquall.hyacinth.dithering.HyacinthDitheringStrategies;
import me.isaquall.hyacinth.resizing_strategy.HyacinthResizingStrategies;
import me.isaquall.hyacinth.resizing_strategy.ResizingStrategy;
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

public class RenderPipeline {

    private final HashMap<BlockPalette, BlockState> selectedBlocks;
    private final List<Function<BufferedImage, BufferedImage>> tasks;

    private File file;
    private ResizingStrategy resizingStrategy = HyacinthResizingStrategies.RESIZING_STRATEGIES.get(Identifier.of("hyacinth", "resizing_strategy/scale_smooth"));
    private DitheringMatrix ditheringMatrix = DitheringMatrix.DITHERING_MATRICES.get(Identifier.of("hyacinth", "dithering_matrix/floyd_steinberg"));
    private DitheringStrategy ditheringStrategy = HyacinthDitheringStrategies.DITHERING_STRATEGIES.get(Identifier.of("hyacinth", "default_dithering_strategy"));

    public RenderPipeline() {
        selectedBlocks = new HashMap<>();
        tasks = new ArrayList<>();

        Function<BufferedImage, BufferedImage> resizingTask = image -> resizingStrategy.resize(image, 128, 128);
        tasks.add(resizingTask);
    }

    public @Nullable BufferedImage process() {
        if (file == null) return null;

        BufferedImage image;
        try {
            image = ImageIO.read(file);
            for (Function<BufferedImage, BufferedImage> task : tasks) {
                image = task.apply(image);
            }
            image = ditheringStrategy.dither(image, ditheringMatrix, selectedBlocks, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return image;
    }

    public void resizingStrategy(ResizingStrategy resizingStrategy) {
        this.resizingStrategy = resizingStrategy;
    }

    public ResizingStrategy resizingStrategy() {
        return resizingStrategy;
    }

    public void openFile(File file) {
        this.file = file;
    }

    public Map<BlockPalette, BlockState> selectedBlocks() {
        return selectedBlocks;
    }

    public void ditheringMatrix(DitheringMatrix matrix) {
        this.ditheringMatrix = matrix;
    }

    public DitheringMatrix ditheringMatrix() {
        return ditheringMatrix;
    }

    public List<Function<BufferedImage, BufferedImage>> tasks() {
        return tasks;
    }
}
