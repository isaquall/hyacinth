package me.isaquall.hyacinth;

import com.google.common.collect.ImmutableMap;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class HyacinthMixinPlugin implements IMixinConfigPlugin {

    private static final Supplier<Boolean> TRUE = () -> true;
    private static final Map<String, Supplier<Boolean>> CONDITIONS = ImmutableMap.of(
            "me.isaquall.hyacinth.mixin.LitematicaSchematicMixin", () -> FabricLoader.getInstance().isModLoaded("litematica")
    );

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return CONDITIONS.getOrDefault(mixinClassName, TRUE).get();
    }

    @Override
    public String getRefMapperConfig() {
        return "";
    }

    @Override
    public List<String> getMixins() {
        return List.of();
    }

    @Override
    public void onLoad(String s) { }

    @Override
    public void acceptTargets(Set<String> set, Set<String> set1) { }

    @Override
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) { }

    @Override
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) { }
}
