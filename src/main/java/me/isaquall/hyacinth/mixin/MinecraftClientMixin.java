package me.isaquall.hyacinth.mixin;

import me.isaquall.hyacinth.block_palette.BlockPaletteReloadListener;
import me.isaquall.hyacinth.client.HyacinthClient;
import me.isaquall.hyacinth.noise.NoiseManager;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.resource.ResourceType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/MapRenderer;<init>(Lnet/minecraft/client/texture/MapDecorationsAtlasManager;Lnet/minecraft/client/texture/MapTextureManager;)V"
            )
    )
    private void hyacinth$createNoiseManager(RunArgs args, CallbackInfo ci) {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(HyacinthClient.NOISE_MANAGER = new NoiseManager());
    }
}
