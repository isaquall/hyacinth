package me.isaquall.hyacinth.ui;

import fi.dy.masa.litematica.util.FileType;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.container.RenderEffectWrapper;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.util.UISounds;
import me.isaquall.hyacinth.ColorUtils;
import me.isaquall.hyacinth.block_palette.BlockPalette;
import me.isaquall.hyacinth.client.MapartPipeline;
import me.isaquall.hyacinth.dithering.DitheringStrategy;
import me.isaquall.hyacinth.mixin.DropdownComponentAccessor;
import me.isaquall.hyacinth.resizing_strategy.ResizingStrategy;
import me.isaquall.hyacinth.schematic.SupportMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
@Environment(EnvType.CLIENT)
public class MapartScreen extends BaseUIModelScreen<GridLayout> {

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
            for (int x = 0; x < RENDER_PIPELINE.mapWidth(); x++) {
                for (int y = 0; y < RENDER_PIPELINE.mapHeight(); y++) {
                    drawContext.drawBorder(component.x() + (128 * x), component.y() + (128 * y), 128, 128, Color.BLUE.argb());
                    for (int i = 1; i < 8; i++) {
                        drawContext.drawVerticalLine(component.x() + (128 * x) + (16 * i), component.y() + (128 * y), component.y() + (128 * (y + 1)), Color.RED.argb());
                        drawContext.drawHorizontalLine(component.x() + (128 * x), component.x() + (128 * (x + 1)), component.y() + (128 * y) + (16 * i), Color.RED.argb());
                    }
                }
            }
        }

        @Override
        public void cleanup(Component component, DrawContext drawContext, float v, float v1) { }
    };

    private static final MapartPipeline RENDER_PIPELINE = new MapartPipeline();

    public MapartScreen() {
        super(GridLayout.class, DataSource.asset(Identifier.of("hyacinth", "mapart_ui_model")));
    }

    @Override
    protected void build(GridLayout rootComponent) {
        buildBlockPalette(rootComponent.childById(FlowLayout.class, "block_palette"), rootComponent);
        rootComponent.childById(ButtonComponent.class, "select_image_file").onPress(button -> openImageFileSelectionWindow(button, rootComponent));

        rootComponent.childById(TextBoxComponent.class, "map_width").text(String.valueOf(RENDER_PIPELINE.mapWidth())).onChanged().subscribe(width -> {
            if (width.isEmpty()) return;
            RENDER_PIPELINE.mapWidth(Integer.parseInt(width));
            redrawImage(rootComponent);
        });

        rootComponent.childById(TextBoxComponent.class, "map_height").text(String.valueOf(RENDER_PIPELINE.mapHeight())).onChanged().subscribe(height -> {
            if (height.isEmpty()) return;
            RENDER_PIPELINE.mapHeight(Integer.parseInt(height));
            redrawImage(rootComponent);
        });

        rootComponent.childById(SmallCheckboxComponent.class, "checkbox_show_grid").onChanged().subscribe(checked -> {
            RenderEffectWrapper<?> renderEffectWrapper = rootComponent.childById(RenderEffectWrapper.class, "map-preview-effect-wrapper");
            if (renderEffectWrapper == null) return;
            if (checked) {
                renderEffectWrapper.effect(GRID);
            } else {
                renderEffectWrapper.clearEffects();
            }
        });

        ButtonComponent resizingStrategyButton = rootComponent.childById(ButtonComponent.class, "resizing_strategy");
        resizingStrategyButton.mouseDown().subscribe((x, y, button) -> {
            UISounds.playButtonSound();
            createOptionDropdown(rootComponent.childById(FlowLayout.class, "resizing_strategy_container"), resizingStrategyButton, rootComponent, ResizingStrategy.RESIZING_STRATEGIES.values(), ResizingStrategy::translatableName, null, RENDER_PIPELINE::resizingStrategy);
            return true;
        });
        resizingStrategyButton.setMessage(Text.translatable(RENDER_PIPELINE.resizingStrategy().translatableName()));

        ButtonComponent ditheringMatrixButton = rootComponent.childById(ButtonComponent.class, "dithering_strategy");
        ditheringMatrixButton.mouseDown().subscribe((x, y, button) -> {
            UISounds.playButtonSound();
            createOptionDropdown(rootComponent.childById(FlowLayout.class, "dithering_strategy_container"), ditheringMatrixButton, rootComponent, DitheringStrategy.DITHERING_STRATEGIES.values(), DitheringStrategy::translatableName, null, RENDER_PIPELINE::ditheringStrategy);
            return true;
        });
        ditheringMatrixButton.setMessage(Text.translatable(RENDER_PIPELINE.ditheringStrategy().translatableName()));

        ButtonComponent exportTypeButton = rootComponent.childById(ButtonComponent.class, "export_type");
        exportTypeButton.mouseDown().subscribe((x, y, button) -> {
            UISounds.playButtonSound();
            createOptionDropdown(rootComponent.childById(FlowLayout.class, "export_type_container"), exportTypeButton, rootComponent, List.of(FileType.values()), FileType::name, null, RENDER_PIPELINE::exportType);
            return true;
        });
        exportTypeButton.setMessage(Text.literal(RENDER_PIPELINE.exportType().name()));

        ButtonComponent supportModeButton = rootComponent.childById(ButtonComponent.class, "support_mode");
        supportModeButton.mouseDown().subscribe((x, y, button) -> {
            UISounds.playButtonSound();
            createOptionDropdown(rootComponent.childById(FlowLayout.class, "support_mode_container"), supportModeButton, rootComponent, List.of(SupportMode.values()), SupportMode::translatableName, SupportMode::translatableTooltip, RENDER_PIPELINE::supportMode);
            return true;
        });
        supportModeButton.setMessage(Text.translatable(RENDER_PIPELINE.supportMode().translatableName()));

        redrawImage(rootComponent);
    }

    private <T> void createOptionDropdown(FlowLayout container, ButtonComponent button, GridLayout rootComponent, Collection<T> options, Function<T, String> nameFunction, @Nullable Function<T, String> tooltipFunction, Consumer<T> update) {
        if (!button.active()) return;

        button.active(false);
        DropdownComponent dropdown = Components.dropdown(Sizing.content(5));
        for (T option : options) {
            MutableText name = Text.translatable(nameFunction.apply(option));
            dropdown.button(name, component -> {
                component.remove();
                update.accept(option);
                button.setMessage(name);
                redrawImage(rootComponent);
                button.active(true);
            });

            // Dumb hack because we can't change the tooltip from the DropdownComponent builder
            if (tooltipFunction != null) {
                for (Component component : ((DropdownComponentAccessor) dropdown).getEntry().children()) {
                    if (component instanceof LabelComponent label) {
                        if (label.text() == name) {
                            label.tooltip(Text.translatable(tooltipFunction.apply(option)));
                        }
                    }
                }
            }
        }
        container.child(dropdown);
    }

    private void redrawImage(GridLayout rootComponent) {
        Identifier id = Identifier.of("hyacinth", "map_preview");

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BufferedImage image = RENDER_PIPELINE.process();
            if (image == null) return;
            ImageIO.write(image, "png", output); // TODO maybe find a way to do this without having to do IO?
            ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(NativeImage.read(input)));
            input.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        RenderEffectWrapper<TextureComponent> mapPreviewEffectWrapper = Containers.renderEffect(Components.texture(id, 0, 0, RENDER_PIPELINE.mapWidth() * 128, RENDER_PIPELINE.mapHeight() * 128, RENDER_PIPELINE.mapWidth() * 128, RENDER_PIPELINE.mapHeight() * 128));
        mapPreviewEffectWrapper.id("map-preview-effect-wrapper");
        mapPreviewEffectWrapper.positioning(Positioning.relative(50, 50));
        FlowLayout previewContainer = rootComponent.childById(FlowLayout.class, "map-preview-container");
        previewContainer.clearChildren();
        previewContainer.child(mapPreviewEffectWrapper);

        if (rootComponent.childById(SmallCheckboxComponent.class, "checkbox_show_grid").checked()) {
            mapPreviewEffectWrapper.effect(GRID);
        }
    }

    private void openImageFileSelectionWindow(ButtonComponent button, GridLayout rootComponent) {
        UISounds.playButtonSound();
        button.active(false);
        (new File(MinecraftClient.getInstance().runDirectory + File.separator + "hyacinth-input" + File.separator)).mkdirs();
        String file = TinyFileDialogs.tinyfd_openFileDialog("Please select a file to import.", MinecraftClient.getInstance().runDirectory + File.separator + "hyacinth-input" + File.separator, null, null, false); // TODO make title translatable

        if (file != null) {
            RENDER_PIPELINE.openFile(new File(file));
            redrawImage(rootComponent);
            UISounds.playButtonSound();
        }
        button.active(true);
    }

    private void buildBlockPalette(FlowLayout blockPalette, GridLayout rootComponent) {
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
                blockStateRenderWrapper.tooltip(Text.translatable(getTranslationKey(blockState)));

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
                    redrawImage(rootComponent);
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

    private String getTranslationKey(BlockState blockState) {
        if (blockState == Blocks.BARRIER.getDefaultState()) {
            return "hyacinth.disabled";
        } else {
            return blockState.getBlock().getTranslationKey();
        }
    }

    public static MapartPipeline getRenderPipeline() {
        return RENDER_PIPELINE;
    }
}
