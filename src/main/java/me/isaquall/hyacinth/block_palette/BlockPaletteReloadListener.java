package me.isaquall.hyacinth.block_palette;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import io.github.cottonmc.jankson.BlockAndItemSerializers;
import io.github.cottonmc.jankson.JanksonFactory;
import io.github.cottonmc.jankson.JanksonOps;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.BlockState;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.LinkedHashSet;

public class BlockPaletteReloadListener implements SimpleSynchronousResourceReloadListener {

    private static final Jankson JANKSON = JanksonFactory.createJankson();
    private static final JanksonOps OPS = JanksonOps.INSTANCE;

    @Override
    public void reload(ResourceManager manager) {
        BlockPalette.BLOCK_PALETTES.clear();

        // Load Hyacinth's default block palette before any others
        manager.findResources("hyacinth_config", identifier -> identifier.getPath().endsWith("hyacinth_default_block_palette.json")).forEach((identifier, resource) -> process(resource));
        manager.findResources("hyacinth_config", identifier -> !identifier.getPath().endsWith("hyacinth_default_block_palette.json") && identifier.getPath().endsWith("block_palette.json")).forEach((identifier, resource) -> process(resource));
    }

    private static void process(Resource resource) {
        try {
            JsonObject file = JANKSON.load(resource.getInputStream());
            for (String key : file.keySet()) {
                JsonObject entry = JANKSON.load(file.get(key).toJson());
                LinkedHashSet<BlockState> states = new LinkedHashSet<>();
                OPS.getList(entry.get("blockstates")).getOrThrow(message -> {
                    throw new RuntimeException("Hyacinth failed to load blockstates. " + message);
                }).accept((element) -> {
                    try {
                        states.add(BlockAndItemSerializers.getBlockState(JANKSON.load(element.toJson()), JANKSON.getMarshaller()));
                    } catch (SyntaxError e) {
                        throw new RuntimeException(e);
                    }
                });

                Identifier id = Identifier.of(key);
                if (!BlockPalette.BLOCK_PALETTES.containsKey(id)) {
                    BlockPalette.BLOCK_PALETTES.put(id, new BlockPalette(id, entry.get(String.class, "name"), entry.getInt("color", 0xFFFFFFFF), states));
                } else {
                    BlockPalette.BLOCK_PALETTES.get(id).states().addAll(states);
                }
            }
        } catch (IOException | SyntaxError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of("hyacinth", "block_palette_reload_listener");
    }
}
