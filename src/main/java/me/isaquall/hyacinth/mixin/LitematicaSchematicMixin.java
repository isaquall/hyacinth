package me.isaquall.hyacinth.mixin;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.io.File;
import java.util.Map;

@Mixin(LitematicaSchematic.class)
public interface LitematicaSchematicMixin {

    @Invoker("<init>")
    static LitematicaSchematic newLitematicaSchematic(File file) {
        throw new RuntimeException();
    }

    @Accessor("blockContainers")
    Map<String, LitematicaBlockStateContainer> getBlockContainers();

    @Accessor("subRegionPositions")
    Map<String, BlockPos> getSubRegionPositions();

    @Accessor("subRegionSizes")
    Map<String, BlockPos> getSubRegionSizes();

    @Accessor("tileEntities")
    Map<String, Map<BlockPos, NbtCompound>> getTileEntities();
}
