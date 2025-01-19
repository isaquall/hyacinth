package me.isaquall.hyacinth.resizing_strategy;

import net.minecraft.util.Identifier;

import java.awt.image.BufferedImage;

public interface ResizingStrategy {

    /**
     * Takes in an {@link BufferedImage} and returns a new {@link BufferedImage} of the new size.
     *
     * @param in the {@link BufferedImage} to resize
     * @return the resized {@link BufferedImage}
     */
    BufferedImage resize(BufferedImage in, int x, int y);

    /**
     *  Returns the translation key for translatableName of this resizing strategy.
     *
     * @return the {@link Identifier} of the translation key
     */
    String name();
}
