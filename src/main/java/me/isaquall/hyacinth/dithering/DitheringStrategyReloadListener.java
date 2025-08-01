package me.isaquall.hyacinth.dithering;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import me.isaquall.hyacinth.Hyacinth;
import me.isaquall.hyacinth.dithering.algorithm.DitheringAlgorithm;
import me.isaquall.hyacinth.ui.component.HyacinthToast;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.List;

public class DitheringStrategyReloadListener implements SimpleSynchronousResourceReloadListener {

    private static final Jankson JANKSON = Hyacinth.JANKSON;

    @Override
    public void reload(ResourceManager manager) {
        DitheringStrategy.DITHERING_STRATEGIES.clear();

        manager.findResources("hyacinth/dithering_strategy", id -> id.getPath().endsWith(".json")).forEach((id, resource) -> {
            try (InputStream stream = resource.getInputStream()) {
                process(resource.getPackId(), stream);
            } catch (IOException e) {
                error(resource.getPackId(), e);
            }
        });

        File folder = FabricLoader.getInstance().getConfigDir().resolve("hyacinth/dithering_strategy").toFile();
        if (!folder.exists()) folder.mkdirs();
        for (File file : folder.listFiles((dir, name) -> name.endsWith(".json"))) {
            try(InputStream stream = Files.newInputStream(file.toPath())) {
                process(file.getPath(), stream);
            } catch (IOException e) {
                error(file.getPath(), e);
            }
        }
    }

    private static void process(String source, InputStream stream) {
        try {
            JsonObject file = JANKSON.load(stream);

            for (String key : file.keySet()) {
                Identifier id = Identifier.of(key);
                JsonObject entry = JANKSON.load(file.get(key).toJson());
                JsonObject algorithmEntry = JANKSON.load(entry.get("algorithm").toJson());
                Class<? extends DitheringAlgorithm> algorithmClass = DitheringAlgorithm.DITHERING_ALGORITHMS.get(Identifier.of(algorithmEntry.get(String.class, "id")));
                DitheringAlgorithm algorithm = algorithmClass.getDeclaredConstructor(Identifier.class, JsonObject.class).newInstance(id, algorithmEntry);
                DitheringStrategy.DITHERING_STRATEGIES.put(id, new DitheringStrategy(id, algorithm, entry.get(String.class, "translatableName")));
            }
        } catch (IOException | SyntaxError | NullPointerException | NoSuchMethodException | IllegalAccessException |
                 InstantiationException | InvocationTargetException e) {
            error(source, e);
        }
    }

    private static void error(String source, Exception e) {
        MinecraftClient.getInstance().getToastManager().add(new HyacinthToast(List.of(
                Text.translatable("hyacinth.error"),
                Text.translatable("hyacinth.failed_to_read_dithering_strategy", source),
                Text.of(e.toString()))));
        throw new RuntimeException(e);
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of("hyacinth", "dithering_strategy_reload_listener");
    }
}
