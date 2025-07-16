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
import java.util.function.Function;
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

        FlowLayout cropSettings = rootComponent.childById(FlowLayout.class, "crop-settings");
        cropSettings.child(buildNumberDisplay("hyacinth.crop_x", textBoxComponent -> cropXDisplay = textBoxComponent, () -> cropX, this::clampCropX));
        cropSettings.child(buildNumberDisplay("hyacinth.crop_y", textBoxComponent -> cropYDisplay = textBoxComponent, () -> cropY, this::clampCropY));

        rootComponent.childById(ButtonComponent.class, "continue-button").onPress(button -> this.close());

        rootComponent.childById(TextBoxComponent.class, "crop-width").text(String.valueOf(cropWidth)).onChanged().subscribe(width -> {
            if (width.isEmpty()) return;
            int value;
            try {
               value = Integer.parseInt(width);
            } catch (NumberFormatException e) {
                return;
            }
            if (value <= 0 || value * 128 > imageWidth) return;
            this.cropWidth = value;
            clampCropX(cropX);
        });

        rootComponent.childById(TextBoxComponent.class, "crop-height").text(String.valueOf(cropHeight)).onChanged().subscribe(height -> {
            if (height.isEmpty()) return;
            int value;
            try {
                value = Integer.parseInt(height);
            } catch (NumberFormatException e) {
                return;
            }
            if (value <= 0 || value * 128 > imageHeight) return;
            this.cropHeight = value;
            clampCropY(cropY);
        });

        FlowLayout resizeOriginalImageContainer = rootComponent.childById(FlowLayout.class, "resize-original-image");

        resizeOriginalImageContainer.child(buildNumberDisplay("hyacinth.image_width", textBoxComponent -> {}, () -> imageWidth, width -> {
            if (width <= 0) return 1;
            setImageSize(width, imageHeight);
            return width;
        }));

        resizeOriginalImageContainer.child(buildNumberDisplay("hyacinth.image_height", textBoxComponent -> {}, () -> imageHeight, height -> {
            if (height <= 0) return 1;
            setImageSize(imageWidth, height);
            return height;
        }));

        buildMapPreview(this.originalImage);
    }

    // Function<Integer, Integer> takes in the inputted value and returns the clamped value.
    private FlowLayout buildNumberDisplay(String translatableLabel, Consumer<TextBoxComponent> textBoxComponentConsumer, Supplier<Integer> get, Function<Integer, Integer> set) {
        FlowLayout numberDisplayComponent = this.model.expandTemplate(FlowLayout.class,
                "position@hyacinth:crop_ui_model",
                Map.of("axis", translatableLabel,
                        "value", String.valueOf(get.get())));
        TextBoxComponent textBox = numberDisplayComponent.childById(TextBoxComponent.class, "position-display");
        textBox.onChanged().subscribe(value -> {
            if (value.isEmpty()) return;
            int intValue = Integer.parseInt(value);
            if (intValue < 0) return;
            set.apply(intValue);
        });
        textBoxComponentConsumer.accept(textBox);
        textBox.mouseScroll().subscribe((mouseX, mouseY, amount) -> {
            textBox.setText(String.valueOf(set.apply(get.get() + (int) amount)));
            return true;
        });
        numberDisplayComponent.childById(ButtonComponent.class, "decrement").onPress(button -> textBox.setText(String.valueOf(set.apply(get.get() - 1))));
        numberDisplayComponent.childById(ButtonComponent.class, "increment").onPress(button -> textBox.setText(String.valueOf(set.apply(get.get() + 1))));
        return numberDisplayComponent;
    }

    private void buildMapPreview(BufferedImage image) {
        FlowLayout mapContainer = rootComponent.childById(FlowLayout.class, "map-preview-container");
        mapContainer.removeChild(mapContainer.childById(FocusableTextureComponent.class, "crop-preview"));

        Identifier id = Identifier.of("hyacinth", "crop_preview");
        ImageUtils.bufferedToNativeImage(image, id);
        Component texture = new FocusableTextureComponent(id, 0, 0, image.getWidth(), image.getHeight(), image.getWidth(), image.getHeight()).id("crop-preview").zIndex(-1);
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

    private int clampCropX(int x) {
        return cropX = Math.clamp(x, 0, resizedImage.getWidth() - (128 * cropWidth));
    }

    private int clampCropY(int y) {
        return cropY = Math.clamp(y, 0, resizedImage.getHeight() - (128 * cropHeight));
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
