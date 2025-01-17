package me.isaquall.hyacinth.client;

import me.isaquall.hyacinth.block_palette.BlockPalette;
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
    When a variable is changed, then its corresponding task and every subsequent task is dirty and must be redone // TODO
 */

public class RenderPipeline {

    private final Map<BlockPalette, BlockState> selectedBlocks = new HashMap<>();
    private final List<Function<BufferedImage, BufferedImage>> tasks = new ArrayList<>();

    private File file;
    private ResizingStrategy resizingStrategy = HyacinthResizingStrategies.RESIZING_STRATEGIES.get(Identifier.of("hyacinth", "resizing_strategy/scale_smooth"));

    public RenderPipeline() {
        Function<BufferedImage, BufferedImage> resizingTask = image -> resizingStrategy.resize(image, 128, 128);
        tasks.add(resizingTask);
    }

    public @Nullable BufferedImage process() {
        BufferedImage image;
        try {
            image = ImageIO.read(file);
            for (Function<BufferedImage, BufferedImage> task : tasks) {
                image = task.apply(image);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return image;
    }

    public void resizingStrategy(ResizingStrategy resizingStrategy) {
        this.resizingStrategy = resizingStrategy;
    }

    public void openFile(File file) {
        this.file = file;
    }

    public Map<BlockPalette, BlockState> selectedBlocks() {
        return selectedBlocks;
    }

    public List<Function<BufferedImage, BufferedImage>> tasks() {
        return tasks;
    }
}
