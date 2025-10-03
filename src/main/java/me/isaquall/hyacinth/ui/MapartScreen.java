package me.isaquall.hyacinth.ui;

import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.container.RenderEffectWrapper;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.util.UISounds;
import me.isaquall.hyacinth.block_palette.BlockPalette;
import me.isaquall.hyacinth.client.MapartPipeline;
import me.isaquall.hyacinth.dithering.DitheringStrategy;
import me.isaquall.hyacinth.schematic.StaircaseMode;
import me.isaquall.hyacinth.schematic.SupportMode;
import me.isaquall.hyacinth.ui.component.ButtonDropdownComponent;
import me.isaquall.hyacinth.util.ColorUtils;
import me.isaquall.hyacinth.util.ImageUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@SuppressWarnings("UnstableApiUsage")
@Environment(EnvType.CLIENT)
public class MapartScreen extends BaseUIModelScreen<GridLayout> { // TODO standardize component id's with either - or _ :(

    private static GridLayout rootComponent;
    private static double previewScale = 1;
    
    private static final RenderEffectWrapper.RenderEffect HIGHLIGHT = new RenderEffectWrapper.RenderEffect() {
        @Override
        public void setup(Component component, DrawContext context, float partialTicks, float delta) {
            context.fill(component.x(), component.y(), component.x() + 24, component.y() + 24, ColorHelper.getArgb(100, 152, 251, 152));
        }

        @Override
        public void cleanup(Component component, DrawContext context, float partialTicks, float delta) { }
    };

    private static final RenderEffectWrapper.RenderEffect GRID = new RenderEffectWrapper.RenderEffect() {
        @Override
        public void setup(Component component, DrawContext drawContext, float v, float v1) {
            ParentComponent parent = component.parent();
            if (parent == null) return;
            drawContext.enableScissor(parent.x(), parent.y(), parent.x() + parent.width(), parent.y() + parent.height());
            for (int x = 0; x < RENDER_PIPELINE.mapWidth(); x++) {
                for (int y = 0; y < RENDER_PIPELINE.mapHeight(); y++) {
                    drawContext.drawBorder((int) (component.x() + (128 * x * previewScale)), (int) (component.y() + (128 * y * previewScale)), (int) (128 * previewScale), (int) (128 * previewScale), Color.BLUE.argb());
                    for (int i = 1; i < 8; i++) {
                        drawContext.drawVerticalLine((int) (component.x() + (128 * x * previewScale) + (16 * i * previewScale)), (int) (component.y() + (128 * y * previewScale)), (int) (component.y() + (128 * (y + 1) * previewScale)), Color.RED.argb());
                        drawContext.drawHorizontalLine((int) (component.x() + (128 * x * previewScale)), (int) (component.x() + (128 * (x + 1) * previewScale)), (int) (component.y() + (128 * y * previewScale) + (16 * i * previewScale)), Color.RED.argb());
                    }
                }
            }
        }

        @Override
        public void cleanup(Component component, DrawContext drawContext, float v, float v1) {
            drawContext.disableScissor();
        }
    };

    private static final MapartPipeline RENDER_PIPELINE = new MapartPipeline();
    private static final Identifier MAP_PREVIEW_ID = Identifier.of("hyacinth", "map_preview");

    public MapartScreen() {
        super(GridLayout.class, DataSource.asset(Identifier.of("hyacinth", "mapart_ui_model")));
    }

