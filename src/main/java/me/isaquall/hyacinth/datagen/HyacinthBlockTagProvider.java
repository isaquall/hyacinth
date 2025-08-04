package me.isaquall.hyacinth.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.block.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class HyacinthBlockTagProvider extends FabricTagProvider.BlockTagProvider {

    public static final TagKey<Block> REQUIRES_SUPPORT = TagKey.of(RegistryKeys.BLOCK, Identifier.of("hyacinth", "requires_support"));

    public HyacinthBlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        FabricTagProvider<Block>.FabricTagBuilder builder = getOrCreateTagBuilder(REQUIRES_SUPPORT)
                .add(Blocks.SNOW)
                .add(Blocks.GLOW_LICHEN);

        for (Block block : Registries.BLOCK) {
            if (block instanceof FallingBlock fallingBlock) {
                builder.add(fallingBlock);
            } else if (block instanceof CarpetBlock carpetBlock) {
                builder.add(carpetBlock);
            } else if (block instanceof AbstractPressurePlateBlock pressurePlateBlock) {
                builder.add(pressurePlateBlock);
            }
        }
    }
}
