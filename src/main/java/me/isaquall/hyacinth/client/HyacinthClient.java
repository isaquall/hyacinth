package me.isaquall.hyacinth.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;

import static net.minecraft.server.command.CommandManager.literal;

public class HyacinthClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("mapart")
                .executes(context -> {
                    MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new MapartScreen()));
                    return 1;
                })));
    }
}
