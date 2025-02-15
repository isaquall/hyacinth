package me.isaquall.hyacinth.mixin;

import io.wispforest.owo.ui.component.DropdownComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DropdownComponent.class)
public interface DropdownComponentAccessor {

    @Accessor("entries")
    FlowLayout getEntry();
}
