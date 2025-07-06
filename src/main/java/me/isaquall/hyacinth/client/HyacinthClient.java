package me.isaquall.hyacinth.client;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.api.DeserializationException;
import blue.endless.jankson.api.SyntaxError;
import io.github.cottonmc.jankson.JanksonOps;
import me.isaquall.hyacinth.Hyacinth;
import me.isaquall.hyacinth.block_palette.BlockPalette;
import me.isaquall.hyacinth.block_palette.BlockPaletteReloadListener;
import me.isaquall.hyacinth.noise.NoiseManager;
import me.isaquall.hyacinth.ui.MapartScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.io.FileUtils;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.minecraft.server.command.CommandManager.literal;

public class HyacinthClient implements ClientModInitializer {

    private final KeyBinding mapartScreenBinding = new KeyBinding("hyacinth.mapart_screen_keybind", GLFW.GLFW_KEY_Z, "Hyacinth");
    public static NoiseManager NOISE_MANAGER;
    private static final Jankson JANKSON = Hyacinth.JANKSON;
    private static final JanksonOps OPS = JanksonOps.INSTANCE;

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
                                int color = blockState.getMapColor(MinecraftClient.getInstance().world, blockPos).color;

                                if (BlockPalette.BLOCK_PALETTES.get(color).states().contains(blockState)) {
                                    context.getSource().sendError(Text.translatable("hyacinth.block_already_exists_in_palette", Text.translatable(BlockPalette.BLOCK_PALETTES.get(color).translatableName())));
                                    return -1;
                                }

                                File manualAdditionsFile = FabricLoader.getInstance().getConfigDir().resolve("hyacinth/block_palettes/manual_additions.json").toFile();
                                if (!manualAdditionsFile.exists()) {
                                    manualAdditionsFile.createNewFile();
                                    FileUtils.writeStringToFile(manualAdditionsFile, JANKSON.getMarshaller().serialize(List.of(new BlockPalette(null, color, new LinkedHashSet<>(List.of(blockState))))).toJson(), "utf-8");
                                    BlockPaletteReloadListener.reloadFromConfigFile();
                                    context.getSource().sendMessage(Text.translatable("hyacinth.successfully_added_new_block"));
                                    return 1;
                                } else {
                                    JsonElement element = JANKSON.loadElement(manualAdditionsFile);
                                    if (element instanceof JsonArray array) {
                                        AtomicBoolean exists = new AtomicBoolean(false);

                                        OPS.getList(array).getOrThrow().accept(paletteElement -> {
                                            try {
                                                BlockPalette palette = JANKSON.getMarshaller().marshallCarefully(BlockPalette.class, paletteElement);
                                                if (palette.color() == color) {
                                                    exists.set(true);
                                                    palette.states().add(blockState);
                                                    array.remove(paletteElement);
                                                    array.add(JANKSON.getMarshaller().serialize(palette));
                                                    FileUtils.writeStringToFile(manualAdditionsFile, array.toJson(), "utf-8");
                                                    BlockPaletteReloadListener.reloadFromConfigFile();
                                                    context.getSource().sendMessage(Text.translatable("hyacinth.successfully_added_new_block"));
                                                }
                                            } catch (DeserializationException | IOException e) {
                                                context.getSource().sendError(Text.translatable("hyacinth.failed_to_add_block"));
                                                throw new RuntimeException(e);
                                            }
                                        });

                                        if (!exists.get()) {
                                            BlockPalette newPalette = new BlockPalette(null, color, new LinkedHashSet<>(List.of(blockState)));
                                            JsonElement newPaletteElement = JANKSON.getMarshaller().serialize(newPalette);
                                            array.add(newPaletteElement);
                                            FileUtils.writeStringToFile(manualAdditionsFile, array.toJson(), "utf-8");
                                            BlockPaletteReloadListener.reloadFromConfigFile();
                                            context.getSource().sendMessage(Text.translatable("hyacinth.successfully_added_new_block"));
                                            return 1;
                                        }
                                    }
                                }
                                return 1;
                            } catch (NullPointerException | IOException | SyntaxError e) {
                                context.getSource().sendError(Text.translatable("hyacinth.failed_to_add_block"));
                                context.getSource().sendError(Text.literal(e.toString()));
                                throw new RuntimeException(e);
                            }
                        }))));

        KeyBindingHelper.registerKeyBinding(mapartScreenBinding);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (mapartScreenBinding.wasPressed()) {
                MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new MapartScreen()));
            }
        });
    }
}
