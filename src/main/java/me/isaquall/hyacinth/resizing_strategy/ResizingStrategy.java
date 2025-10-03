package me.isaquall.hyacinth.resizing_strategy;

import org.imgscalr.Scalr;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public abstract class ResizingStrategy {

    public static final List<ResizingStrategy> RESIZING_STRATEGIES = new ArrayList<>();

    static {
        for (Scalr.Method method : Scalr.Method.values()) {
            RESIZING_STRATEGIES.add(new ResizingStrategy() {
                @Override
                public BufferedImage resize(BufferedImage input, int width, int height, @Nullable Color padColor) {
                    return Scalr.resize(input, method, Scalr.Mode.FIT_EXACT, width, height);
                }

                @Override
                public String translatableName() {
                    return "hyacinth." + method.name().toLowerCase();
                }

                @Override
                public String translatableTooltip() {
                    return "hyacinth." + method.name().toLowerCase() + "_tooltip";
                }
            });
        }

        RESIZING_STRATEGIES.add(new ResizingStrategy() {
            @Override
            public BufferedImage resize(BufferedImage input, int width, int height, @Nullable Color padColor) {
                BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = result.createGraphics();
                g.setColor(padColor);
                g.fillRect(0, 0, width, height);
                g.drawImage(input, (width - input.getWidth()) / 2, (height - input.getHeight()) / 2, null);
                g.dispose();
                return result;
            }

            @Override
            public String translatableName() {
                return "hyacinth.pad";
            }

            @Override
            public String translatableTooltip() {
                return "hyacinth.pad_tooltip";
            }
        });
    }

    public abstract BufferedImage resize(BufferedImage input, int width, int height, @Nullable Color padColor);

    public abstract String translatableName();

    public abstract String translatableTooltip();
}
