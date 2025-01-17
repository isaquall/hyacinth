package me.isaquall.hyacinth.client;

import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.DropdownComponent;
import io.wispforest.owo.ui.component.TextureComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.container.RenderEffectWrapper;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.util.UISounds;
import me.isaquall.hyacinth.block_palette.BlockPalette;
import me.isaquall.hyacinth.resizing_strategy.HyacinthResizingStrategies;
import me.isaquall.hyacinth.resizing_strategy.ResizingStrategy;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class MapartScreen extends BaseUIModelScreen<GridLayout> {

    public MapartScreen() {
        super(GridLayout.class, DataSource.asset(Identifier.of("hyacinth", "mapart_ui_model")));
    }

    @Override
    protected void build(GridLayout rootComponent) {
        buildBlockPalette(rootComponent.childById(FlowLayout.class, "block_palette"));
        rootComponent.childById(ButtonComponent.class, "select_image_file").onPress(button -> openImageFileSelectionWindow(button, rootComponent));

        ButtonComponent resizingStrategyButton = rootComponent.childById(ButtonComponent.class, "resizing_strategy");
        resizingStrategyButton.mouseDown().subscribe((x, y, button) -> {
            openResizingStrategiesDropdown(rootComponent.childById(FlowLayout.class, "resizing_strategy_container"), resizingStrategyButton, rootComponent);
            return true;
        });
        resizingStrategyButton.setMessage(Text.translatable("hyacinth.scale_smooth"));
    }

    private void openResizingStrategiesDropdown(FlowLayout container, ButtonComponent resizingStrategyButton, GridLayout rootComponent) {
        if (!resizingStrategyButton.active()) return;

        resizingStrategyButton.active(false);
        DropdownComponent dropdown = Components.dropdown(Sizing.content(5));
        for (ResizingStrategy strategy : HyacinthResizingStrategies.RESIZING_STRATEGIES.values()) {
            dropdown.button(Text.translatable(strategy.name()), component -> {
                    component.remove();
                    HyacinthClient.getRenderPipeline().resizingStrategy(strategy);
                    resizingStrategyButton.setMessage(Text.translatable(strategy.name()));
                    redrawImage(rootComponent);
                    resizingStrategyButton.active(true);
            });
        }
        container.child(dropdown);
    }

    private void redrawImage(GridLayout rootComponent) {
        Identifier id = Identifier.of("hyacinth", UUID.randomUUID() + "_resized");

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BufferedImage image = HyacinthClient.getRenderPipeline().process();
            ImageIO.write(image, "png", output);
            ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(NativeImage.read(input)));
            input.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        TextureComponent mapPreview = Components.texture(id, 128, 128, 128, 128, 128, 128);
        rootComponent.child(mapPreview, 1, 0);
    }

    private void openImageFileSelectionWindow(ButtonComponent button, GridLayout rootComponent) {
        UISounds.playButtonSound();
        button.active(false);

        String file = TinyFileDialogs.tinyfd_openFileDialog("Please select a file to import.", MinecraftClient.getInstance().runDirectory + File.separator + "hyacinth-input" + File.separator, null, null, false); // TODO make title translatable

        if (file != null) {
            HyacinthClient.getRenderPipeline().openFile(new File(file));
            redrawImage(rootComponent);
            UISounds.playButtonSound();
        }
        button.active(true);
    }

    private void buildBlockPalette(FlowLayout blockPalette) {
        for (BlockPalette palette : BlockPalette.BLOCK_PALETTES.values()) {

            FlowLayout color = Containers.horizontalFlow(Sizing.fill(95), Sizing.content(3));
            blockPalette.child(color);

            color.child(Components.box(Sizing.fixed(16), Sizing.fixed(16)).color(Color.ofRgb((palette.color()))).fill(true).tooltip(Text.of(palette.name())).margins(Insets.right(7)));

            HyacinthClient.getRenderPipeline().selectedBlocks().computeIfAbsent(palette, k -> Blocks.BARRIER.getDefaultState());
            palette.states().addFirst(Blocks.BARRIER.getDefaultState());

            for (BlockState blockState : palette.states()) {
                RenderEffectWrapper<Component> blockStateRenderWrapper = Containers.renderEffect(createBlockStateComponent(blockState));
                blockStateRenderWrapper.tooltip(Text.translatable(blockState.getBlock().getTranslationKey()));

                if (HyacinthClient.getRenderPipeline().selectedBlocks().get(palette) == blockState) {
                    blockStateRenderWrapper.effect(RenderEffectWrapper.RenderEffect.color(Color.GREEN));
                }

                color.child(blockStateRenderWrapper);
                blockStateRenderWrapper.mouseDown().subscribe((x, y, button) -> {
                    for (Component child : color.children()) {
                        if (child instanceof RenderEffectWrapper<?> renderEffectWrapper) {
                            renderEffectWrapper.clearEffects();
                        }
                    }

                    blockStateRenderWrapper.effect(RenderEffectWrapper.RenderEffect.color(Color.GREEN));
                    HyacinthClient.getRenderPipeline().selectedBlocks().put(palette, blockState);
                    UISounds.playButtonSound();
                    return true;
                });
            }
        }
    }

    private Component createBlockStateComponent(BlockState blockState) {
        if (blockState == Blocks.BARRIER.getDefaultState()) {
            return Components.item(Items.BARRIER.getDefaultStack()).sizing(Sizing.fixed(16));
        } else if (blockState == Blocks.WATER.getDefaultState()) {
            return Components.item(Items.WATER_BUCKET.getDefaultStack()).sizing(Sizing.fixed(16));
        } else {
            return Components.block(blockState).sizing(Sizing.fixed(16));
        }
    }
}
