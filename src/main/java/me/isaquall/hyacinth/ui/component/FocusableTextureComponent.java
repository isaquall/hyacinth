package me.isaquall.hyacinth.ui.component;

import io.wispforest.owo.ui.component.TextureComponent;
import net.minecraft.util.Identifier;

public class FocusableTextureComponent extends TextureComponent {

    public FocusableTextureComponent (Identifier texture, int u, int v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        super(texture, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
    }

    @Override
    public boolean canFocus(FocusSource source) {
        return source == FocusSource.MOUSE_CLICK;
    }
}