    @Override
    protected void build(GridLayout rootComponent) {
        this.rootComponent = rootComponent;
        
        buildBlockPalette(rootComponent.childById(FlowLayout.class, "block_palette"));

        rootComponent.childById(SmallCheckboxComponent.class, "checkbox_show_grid").onChanged().subscribe(checked -> {
            RenderEffectWrapper<?> renderEffectWrapper = rootComponent.childById(RenderEffectWrapper.class, "map-preview-effect-wrapper");
            if (renderEffectWrapper == null) return;
            if (checked) {
                renderEffectWrapper.effect(GRID);
            } else {
                renderEffectWrapper.clearEffects();
            }
        });

        rootComponent.childById(SmallCheckboxComponent.class, "checkbox_better_color").onChanged().subscribe(checked -> {
            RENDER_PIPELINE.betterColor(checked);
            reprocessImage();
        });

        FlowLayout renderSettings = rootComponent.childById(FlowLayout.class, "render-settings");

        renderSettings.child(new ButtonDropdownComponent<>(renderSettings, "hyacinth.dithering_strategy", DitheringStrategy.DITHERING_STRATEGIES.values().toArray(new DitheringStrategy[]{}), DitheringStrategy::translatableName, null, strategy -> {
            RENDER_PIPELINE.ditheringStrategy(strategy);
            reprocessImage();
        }, RENDER_PIPELINE.ditheringStrategy()));

        FlowLayout schematicSettings = rootComponent.childById(FlowLayout.class, "schematic-settings");

        schematicSettings.child(new ButtonDropdownComponent<>(schematicSettings, "hyacinth.support_mode", SupportMode.values(), SupportMode::translatableName, SupportMode::translatableTooltip, RENDER_PIPELINE::supportMode, RENDER_PIPELINE.supportMode()));
        schematicSettings.child(new ButtonDropdownComponent<>(schematicSettings, "hyacinth.staircase_mode", StaircaseMode.values(), StaircaseMode::translatableName, StaircaseMode::translatableTooltip, mode -> {
            RENDER_PIPELINE.staircaseMode(mode);
            reprocessImage();
        }, RENDER_PIPELINE.staircaseMode()));

        schematicSettings.child(Components.button(Text.translatable("hyacinth.export_to_litematica"), button -> RENDER_PIPELINE.exportToLitematica()));

        rootComponent.childById(TextureComponent.class, "download-button").mouseDown().subscribe((x, y, button) -> {
            (new File(MinecraftClient.getInstance().runDirectory + File.separator + "hyacinth" + File.separator)).mkdirs();
            String file = TinyFileDialogs.tinyfd_saveFileDialog("Please select file save location.", MinecraftClient.getInstance().runDirectory + File.separator + "hyacinth" + File.separator + "untitled.png", null, null); // TODO make title translatable
            if (file == null) return true;
            try {
                ImageIO.write(RENDER_PIPELINE.finishedImage(), "png", new File(file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return true;
        });

        LabelComponent sizeLabel = rootComponent.childById(LabelComponent.class, "size-label").text(Text.translatable("hyacinth.size_percent", Math.round(previewScale * 100)).formatted(Formatting.ITALIC, Formatting.GRAY));
        SlimSliderComponent slider = rootComponent.childById(SlimSliderComponent.class, "size-slider");
        slider.cursorStyle(CursorStyle.HORIZONTAL_RESIZE);
        slider.onChanged().subscribe(value -> {
            previewScale = value;
            sizeLabel.text(Text.translatable("hyacinth.size_percent", Math.round(previewScale * 100)).formatted(Formatting.ITALIC, Formatting.GRAY));
            redrawImage();
        });

        reprocessImage();
    }

    public void reprocessImage() {
        if (RENDER_PIPELINE.baseImage() == null) {
            NativeImage defaultImage = MinecraftClient.getInstance().getGuiAtlasManager().getSprite(Identifier.of("hyacinth", "select_image")).getContents().image;
            RENDER_PIPELINE.baseImage(ImageUtils.nativeToBufferedImage(defaultImage));
        }
        BufferedImage image = RENDER_PIPELINE.process();
        if (image == null) return;
        ImageUtils.bufferedToNativeImage(image, MAP_PREVIEW_ID);

        redrawImage();
    }

    public void redrawImage() {
        RenderEffectWrapper<Component> mapPreviewEffectWrapper = Containers.renderEffect(Components.texture(MAP_PREVIEW_ID, 0, 0, (int) (RENDER_PIPELINE.mapWidth() * 128 * previewScale), (int) (RENDER_PIPELINE.mapHeight() * 128 * previewScale), (int) (RENDER_PIPELINE.mapWidth() * 128 * previewScale), (int) (RENDER_PIPELINE.mapHeight() * 128 * previewScale)).id("map-preview"));
        mapPreviewEffectWrapper.id("map-preview-effect-wrapper");
        mapPreviewEffectWrapper.positioning(Positioning.relative(50, 50));
        FlowLayout previewContainer = rootComponent.childById(FlowLayout.class, "map-preview-container");
        RenderEffectWrapper<?> existingWrapper = previewContainer.childById(RenderEffectWrapper.class, "map-preview-effect-wrapper");
        if (existingWrapper != null) existingWrapper.remove();
        previewContainer.child(mapPreviewEffectWrapper);

        mapPreviewEffectWrapper.mouseDown().subscribe((x, y, button) -> {
            openImageFileSelectionWindow();
            return true;
        });

        if (rootComponent.childById(SmallCheckboxComponent.class, "checkbox_show_grid").checked()) {
            mapPreviewEffectWrapper.effect(GRID);
        }
    }

    private void openImageFileSelectionWindow() {
        UISounds.playButtonSound();
        (new File(MinecraftClient.getInstance().runDirectory + File.separator + "hyacinth" + File.separator)).mkdirs();
        String path = TinyFileDialogs.tinyfd_openFileDialog("Please select a file to import.", MinecraftClient.getInstance().runDirectory + File.separator + "hyacinth-input" + File.separator, null, null, false); // TODO make title translatable

        if (path != null) {
            File file = new File(path);
            try {
                BufferedImage image = ImageIO.read(file);
                if (image.getWidth() % 128 != 0 || image.getHeight() % 128 != 0) {
                    this.client.setScreen(new CropScreen(this, image));
                } else {
                    RENDER_PIPELINE.baseImage(image);
                    RENDER_PIPELINE.mapHeight(image.getHeight() / 128);
                    RENDER_PIPELINE.mapWidth(image.getWidth() / 128);
                    reprocessImage();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void buildBlockPalette(FlowLayout blockPalette) {
        for (BlockPalette palette : BlockPalette.BLOCK_PALETTES.values()) {

            FlowLayout color = Containers.horizontalFlow(Sizing.fill(95), Sizing.content(3));
            blockPalette.child(color);

            FlowLayout swatch = Containers.verticalFlow(Sizing.fixed(24), Sizing.fixed(24));
            swatch.tooltip(Text.translatable(palette.translatableName())).margins(Insets.right(7));
            int[] swatchColors = ColorUtils.getVariations(palette.color());
            for (int i = 0; i < 3; i++) {
                swatch.child(Components.box(Sizing.fixed(24), Sizing.fixed(8)).color(Color.ofRgb(swatchColors[i])).fill(true));
            }
            color.child(swatch);

            RENDER_PIPELINE.selectedBlocks().computeIfAbsent(palette, p -> p.states().getFirst());
            palette.states().addFirst(Blocks.BARRIER.getDefaultState());

            for (BlockState blockState : palette.states()) {
                RenderEffectWrapper<Component> blockStateRenderWrapper = Containers.renderEffect(createBlockStateComponent(blockState));
                blockStateRenderWrapper.tooltip(Text.translatable(translationKey(blockState)));

                if (RENDER_PIPELINE.selectedBlocks().get(palette) == blockState) {
                    blockStateRenderWrapper.effect(HIGHLIGHT);
                }

                color.child(blockStateRenderWrapper);
                blockStateRenderWrapper.mouseDown().subscribe((x, y, button) -> {
                    for (Component child : color.children()) {
                        if (child instanceof RenderEffectWrapper<?> renderEffectWrapper) {
                            renderEffectWrapper.clearEffects();
                        }
                    }

                    blockStateRenderWrapper.effect(HIGHLIGHT);
                    RENDER_PIPELINE.selectedBlocks().put(palette, blockState);
                    UISounds.playButtonSound();
                    reprocessImage();
                    return true;
                });
            }
        }
    }

    private Component createBlockStateComponent(BlockState blockState) {
        if (blockState == Blocks.BARRIER.getDefaultState()) {
            return Components.item(Items.BARRIER.getDefaultStack()).sizing(Sizing.fixed(24));
        } else if (blockState == Blocks.COBWEB.getDefaultState()) {
            return Components.item(Items.COBWEB.getDefaultStack()).sizing(Sizing.fixed(24));
        } else {
            return Components.block(blockState).sizing(Sizing.fixed(24));
        }
    }

    private String translationKey(BlockState blockState) {
        if (blockState == Blocks.BARRIER.getDefaultState()) {
            return "hyacinth.disabled";
        } else {
            return blockState.getBlock().getTranslationKey();
        }
    }

    public static MapartPipeline renderPipeline() {
        return RENDER_PIPELINE;
    }

    @Override
    public void close() {
        super.close();
        RENDER_PIPELINE.clearData();
    }
}
