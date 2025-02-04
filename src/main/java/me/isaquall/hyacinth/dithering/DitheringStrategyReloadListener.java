package me.isaquall.hyacinth.dithering;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import me.isaquall.hyacinth.Hyacinth;
import me.isaquall.hyacinth.dithering.algorithm.DitheringAlgorithm;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class DitheringStrategyReloadListener implements SimpleSynchronousResourceReloadListener {

    private static final Jankson JANKSON = Hyacinth.JANKSON;

    @Override
    public void reload(ResourceManager manager) {
        DitheringStrategy.DITHERING_STRATEGIES.clear();

        manager.findResources("hyacinth/dithering_strategy", id -> id.getPath().endsWith(".json")).forEach((id, resource) -> process(resource));
    }

    private static void process(Resource resource) {
        try {
            JsonObject file = JANKSON.load(resource.getInputStream());

            for (String key : file.keySet()) {
                Identifier id = Identifier.of(key);
                JsonObject entry = JANKSON.load(file.get(key).toJson());
                JsonObject algorithmEntry = JANKSON.load(entry.get("algorithm").toJson());
                Class<? extends DitheringAlgorithm> algorithmClass = DitheringAlgorithm.DITHERING_ALGORITHMS.get(Identifier.of(algorithmEntry.get(String.class, "id")));
                DitheringAlgorithm algorithm = algorithmClass.getDeclaredConstructor(JsonObject.class).newInstance(algorithmEntry);
                DitheringStrategy.DITHERING_STRATEGIES.put(id, new DitheringStrategy(id, algorithm, entry.get(String.class, "translatableName")));
            }
        } catch (IOException | SyntaxError | NullPointerException | NoSuchMethodException | IllegalAccessException |
                 InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of("hyacinth", "dithering_strategy_reload_listener");
    }
}
