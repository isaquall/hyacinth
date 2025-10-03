package me.isaquall.hyacinth.client;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.api.DeserializationException;
import blue.endless.jankson.api.SyntaxError;
import com.mojang.brigadier.context.CommandContext;
import io.github.cottonmc.jankson.JanksonOps;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.hud.Hud;
import me.isaquall.hyacinth.Hyacinth;
import me.isaquall.hyacinth.block_palette.BlockPalette;
import me.isaquall.hyacinth.block_palette.BlockPaletteReloadListener;
import me.isaquall.hyacinth.noise.NoiseManager;
import me.isaquall.hyacinth.ui.MapartScreen;
import me.isaquall.hyacinth.ui.component.HudMapComponent;
import me.isaquall.hyacinth.ui.component.HyacinthToast;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.map.MapState;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.PersistentState;
import org.apache.commons.io.FileUtils;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class HyacinthClient implements ClientModInitializer {

    private static final KeyBinding MAPART_SCREEN_BINDING = new KeyBinding("hyacinth.mapart_screen_keybind", GLFW.GLFW_KEY_Z, "Hyacinth");
    public static NoiseManager NOISE_MANAGER;
    private static final Jankson JANKSON = Hyacinth.JANKSON;
    private static final JanksonOps OPS = JanksonOps.INSTANCE;
    private static final Identifier HUD_MAP_RENDER = Identifier.of("hyacinth", "hud_map_render");

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("hyacinth")
                    .executes(context -> {
                        MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new MapartScreen()));
                        return 0;
                    })
                    .then(literal("addBlock").executes(HyacinthClient::executeAddBlock)));
            //                .then(literal("hud").executes(HyacinthClient::executeHud)) TODO
        });

        KeyBindingHelper.registerKeyBinding(MAPART_SCREEN_BINDING);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (MAPART_SCREEN_BINDING.wasPressed()) {
                MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new MapartScreen()));
            }
        });
    }

    private static int executeHud(CommandContext<ServerCommandSource> context) {
        if (Hud.hasComponent(HUD_MAP_RENDER)) {
            Hud.remove(HUD_MAP_RENDER);
        } else {
            var player = context.getSource().getPlayer();

            ServerWorld world = player.getServerWorld();

            int i = 128;
            int j = MathHelper.floor((player.getBlockX() + 64.0) / (double) i);
            int k = MathHelper.floor((player.getBlockZ() + 64.0) / (double) i);
            int centerX = j * i + i / 2 - 64;
            int centerZ = k * i + i / 2 - 64;

            MapIdComponent id = null;
            for (Map.Entry<String, Optional<PersistentState>> entry : world.getServer().getOverworld().getPersistentStateManager().loadedStates.entrySet()) {
                if (entry.getKey().startsWith("map_")) {
                    if (entry.getValue().isEmpty()) continue;
                    MapState possible = world.getServer().getOverworld().getPersistentStateManager().get(MapState.getPersistentStateType(), entry.getKey());
                    if (possible == null) continue;
                    if (possible.centerX == centerX && possible.centerZ == centerZ) {
                        id = new MapIdComponent(Integer.parseInt(entry.getKey().replace("map_", "")));
                        break;
                    }
                }
            }

            if (id == null) {
                id = FilledMapItem.allocateMapId(player.getServerWorld(), player.getBlockX(), player.getBlockZ(), 0, false, true, player.getServerWorld().getRegistryKey());
            }

            final MapIdComponent finalId = id;
            Hud.add(HUD_MAP_RENDER, () -> Containers.draggable(Sizing.content(), Sizing.content(), new HudMapComponent(finalId, player.getServerWorld().getMapState(finalId)).sizing(Sizing.fixed(138))).positioning(Positioning.relative(0, 0)));
        }
        return 0;
    }

    private static int executeAddBlock(CommandContext<FabricClientCommandSource> context) {
        try {
            HitResult hit = MinecraftClient.getInstance().getCameraEntity().raycast(20.0, 0.0F, false);
            BlockPos blockPos = ((BlockHitResult) hit).getBlockPos();
            BlockState blockState = MinecraftClient.getInstance().world.getBlockState(blockPos);
            int color = blockState.getMapColor(MinecraftClient.getInstance().world, blockPos).color;

            if (BlockPalette.BLOCK_PALETTES.get(color).states().contains(blockState)) {
                HyacinthToast.error(Text.translatable("hyacinth.block_already_exists_in_palette", Text.translatable(BlockPalette.BLOCK_PALETTES.get(color).translatableName())));
                return -1;
            }

            File manualAdditionsFile = FabricLoader.getInstance().getConfigDir().resolve("hyacinth/block_palettes/manual_additions.json").toFile();
            if (!manualAdditionsFile.exists()) {
                manualAdditionsFile.createNewFile();
                FileUtils.writeStringToFile(manualAdditionsFile, JANKSON.getMarshaller().serialize(List.of(new BlockPalette(null, color, new LinkedHashSet<>(List.of(blockState))))).toJson(), "utf-8");
                BlockPaletteReloadListener.reloadFromConfigFile();
                HyacinthToast.info(Text.translatable("hyacinth.successfully_added_new_block", Text.translatable(blockState.getBlock().getTranslationKey())));
                return 0;
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
                                HyacinthToast.info(Text.translatable("hyacinth.successfully_added_new_block", Text.translatable(blockState.getBlock().getTranslationKey())));
                            }
                        } catch (DeserializationException | IOException e) {
                            HyacinthToast.error(Text.translatable("hyacinth.failed_to_add_block"));
                            throw new RuntimeException("Hyacinth failed to add a block to the block palette from addBlock command. " + e);
                        }
                    });

                    if (!exists.get()) {
                        BlockPalette newPalette = new BlockPalette(null, color, new LinkedHashSet<>(List.of(blockState)));
                        JsonElement newPaletteElement = JANKSON.getMarshaller().serialize(newPalette);
                        array.add(newPaletteElement);
                        FileUtils.writeStringToFile(manualAdditionsFile, array.toJson(), "utf-8");
                        BlockPaletteReloadListener.reloadFromConfigFile();
                        HyacinthToast.info(Text.translatable("hyacinth.successfully_added_new_block", Text.translatable(blockState.getBlock().getTranslationKey())));
                        return 0;
                    }
                }
            }
            return 0;
        } catch (NullPointerException | IOException | SyntaxError e) {
            HyacinthToast.error(Text.translatable("hyacinth.failed_to_add_block"));
            throw new RuntimeException("Hyacinth failed to add a block to the block palette from addBlock command. " + e);
        }
    }
}
