package me.isaquall.hyacinth.client;

import me.isaquall.hyacinth.block_palette.BlockPalette;
import me.isaquall.hyacinth.resizing_strategy.HyacinthResizingStrategies;
import me.isaquall.hyacinth.resizing_strategy.ResizingStrategy;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/*
    Keeps track of all tasks to be applied to the image
    When a variable is changed, then its corresponding task and every subsequent task is dirty and must be redone // TODO
 */

public class RenderPipeline {

    public final Map<BlockPalette, BlockState> SELECTED_BLOCKS = new HashMap<>(); // TODO eventually make private??

    private File file;
    private boolean fileDirty = true;
    private BufferedImage cachedFile;
    private final Function<File, BufferedImage> readFileTask = file -> {
        if (fileDirty) {
            fileDirty = false;
            try {
                cachedFile = ImageIO.read(file);
                return cachedFile;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return cachedFile;
    };

    private ResizingStrategy resizingStrategy = HyacinthResizingStrategies.RESIZING_STRATEGIES.get(Identifier.of("hyacinth", "resizing_strategy/scale_smooth"));
    private boolean resizingStrategyDirty = true;
    private BufferedImage cachedResizingStrategy;
    private final Function<BufferedImage, BufferedImage> resizingTask = image -> {
        if (resizingStrategyDirty) {
            resizingStrategyDirty = false;
            cachedResizingStrategy = resizingStrategy.resize(image, 128, 128);
            return cachedResizingStrategy;
        }
        return cachedResizingStrategy;
    };

    public BufferedImage process() {
        return readFileTask.andThen(resizingTask).apply(file);
    }

    public void resizingStrategy(ResizingStrategy resizingStrategy) {
        this.resizingStrategy = resizingStrategy;
        this.resizingStrategyDirty = true;
    }

    public void openFile(File file) {
        this.file = file;
        this.fileDirty = true;
    }
}
