package me.isaquall.hyacinth.ui.component;

import io.wispforest.owo.ui.component.DropdownComponent;
import io.wispforest.owo.ui.core.ParentComponent;
import io.wispforest.owo.ui.core.Sizing;

public class ExtendedDropdownComponent extends DropdownComponent {

    private final int x;
    private final int y;

    protected ExtendedDropdownComponent(Sizing horizontalSizing, int x, int y) {
        super(horizontalSizing);
        this.x = x;
        this.y = y;
    }

    @Override
    public void mount(ParentComponent parent, int ignoredX, int ignoredY) {
        this.parent = parent;
        this.mounted = true;
        this.moveTo(this.x, this.y);
    }
}
