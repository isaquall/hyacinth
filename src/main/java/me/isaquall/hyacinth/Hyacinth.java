package me.isaquall.hyacinth;

import me.isaquall.hyacinth.block_palette.BlockPaletteReloadListener;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;

public class Hyacinth implements ModInitializer {

    @Override
    public void onInitialize() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new BlockPaletteReloadListener());
    }
}
