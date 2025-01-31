package me.isaquall.hyacinth.dithering;

import me.isaquall.hyacinth.dithering.algorithm.DitheringAlgorithm;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public record DitheringStrategy(Identifier id, DitheringAlgorithm ditheringAlgorithm, String translatableName) {

    public static final Map<Identifier, DitheringStrategy> DITHERING_STRATEGIES = new HashMap<>();
}
