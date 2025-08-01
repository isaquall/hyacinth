package me.isaquall.hyacinth.ui.component;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.DropdownComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.OverlayContainer;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import me.isaquall.hyacinth.mixin.DropdownComponentAccessor;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public class ButtonDropdownComponent<T> extends FlowLayout {

    private static final Surface TRANSPARENT = (context, component) -> {};

    public ButtonDropdownComponent(FlowLayout flow, String labelTranslatable, T[] options, Function<T, String> translatableName, @Nullable Function<T, String> translatableTooltip, Consumer<T> writeFunction, T current) {
        super(Sizing.content(), Sizing.content(), Algorithm.HORIZONTAL);

        this.child(Components.label(Text.translatable(labelTranslatable)).margins(Insets.top(4).withRight(1)));
        this.child(Components.button(Text.translatable(translatableName.apply(current)), button -> {
            if (!button.active) return;
            button.active(false);

            DropdownComponent dropdown = new ExtendedDropdownComponent(Sizing.content(5), button.x(), button.y() + 20);
            OverlayContainer<DropdownComponent> overlay = Containers.overlay(dropdown);
            overlay.surface(TRANSPARENT);
            flow.child(overlay);

            overlay.mouseDown().subscribe((a, b, c) -> {
                if (!dropdown.isInBoundingBox(a, b)) {
                    button.active(true);
                    flow.removeChild(overlay);
                }
                return true;
            });

            for (T option : options) {
                MutableText name = Text.translatable(translatableName.apply(option));
                dropdown.button(name, dropdownComponent -> {
                    flow.removeChild(overlay);
                    writeFunction.accept(option);
                    button.setMessage(name);
                    button.active(true);
                });

                // Dumb hack because we can't change the tooltip from the DropdownComponent builder
                if (translatableTooltip != null) {
                    for (Component child : ((DropdownComponentAccessor) dropdown).getEntry().children()) {
                        if (child instanceof LabelComponent label) {
                            if (label.text() == name) {
                                label.tooltip(Text.translatable(translatableTooltip.apply(option)));
                            }
                        }
                    }
                }
            }
            dropdown.zIndex(1);
        }));
    }
}

