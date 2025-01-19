package me.isaquall.hyacinth.dithering;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import io.github.cottonmc.jankson.JanksonOps;
import me.isaquall.hyacinth.Hyacinth;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
// TODO eventually make it so you can declare multiple matrices in one file like the block palettes
public class DitheringMatrixReloadListener implements SimpleSynchronousResourceReloadListener {

    private static final Jankson JANKSON = Hyacinth.JANKSON;
    private static final JanksonOps OPS = JanksonOps.INSTANCE;

    @Override
    public void reload(ResourceManager manager) {
        DitheringMatrix.DITHERING_MATRICES.clear();

        manager.findResources("hyacinth/dithering_matrix", id -> id.getPath().endsWith(".json")).forEach((id, resource) -> process(resource));
    }

    private static void process(Resource resource) {
        List<int[]> matrix = new ArrayList<>();
        try {
            JsonObject file = JANKSON.load(resource.getInputStream());
            OPS.getList(file.get("matrix")).getOrThrow(message -> {
                throw new RuntimeException("Hyacinth failed to read a dithering matrix. " + message); // TODO better logging info?
            }).accept(element -> {
                try {
                    JsonObject entry = JANKSON.load(element.toJson());
                    matrix.add(new int[]{entry.getInt("x", 0), entry.getInt("y", 0), entry.getInt("value", 0)});
                } catch (SyntaxError e) {
                    throw new RuntimeException(e);
                }
            });

            Identifier id = Identifier.of(file.get(String.class, "id"));
            DitheringMatrix.DITHERING_MATRICES.put(id, new DitheringMatrix(id, file.get(String.class, "translatableName"), file.getInt("scale_factor", 1), matrix));
        } catch (IOException | SyntaxError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of("hyacinth", "dithering_matrix_reload_listener");
    }
}
