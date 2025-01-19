package me.isaquall.hyacinth.dithering;

import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record DitheringMatrix(Identifier id, String translatableName, int scaleFactor, List<int[]> matrix) { // TODO javadoc

    public static final Map<Identifier, DitheringMatrix> DITHERING_MATRICES = new HashMap<>();
}
