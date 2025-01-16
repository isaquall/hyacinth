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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class MapartScreen extends BaseUIModelScreen<GridLayout> {

    private static final Map<BlockPalette, BlockState> SELECTED_BLOCK = new HashMap<>();

    public MapartScreen() {
        super(GridLayout.class, DataSource.asset(Identifier.of("hyacinth", "mapart_ui_model")));
    }

    @Override
    protected void build(GridLayout rootComponent) {
        buildBlockPalette(rootComponent.childById(FlowLayout.class, "block_palette"));
        rootComponent.childById(ButtonComponent.class, "select_image_file").onPress(button -> openImageFileSelectionWindow(button, rootComponent));
        rootComponent.childById(ButtonComponent.class, "resizing_strategy").mouseDown().subscribe((x, y, button) -> {
            openResizingStrategiesDropdown(x, y, rootComponent.childById(FlowLayout.class, "resizing_strategy_container"));
            return true;
        });
    }

    private void openResizingStrategiesDropdown(double x, double y, FlowLayout container) {
        DropdownComponent dropdown = Components.dropdown(Sizing.content(5));
        dropdown.button(Text.of("Hi"), (component) -> {
            dropdown.remove();
        });
        dropdown.button(Text.of("Wawawajkajkfjkdsafkj"), (component) -> {
            dropdown.remove();
        });
        container.child(dropdown);
    }

    private void updateImage(File file, GridLayout rootComponent) {
        try {
            BufferedImage image = ImageIO.read(file);
            image.getScaledInstance(128, 128, Image.SCALE_DEFAULT);
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                image.getScaledInstance(128, 128, Image.SCALE_DEFAULT);
                ImageIO.write(image, "png", output);
                ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
                MinecraftClient.getInstance().getTextureManager().registerTexture(Identifier.of("hyacinth", file.getName() + "_resized"), new NativeImageBackedTexture(NativeImage.read(input)));
                input.close();
            }

            TextureComponent mapPreview = Components.texture(Identifier.of("hyacinth", file.getName() + "_resized"), 128, 128, 128, 128, 128, 128);
            rootComponent.child(mapPreview, 1, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openImageFileSelectionWindow(ButtonComponent button, GridLayout rootComponent) {
        UISounds.playButtonSound();
        button.active(false);

        String file = TinyFileDialogs.tinyfd_openFileDialog("Please select a file to import.", MinecraftClient.getInstance().runDirectory + File.separator + "hyacinth-input" + File.separator, null, null, false); // TODO make title translatable

        if (file != null) {
            updateImage(new File(file), rootComponent);
            UISounds.playButtonSound();
        }
        button.active(true);
    }

    private void buildBlockPalette(FlowLayout blockPalette) {
        for (BlockPalette palette : BlockPalette.BLOCK_PALETTES.values()) {

            FlowLayout color = Containers.horizontalFlow(Sizing.fill(95), Sizing.content(3));
            blockPalette.child(color);

            color.child(Components.box(Sizing.fixed(16), Sizing.fixed(16)).color(Color.ofRgb((palette.color()))).fill(true).tooltip(Text.of(palette.name())).margins(Insets.right(7)));

            SELECTED_BLOCK.computeIfAbsent(palette, k -> Blocks.BARRIER.getDefaultState());
            palette.states().addFirst(Blocks.BARRIER.getDefaultState());

            for (BlockState blockState : palette.states()) {
                RenderEffectWrapper<Component> blockStateRenderWrapper = Containers.renderEffect(createBlockStateComponent(blockState));
                blockStateRenderWrapper.tooltip(Text.translatable(blockState.getBlock().getTranslationKey()));

                if (SELECTED_BLOCK.get(palette) == blockState) {
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
                    SELECTED_BLOCK.put(palette, blockState);
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
