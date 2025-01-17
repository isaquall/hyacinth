package me.isaquall.hyacinth.resizing_strategy;

import net.minecraft.util.Identifier;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class HyacinthResizingStrategies {

    public static final Map<Identifier, ResizingStrategy> RESIZING_STRATEGIES = new HashMap<>();

    static {
        RESIZING_STRATEGIES.put(Identifier.of("hyacinth", "resizing_strategy/scale_default"), new ResizingStrategy() {
            @Override
            public BufferedImage resize(BufferedImage in, int x, int y) {
                BufferedImage out = new BufferedImage(x, y, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = out.createGraphics();
                graphics.drawImage(in.getScaledInstance(x, y, Image.SCALE_DEFAULT), 0, 0, null);
                graphics.dispose();
                return out;
            }

            @Override
            public String name() {
                return "hyacinth.scale_default";
            }
        });

        RESIZING_STRATEGIES.put(Identifier.of("hyacinth", "resizing_strategy/scale_smooth"), new ResizingStrategy() {
            @Override
            public BufferedImage resize(BufferedImage in, int x, int y) {
                BufferedImage out = new BufferedImage(x, y, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = out.createGraphics();
                graphics.drawImage(in.getScaledInstance(x, y, Image.SCALE_SMOOTH), 0, 0, null);
                graphics.dispose();
                return out;
            }

            @Override
            public String name() {
                return "hyacinth.scale_smooth";
            }
        });
    }
}
