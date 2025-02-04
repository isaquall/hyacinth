package me.isaquall.hyacinth.client;

import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.litematica.util.PositionUtils;
import me.isaquall.hyacinth.block_palette.BlockPalette;
import me.isaquall.hyacinth.dithering.DitheringStrategy;
import me.isaquall.hyacinth.dithering.algorithm.DitheringAlgorithm;
import me.isaquall.hyacinth.mixin.LitematicaSchematicMixin;
import me.isaquall.hyacinth.resizing_strategy.ResizingStrategy;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
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
    private DitheringStrategy ditheringStrategy = DitheringStrategy.DITHERING_STRATEGIES.get(Identifier.of("hyacinth", "dithering_strategy/atkinson"));
    private int mapWidth = 1;
    private int mapHeight = 1;

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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Process completed in " + (System.currentTimeMillis() - time) + "ms.");

        LitematicaSchematic schematic = LitematicaSchematicMixin.newLitematicaSchematic(null);
//        schematic.setSubRegionPositions(boxes, area.getEffectiveOrigin());
//        schematic.setSubRegionSizes(boxes);
        schematic.getMetadata().setAuthor(MinecraftClient.getInstance().player.getName().getString());
        schematic.getMetadata().setName("Mapart Schematic");
        schematic.getMetadata().setRegionCount(1);

        Box box = new Box(new BlockPos(0, 0, 0), new BlockPos(8, 8, 8), "box");
        schematic.getMetadata().setTotalVolume(PositionUtils.getTotalVolume(List.of(box)));
        schematic.getMetadata().setEnclosingSize(PositionUtils.getEnclosingAreaSize(List.of(box)));
        ((LitematicaSchematicMixin) schematic).getSubRegionSizes().put("box", new BlockPos(8, 8, 8));
        ((LitematicaSchematicMixin) schematic).getSubRegionPositions().put("box", new BlockPos(0, 0, 0));
        ((LitematicaSchematicMixin) schematic).getTileEntities().put("box", new HashMap<>());

        schematic.getMetadata().setSchematicVersion(7);
        schematic.getMetadata().setMinecraftDataVersion(LitematicaSchematic.MINECRAFT_DATA_VERSION);
        schematic.getMetadata().setFileType(FileType.LITEMATICA_SCHEMATIC);
        LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(Math.abs(box.getSize().getX()), Math.abs(box.getSize().getY()), Math.abs(box.getSize().getZ()));

        ((LitematicaSchematicMixin) schematic).getBlockContainers().put("box", container);
        container.set(1, 1, 1, Blocks.REDSTONE_BLOCK.getDefaultState());
        container.set(2, 2, 2, Blocks.REDSTONE_BLOCK.getDefaultState());
        container.set(4, 4, 4, Blocks.REDSTONE_BLOCK.getDefaultState());
        container.set(0, 4, 6, Blocks.REDSTONE_BLOCK.getDefaultState());

        schematic.getMetadata().setTotalBlocks(4);

        SchematicHolder.getInstance().addSchematic(schematic, false);
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

    public List<Function<BufferedImage, BufferedImage>> tasks() {
        return tasks;
    }
}
