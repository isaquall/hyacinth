package me.isaquall.hyacinth;

import blue.endless.jankson.Jankson;
import io.github.cottonmc.jankson.JanksonFactory;
import me.isaquall.hyacinth.block_palette.BlockPaletteReloadListener;
import me.isaquall.hyacinth.dithering.DitheringMatrixReloadListener;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;

public class Hyacinth implements ModInitializer {

    public static final Jankson JANKSON = JanksonFactory.createJankson();

    @Override
    public void onInitialize() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new BlockPaletteReloadListener());
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new DitheringMatrixReloadListener());
    }
}
