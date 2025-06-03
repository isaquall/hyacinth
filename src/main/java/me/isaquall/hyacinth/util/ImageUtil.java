package me.isaquall.hyacinth.util;

import net.minecraft.client.texture.NativeImage;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageUtil {

    public static BufferedImage nativeToBufferedImage(NativeImage input) {
        BufferedImage image = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, input.getWidth(), input.getHeight(), input.copyPixelsArgb(), 0, input.getWidth());
        return image;
    }
}
