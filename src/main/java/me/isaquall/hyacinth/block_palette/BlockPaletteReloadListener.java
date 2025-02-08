package me.isaquall.hyacinth.block_palette;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import io.github.cottonmc.jankson.BlockAndItemSerializers;
import io.github.cottonmc.jankson.JanksonOps;
import me.isaquall.hyacinth.Hyacinth;
import me.isaquall.hyacinth.ui.HyacinthToast;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;

public class BlockPaletteReloadListener implements SimpleSynchronousResourceReloadListener {

    private static final Jankson JANKSON = Hyacinth.JANKSON;
    private static final JanksonOps OPS = JanksonOps.INSTANCE;

    @Override
    public void reload(ResourceManager manager) {
        BlockPalette.BLOCK_PALETTES.clear();

        // Load Hyacinth's default block palette before any others
        manager.findResources("hyacinth/block_palette", id -> id.getPath().endsWith("hyacinth_default.json")).forEach((identifier, resource) -> process(resource));
        manager.findResources("hyacinth/block_palette", id -> !id.getPath().endsWith("hyacinth_default.json")).forEach((identifier, resource) -> process(resource));
    }

    private static void process(Resource resource) {
        try {
            JsonObject file = JANKSON.load(resource.getInputStream());
            for (String key : file.keySet()) {
                Identifier id = Identifier.of(key);
                JsonObject entry = JANKSON.load(file.get(key).toJson());
                LinkedHashSet<BlockState> states = new LinkedHashSet<>();
                OPS.getList(entry.get("blockstates")).getOrThrow(message -> {
                    error(id.toString(), message);
                    throw new RuntimeException("Hyacinth failed to read a block palette. " + message);
                }).accept(element -> {
                    try {
                        BlockState state = BlockAndItemSerializers.getBlockState(JANKSON.load(element.toJson()), JANKSON.getMarshaller());
                        if (state == null) {
                            error(id.toString(), "Failed to read block state from json: " + element.toJson());
                        } else {
                            states.add(state);
                        }
                    } catch (SyntaxError e) {
                        error(id.toString(), e.toString());
                        throw new RuntimeException("Hyacinth failed to read a block palette. " + e);
                    }
                });

                if (!BlockPalette.BLOCK_PALETTES.containsKey(id)) {
                    BlockPalette.BLOCK_PALETTES.put(id, new BlockPalette(id, entry.get(String.class, "translatableName"), entry.getInt("color", 0xFFFFFFFF), states));
                } else {
                    BlockPalette.BLOCK_PALETTES.get(id).states().addAll(states);
                }
            }
        } catch (IOException | SyntaxError e) {
            error(resource.getPackId(), e.toString());
            throw new RuntimeException("Hyacinth failed to read a block palette. " + e);
        }
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of("hyacinth", "block_palette_reload_listener");
    }

    private static void error(String id, String message) {
        MinecraftClient.getInstance().getToastManager().add(new HyacinthToast(List.of(
                Text.translatable("hyacinth.error"),
                Text.translatable("hyacinth.failed_to_load_palette", id),
                Text.of(message))));
    }
}
