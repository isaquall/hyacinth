package me.isaquall.hyacinth.resizing_strategy;

import net.minecraft.util.Identifier;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public interface ResizingStrategy {

    Map<Identifier, ResizingStrategy> RESIZING_STRATEGIES = new HashMap<>(Map.of(
        Identifier.of("hyacinth", "resizing_strategy/scale_default"), new ResizingStrategy() {
            @Override
            public BufferedImage resize(BufferedImage in, int x, int y) {
                return simpleResize(in, x, y, Image.SCALE_DEFAULT);
            }

            @Override
            public String translatableName() {
                return "hyacinth.scale_default";
            }
        },
        Identifier.of("hyacinth", "resizing_strategy/scale_smooth"), new ResizingStrategy() {
            @Override
            public BufferedImage resize(BufferedImage in, int x, int y) {
                return simpleResize(in, x, y, Image.SCALE_SMOOTH);
            }

            @Override
            public String translatableName() {
                return "hyacinth.scale_smooth";
            }
        }
    ));

    static BufferedImage simpleResize(BufferedImage in, int x, int y, int mode) {
        BufferedImage out = new BufferedImage(x, y, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = out.createGraphics();
        graphics.drawImage(in.getScaledInstance(x, y, mode), 0, 0, null);
        graphics.dispose();
        return out;
    }

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
    String translatableName();
}
