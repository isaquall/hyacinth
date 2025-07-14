package me.isaquall.hyacinth.ui;

import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.container.RenderEffectWrapper;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Sizing;
import me.isaquall.hyacinth.util.ImageUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public class CropScreen extends BaseUIModelScreen<FlowLayout> {

    private FlowLayout rootComponent;
    private final MapartScreen parent;
    private final BufferedImage originalImage;
    private BufferedImage resizedImage;
    private int cropWidth = 1;
    private int cropHeight = 1;
    private int cropX = 0;
    private int cropY = 0;
    private TextBoxComponent cropXDisplay;
    private TextBoxComponent cropYDisplay;
    private int imageWidth;
    private int imageHeight;
    private TextBoxComponent imageWidthDisplay;
    private TextBoxComponent imageHeightDisplay;

    private final RenderEffectWrapper.RenderEffect CROP_OVERLAY = new RenderEffectWrapper.RenderEffect() {
        @Override
        public void setup(Component component, DrawContext drawContext, float v, float v1) {
            drawContext.drawBorder(component.x() + cropX, component.y() + cropY, cropWidth * 128, cropHeight * 128, Color.RED.argb());
        }

        @Override
        public void cleanup(Component component, DrawContext drawContext, float v, float v1) { }
    };

    public CropScreen(MapartScreen parent, BufferedImage originalImage) {
        super(FlowLayout.class, DataSource.asset(Identifier.of("hyacinth", "crop_ui_model")));

        this.parent = parent;
        this.originalImage = originalImage;
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        this.rootComponent = rootComponent;

        this.imageWidth = originalImage.getWidth();
        this.imageHeight = originalImage.getHeight();
        this.resizedImage = originalImage;

        GridLayout grid = rootComponent.childById(GridLayout.class, "crop-container");
        grid.child(Components.spacer().sizing(Sizing.fixed(50), Sizing.fixed(1)), 0, 1);

        FlowLayout cropSettings = rootComponent.childById(FlowLayout.class, "crop-settings");
        cropSettings.child(buildNumberDisplay("hyacinth.crop_x", textBoxComponent -> cropXDisplay = textBoxComponent, () -> cropX, x -> {
            clampCropX(x);
            cropXDisplay.setText(String.valueOf(cropX));
        }));
        cropSettings.child(buildNumberDisplay("hyacinth.crop_y", textBoxComponent -> cropYDisplay = textBoxComponent, () -> cropY, y -> {
            clampCropY(y);
            cropYDisplay.setText(String.valueOf(cropY));
        }));

        rootComponent.childById(ButtonComponent.class, "continue-button").onPress(button -> this.close());

        rootComponent.childById(TextBoxComponent.class, "crop-width").text(String.valueOf(cropHeight)).onChanged().subscribe(width -> {
            if (width.isEmpty()) return;
            this.cropHeight = Integer.parseInt(width);
        });

        rootComponent.childById(TextBoxComponent.class, "crop-height").text(String.valueOf(cropWidth)).onChanged().subscribe(height -> {
            if (height.isEmpty()) return;
            this.cropWidth = Integer.parseInt(height);
        });

        FlowLayout resizeOriginalImageContainer = rootComponent.childById(FlowLayout.class, "resize-original-image");

        resizeOriginalImageContainer.child(buildNumberDisplay("hyacinth.image_width", textBoxComponent -> imageWidthDisplay = textBoxComponent, () -> imageWidth, width -> {
            setImageSize(width, imageHeight);
            imageWidthDisplay.setText(String.valueOf(imageWidth));
        }));

        resizeOriginalImageContainer.child(buildNumberDisplay("hyacinth.image_height", textBoxComponent -> imageHeightDisplay = textBoxComponent, () -> imageHeight, height -> {
            setImageSize(imageWidth, height);
            imageHeightDisplay.setText(String.valueOf(imageHeight));
        }));

        buildMapPreview(this.originalImage);
    }

    private FlowLayout buildNumberDisplay(String translatableLabel, Consumer<TextBoxComponent> textBoxComponentConsumer, Supplier<Integer> get, Consumer<Integer> set) {
        FlowLayout numberDisplayComponent = this.model.expandTemplate(FlowLayout.class,
                "position@hyacinth:crop_ui_model",
                Map.of("axis", translatableLabel));
        TextBoxComponent textBox = numberDisplayComponent.childById(TextBoxComponent.class, "position-display");
        textBox.setText(String.valueOf(get.get()));
        textBox.onChanged().subscribe(value -> {
            if (value.isEmpty()) return;
            set.accept(Integer.valueOf(value));
        });
        textBoxComponentConsumer.accept(textBox);
        textBox.mouseScroll().subscribe((mouseX, mouseY, amount) -> {
            set.accept(get.get() + (int) amount);
            return true;
        });
        numberDisplayComponent.childById(ButtonComponent.class, "decrement").onPress(button -> set.accept(get.get() - 1));
        numberDisplayComponent.childById(ButtonComponent.class, "increment").onPress(button -> set.accept(get.get() + 1));
        return numberDisplayComponent;
    }

    private void buildMapPreview(BufferedImage image) {
        FlowLayout mapContainer = rootComponent.childById(FlowLayout.class, "map-preview-container").clearChildren();

        Identifier id = Identifier.of("hyacinth", "crop_preview");
        ImageUtils.bufferedToNativeImage(image, id);
        Component texture = new FocusableTextureComponent(id, 0, 0, image.getWidth(), image.getHeight(), image.getWidth(), image.getHeight()).id("crop-preview");
        RenderEffectWrapper<Component> cropRenderLayer = Containers.renderEffect(texture);
        cropRenderLayer.effect(CROP_OVERLAY);
        texture.mouseDrag().subscribe((mouseX, mouseY, deltaX, deltaY, button) -> {
            if (mouseX >= cropX && mouseX <= (cropX + (128 * cropWidth)) && mouseY >= cropY && mouseY <= (cropY + (128 * cropHeight))) {
                clampCropX(cropX + (int) deltaX);
                clampCropY(cropY + (int) deltaY);
                cropXDisplay.setText(String.valueOf(cropX));
                cropYDisplay.setText(String.valueOf(cropY));
                return true;
            }
            return false;
        });

        mapContainer.child(cropRenderLayer);
    }

    private void clampCropX(int x) {
        cropX = Math.clamp(x, 0, resizedImage.getWidth() - (128 * cropWidth));
    }

    private void clampCropY(int y) {
        cropY = Math.clamp(y, 0, resizedImage.getHeight() - (128 * cropHeight));
    }

    private void setImageSize(int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;

        this.resizedImage = ImageUtils.cloneBufferedImage(originalImage.getScaledInstance(imageWidth, imageHeight, Image.SCALE_SMOOTH));
        buildMapPreview(this.resizedImage);
    }

    @Override
    public void close() {
        MapartScreen.renderPipeline().baseImage(this.resizedImage.getSubimage(cropX, cropY, cropWidth * 128, cropHeight * 128));
        this.client.setScreen(parent);
        MapartScreen.renderPipeline().mapHeight(cropHeight);
        MapartScreen.renderPipeline().mapWidth(cropWidth);
        this.parent.redrawImage();
    }
}
