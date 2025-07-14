package me.isaquall.hyacinth.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageUtils {

    public static BufferedImage nativeToBufferedImage(NativeImage input) {
        BufferedImage image = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, input.getWidth(), input.getHeight(), input.copyPixelsArgb(), 0, input.getWidth());
        return image;
    }

    public static void bufferedToNativeImage(BufferedImage inputImage, Identifier id) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            // Convert BufferedImage to NativeImage
            ImageIO.write(inputImage, "png", stream);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(stream.toByteArray());
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(NativeImage.read(inputStream)));
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static BufferedImage cloneBufferedImage(Image inputImage) {
        BufferedImage clone = new BufferedImage(inputImage.getWidth(null), inputImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = clone.getGraphics();
        graphics.drawImage(inputImage, 0, 0, null);
        graphics.dispose();
        return clone;
    }
}
