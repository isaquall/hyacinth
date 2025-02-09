package me.isaquall.hyacinth.noise;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.SpriteAtlasHolder;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;

public class NoiseManager extends SpriteAtlasHolder implements IdentifiableResourceReloadListener {

    public NoiseManager() {
        super(MinecraftClient.getInstance().getTextureManager(), Identifier.of("hyacinth", "textures/atlas/noise.png"), Identifier.of("hyacinth", "noise"));
    }

    public SpriteAtlasTexture getAtlas() {
        return this.atlas;
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of("hyacinth", "noise_manager");
    }
}
