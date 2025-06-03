package me.isaquall.hyacinth.client;

import me.isaquall.hyacinth.block_palette.BlockPalette;
import me.isaquall.hyacinth.dithering.DitheringStrategy;
import me.isaquall.hyacinth.dithering.algorithm.DitheringAlgorithm;
import me.isaquall.hyacinth.resizing_strategy.ResizingStrategy;
import me.isaquall.hyacinth.schematic.SchematicWriter;
import me.isaquall.hyacinth.schematic.StaircaseMode;
import me.isaquall.hyacinth.schematic.SupportMode;
import me.isaquall.hyacinth.util.ImageUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
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
    Keeps track of all tasks to be applied to the image and stores the result
 */

public class MapartPipeline {

    private final HashMap<BlockPalette, BlockState> selectedBlocks;
    private final List<Function<BufferedImage, BufferedImage>> tasks;

    private File file = null;
    private ResizingStrategy resizingStrategy = ResizingStrategy.RESIZING_STRATEGIES.get(Identifier.of("hyacinth", "resizing_strategy/scale_smooth"));
    private DitheringStrategy ditheringStrategy = DitheringStrategy.DITHERING_STRATEGIES.get(Identifier.of("hyacinth", "dithering_strategy/floyd_steinberg"));
    private int mapWidth = 1;
    private int mapHeight = 1;
    private SupportMode supportMode = SupportMode.ONLY_REQUIRED;
    private StaircaseMode staircaseMode = StaircaseMode.CLASSIC;
    private boolean betterColor = true;
    private DitheringAlgorithm.Pixel[][] pixels = null;
    private BufferedImage image = null;

    public MapartPipeline() {
        selectedBlocks = new HashMap<>();
        tasks = new ArrayList<>();

        Function<BufferedImage, BufferedImage> resizingTask = image -> resizingStrategy.resize(image, mapWidth * 128, mapHeight * 128);
        tasks.add(resizingTask);
    }

    public @Nullable BufferedImage process() {
        long time = System.currentTimeMillis();
        BufferedImage image;
        try {
            if (file == null) {
                NativeImage defaultImage = MinecraftClient.getInstance().getGuiAtlasManager().getSprite(Identifier.of("hyacinth", "select_image")).getContents().image;
                image = ImageUtil.nativeToBufferedImage(defaultImage);
            } else {
                ImageIO.setUseCache(false); // TODO profile this?
                image = ImageIO.read(file);
            }

            if (image == null) return null;
            for (Function<BufferedImage, BufferedImage> task : tasks) {
                image = task.apply(image);
            }
            DitheringAlgorithm.DitheringResult ditheringResult = ditheringStrategy.ditheringAlgorithm().dither(image, selectedBlocks, (staircaseMode == StaircaseMode.VALLEY || staircaseMode == StaircaseMode.CLASSIC), betterColor);
            this.image = ditheringResult.image();
            pixels = ditheringResult.pixels();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Process completed in " + (System.currentTimeMillis() - time) + "ms.");
        return image;
    }

    public void exportToLitematica() {
        SchematicWriter.createSchematic(pixels, file.getName(), supportMode, staircaseMode);
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

    public BufferedImage getImage() {
        return this.image;
    }
}
