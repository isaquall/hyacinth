package me.isaquall.hyacinth.block_palette;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.api.SyntaxError;
import io.github.cottonmc.jankson.JanksonOps;
import me.isaquall.hyacinth.Hyacinth;
import me.isaquall.hyacinth.ui.HyacinthToast;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

public class BlockPaletteReloadListener implements SimpleSynchronousResourceReloadListener {

    private static final Jankson JANKSON = Hyacinth.JANKSON;
    private static final JanksonOps OPS = JanksonOps.INSTANCE;

    @Override
    public void reload(ResourceManager manager) {
        BlockPalette.BLOCK_PALETTES.clear();

        // Make sure to load Hyacinth's default block palette before any others
        manager.findResources("hyacinth/block_palette", id -> id.getPath().endsWith("hyacinth_default.json")).forEach((identifier, resource) -> processResource(resource));
        manager.findResources("hyacinth/block_palette", id -> !id.getPath().endsWith("hyacinth_default.json")).forEach((identifier, resource) -> processResource(resource));
        reloadFromConfigFile();
    }

    public static void reloadFromConfigFile() {
        File folder = FabricLoader.getInstance().getConfigDir().resolve("hyacinth/block_palettes").toFile();
        if (!folder.exists()) folder.mkdirs();
        for (File file : folder.listFiles((dir, name) -> name.endsWith(".json"))) {
            try(InputStream stream = Files.newInputStream(file.toPath())) {
                process(file.toString(), stream);
            } catch (IOException e) {
                error(file.toString(), e);
            }
        }
    }

    private static void processResource(Resource resource) {
        try(InputStream stream = resource.getInputStream()) {
            process(resource.getPackId(), stream);
        } catch (IOException e) {
            error(resource.getPackId(), e);
        }
    }

    private static void process(String id, InputStream stream) {
        try {
            OPS.getList(JANKSON.loadElement(stream)).getOrThrow().accept(element -> {
                BlockPalette palette = JANKSON.getMarshaller().marshall(BlockPalette.class, element);
                if (!BlockPalette.BLOCK_PALETTES.keySet().contains(palette.color())) {
                    BlockPalette.BLOCK_PALETTES.put(palette.color(), palette);
                } else {
                    BlockPalette.BLOCK_PALETTES.get(palette.color()).states().addAll(palette.states());
                }
            });
        } catch (SyntaxError | IOException e) {
            error(id, e);
        }
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of("hyacinth", "block_palette_reload_listener");
    }

    public static void error(String id, Exception e) {
        MinecraftClient.getInstance().getToastManager().add(new HyacinthToast(List.of(
                Text.translatable("hyacinth.error"),
                Text.translatable("hyacinth.failed_to_load_palette", id),
                Text.of(e.getMessage()))));
        throw new RuntimeException("Hyacinth failed to read a block palette. " + e);
    }
}
