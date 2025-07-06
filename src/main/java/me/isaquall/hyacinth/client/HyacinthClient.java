package me.isaquall.hyacinth.client;

import blue.endless.jankson.Jankson;
import me.isaquall.hyacinth.Hyacinth;
import me.isaquall.hyacinth.block_palette.BlockPalette;
import me.isaquall.hyacinth.noise.NoiseManager;
import me.isaquall.hyacinth.ui.MapartScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

import static net.minecraft.server.command.CommandManager.literal;

public class HyacinthClient implements ClientModInitializer {

    private final KeyBinding mapartScreenBinding = new KeyBinding("hyacinth.mapart_screen_keybind", GLFW.GLFW_KEY_Z, "Hyacinth");
    public static NoiseManager NOISE_MANAGER;
    private static final Jankson JANKSON = Hyacinth.JANKSON;

    @Override
    public void onInitializeClient() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("hyacinth")
                .executes(context -> {
                    MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new MapartScreen()));
                    return 1;
                })
                .then(literal("addBlock")
                        .requires(ServerCommandSource::isExecutedByPlayer)
                        .executes(context -> {
                            try {
                                HitResult hit = MinecraftClient.getInstance().getCameraEntity().raycast(20.0, 0.0F, false);
                                BlockPos blockPos = ((BlockHitResult) hit).getBlockPos();
                                BlockState blockState = MinecraftClient.getInstance().world.getBlockState(blockPos);
                                MapColor color = blockState.getMapColor(MinecraftClient.getInstance().world, blockPos);
                                BlockPalette match = null;
                                for (Map.Entry<Integer, BlockPalette> entry : BlockPalette.BLOCK_PALETTES.entrySet()) {
                                    if (entry.getValue().color() == color.color) {
                                        match = entry.getValue();
                                    }
                                }

                                if (match == null) {
                                    context.getSource().sendError(Text.translatable("hyacinth.failed_to_find_matching_palette"));
                                    return -1;
                                }

                                match.states().add(blockState);
//                                BlockPaletteReloadListener.reloadFromConfigFile();
                            } catch (NullPointerException e) {
                                context.getSource().sendError(Text.translatable("hyacinth.failed_to_add_block"));
                                context.getSource().sendError(Text.literal(e.toString()));
                                throw new RuntimeException(e);
                            }
                            return 1;
                        }))));

        KeyBindingHelper.registerKeyBinding(mapartScreenBinding);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (mapartScreenBinding.wasPressed()) {
                MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new MapartScreen()));
            }
        });
    }
}
