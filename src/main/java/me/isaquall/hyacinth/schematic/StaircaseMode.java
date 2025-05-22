package me.isaquall.hyacinth.schematic;

public enum StaircaseMode {
    FLAT("hyacinth.staircase_mode.flat", "hyacinth.staircase_mode.flat_tooltip"),
    VALLEY("hyacinth.staircase_mode.valley", "hyacinth.staircase_mode.valley_tooltip"),
    CLASSIC("hyacinth.staircase_mode.classic", "hyacinth.staircase_mode.classic_tooltip");

    private final String translatableName;
    private final String translatableTooltip;

    StaircaseMode(String translatableName, String translatableTooltip) {
        this.translatableName = translatableName;
        this.translatableTooltip = translatableTooltip;
    }

    public String translatableName() {
        return translatableName;
    }

    public String translatableTooltip() {
        return translatableTooltip;
    }
}
