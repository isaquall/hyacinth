package me.isaquall.hyacinth.client;

import me.isaquall.hyacinth.ui.MapartScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

import static net.minecraft.server.command.CommandManager.literal;

public class HyacinthClient implements ClientModInitializer {

    private final KeyBinding mapartScreenBinding = new KeyBinding("hyacinth.mapart_screen_keybind", GLFW.GLFW_KEY_Z, "Hyacinth");

    @Override
    public void onInitializeClient() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("hyacinth")
                .executes(context -> {
                    MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new MapartScreen()));
                    return 1;
                })));

        KeyBindingHelper.registerKeyBinding(mapartScreenBinding);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (mapartScreenBinding.wasPressed()) {
                MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new MapartScreen()));
            }
        });
    }
}
