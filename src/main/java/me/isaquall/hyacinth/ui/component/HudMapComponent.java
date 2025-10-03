package me.isaquall.hyacinth.ui.component;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Identifier;

public class HudMapComponent extends BaseComponent {

    private final MapRenderState renderState = new MapRenderState();
    private final MapIdComponent id;
    private final MapState state;
    // Only used so we can access FilledMapItem.update(), which doesn't use any internal state
    private static final FilledMapItem DUMMY = (FilledMapItem) Items.FILLED_MAP;
    private static final Identifier MAP_TEXTURE = Identifier.of("textures/map/map_background_checkerboard.png");

    public HudMapComponent(MapIdComponent id, MapState state) {
        this.id = id;
        this.state = state;
        DUMMY.updateColors(MinecraftClient.getInstance().world, MinecraftClient.getInstance().player, state);
        PlayerEntity player = MinecraftClient.getInstance().player;
        MapState.PlayerUpdateTracker playerUpdateTracker = state.new PlayerUpdateTracker(player);
        state.updateTrackersByPlayer.put(MinecraftClient.getInstance().player, playerUpdateTracker);
        state.updateTrackers.add(playerUpdateTracker);
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        MatrixStack stack = context.getMatrixStack();
        stack.push();
        stack.translate((float) this.x(), (float) this.y(), 1.0F);
        context.drawTexture(RenderLayer::getGuiTextured, MAP_TEXTURE, 0, 0, 0, 0, 138, 138, 138, 138);
        stack.translate(5, 5, 2);
        MapRenderer mapRenderer = MinecraftClient.getInstance().getMapRenderer();
        DUMMY.updateColors(MinecraftClient.getInstance().world, MinecraftClient.getInstance().player, state);
        mapRenderer.update(id, state, renderState);
        mapRenderer.draw(renderState, stack, context.vertexConsumers(), true, 15728880);
        stack.pop();
    }
}
