package me.isaquall.hyacinth.client;

import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextureComponent;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.util.UISounds;
import me.isaquall.hyacinth.block_palette.BlockPalette;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class MapartScreen extends BaseUIModelScreen<GridLayout> {

//     TODO the selection stuff is a little jank rn
    private static final Map<Identifier, Pair<BlockState, RenderEffectWrapper<Component>>> SELECTED_BLOCK = new HashMap<>();

    public MapartScreen() {
        super(GridLayout.class, DataSource.asset(Identifier.of("hyacinth", "mapart_ui_model")));
    }

    @Override
    protected void build(GridLayout rootComponent) {
        buildBlockPalette(rootComponent.childById(FlowLayout.class, "block_palette"));
        rootComponent.childById(ButtonComponent.class, "select_image_file").onPress(button -> openImageFileSelectionWindow(button, rootComponent));
    }

    private void updateImage(File file, GridLayout rootComponent) {
        try {
            BufferedImage image = ImageIO.read(file);
            image.getScaledInstance(128, 128, Image.SCALE_DEFAULT);
            try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                 ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray())) {
                ImageIO.write(image, "png", output);
                MinecraftClient.getInstance().getTextureManager().registerTexture(Identifier.of("hyacinth", file.getName() + "_resized"), new NativeImageBackedTexture(NativeImage.read(input)));
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

        FlowLayout selectionFlow = Containers.verticalFlow(Sizing.fill(), Sizing.fill());

        DraggableContainer<Component> selectionWindow = Containers.draggable(Sizing.fill(50), Sizing.fill(20), selectionFlow);
        selectionWindow.zIndex(2);
        selectionWindow.foreheadSize(10);
        selectionWindow.surface(Surface.PANEL);
        selectionWindow.verticalAlignment(VerticalAlignment.CENTER);
        selectionWindow.horizontalAlignment(HorizontalAlignment.CENTER);
        rootComponent.child(selectionWindow, 1, 0);

        LabelComponent header = Components.label(Text.translatable("hyacinth.select_file"));
        header.margins(Insets.left(5));

        FlowLayout entries = Containers.verticalFlow(Sizing.fill(), Sizing.fill());

        ScrollContainer<Component> verticalScroll = Containers.verticalScroll(Sizing.fill(), Sizing.fill(), entries);
        verticalScroll.scrollbar(ScrollContainer.Scrollbar.vanilla());
        verticalScroll.scrollbarThiccness(3);
        verticalScroll.margins(Insets.left(5).withRight(5).withBottom(15).withTop(10));

        selectionFlow.child(header);
        selectionFlow.child(verticalScroll);

        File inputDir = new File(MinecraftClient.getInstance().runDirectory + File.separator + "hyacinth-input");
        inputDir.mkdirs();

        for (File file : inputDir.listFiles()) {
            if (file.isDirectory()) continue;
            if (!file.getPath().endsWith(".png")) continue;

            LabelComponent text = Components.label(Text.of(file.getName()));
            entries.child(text);
            text.mouseDown().subscribe((x, y, b) -> {
                button.active(true);
                updateImage(file, rootComponent);
                return true;
            });
            text.color().animate(500, Easing.QUADRATIC, Color.GREEN);
            text.mouseEnter().subscribe(() -> text.color().animation().forwards());
            text.mouseLeave().subscribe(() -> text.color().animation().backwards());
        }
    }

    private void buildBlockPalette(FlowLayout blockPalette) {
        for (BlockPalette palette : BlockPalette.BLOCK_PALETTES.values()) {
            Identifier id = palette.id();

            FlowLayout color = Containers.horizontalFlow(Sizing.fill(95), Sizing.content(3));
            blockPalette.child(color);

            color.child(Components.box(Sizing.fixed(16), Sizing.fixed(16)).color(Color.ofRgb((palette.color()))).fill(true).tooltip(Text.of(palette.name())).margins(Insets.right(7)));
            RenderEffectWrapper<Component> barrier = Containers.renderEffect(Components.item(Items.BARRIER.getDefaultStack()).sizing(Sizing.fixed(16)).tooltip(Text.of("Disable")));
            color.child(barrier);

            barrier.mouseDown().subscribe((x, y, button) -> {
                if (SELECTED_BLOCK.get(id).getLeft() != null) {
                    SELECTED_BLOCK.get(palette.id()).getRight().clearEffects();
                    SELECTED_BLOCK.put(palette.id(), Pair.of(null, barrier));
                    barrier.effect(RenderEffectWrapper.RenderEffect.color(Color.GREEN));
                    System.out.println(SELECTED_BLOCK);
                    UISounds.playButtonSound();
                }
                return true;
            });

            SELECTED_BLOCK.put(id, Pair.of(null, barrier)); // by default, barrier/null is selected
            barrier.effect(RenderEffectWrapper.RenderEffect.color(Color.GREEN));

            for (BlockState blockState : palette.states()) {
                RenderEffectWrapper<Component> block = Containers.renderEffect(Components.block(blockState).sizing(Sizing.fixed(16)));
                block.tooltip(Text.translatable(blockState.getBlock().getTranslationKey()));

                color.child(block);
                block.mouseDown().subscribe((x, y, button) -> {
                    if (SELECTED_BLOCK.get(id).getLeft() != blockState) {
                        SELECTED_BLOCK.get(palette.id()).getRight().clearEffects();
                        SELECTED_BLOCK.put(palette.id(), Pair.of(blockState, block));
                        block.effect(RenderEffectWrapper.RenderEffect.color(Color.GREEN));
                        UISounds.playButtonSound();
                    }
                    return true;
                });
            }
        }
    }
}
