package me.isaquall.hyacinth.client;

import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import me.isaquall.hyacinth.block_palette.BlockPalette;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class MapartScreen extends BaseUIModelScreen<FlowLayout> {

    private static final Map<Identifier, BlockState> SELECTED_BLOCK = new HashMap<>();


    public MapartScreen() {
        super(FlowLayout.class, DataSource.asset(Identifier.of("hyacinth", "mapart_ui_model")));
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.child(Components.dropdown(Sizing.fixed(20)).child(Components.textBox(Sizing.fixed(20), "hi")).child(Components.textBox(Sizing.fixed(10), "asdjfk")));
        buildBlockPalette(rootComponent.childById(FlowLayout.class, "block_palette"));
    }

    private void buildBlockPalette(FlowLayout blockPalette) {
        for (BlockPalette palette : BlockPalette.BLOCK_PALETTES.values()) {
            SELECTED_BLOCK.put(palette.id(), null);

            FlowLayout color = Containers.horizontalFlow(Sizing.fill(95), Sizing.content(3));
            blockPalette.child(color);

            color.child(Components.box(Sizing.fixed(16), Sizing.fixed(16)).color(Color.ofRgb((palette.color()))).fill(true).tooltip(Text.of(palette.name())).margins(Insets.right(7)));
            Component barrier = Components.item(Items.BARRIER.getDefaultStack()).sizing(Sizing.fixed(16)).tooltip(Text.of("Disable"));
            color.child(barrier);

            barrier.mouseDown().subscribe((x, y, button) -> {
                SELECTED_BLOCK.put(palette.id(), null);
                System.out.println(SELECTED_BLOCK);
                return true;
            });

            for (BlockState blockState : palette.states()) {
                Component block = Components.block(blockState).sizing(Sizing.fixed(16));
                block.tooltip(blockState.getBlock().getName());

                color.child(block);
                block.mouseDown().subscribe((x, y, button) -> {
                    SELECTED_BLOCK.put(palette.id(), blockState);
                    System.out.println(SELECTED_BLOCK);
                    return true;
                });
            }
        }
    }
}
