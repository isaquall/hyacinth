package me.isaquall.hyacinth.schematic;

public enum SupportMode {
    NEVER("hyacinth.support_mode.never", "hyacinth.support_mode.never_tooltip"),
    ONLY_REQUIRED("hyacinth.support_mode.only_required", "hyacinth.support_mode.only_required_tooltip"),
    ALWAYS("hyacinth.support_mode.always", "hyacinth.support_mode.always_tooltip"),;

    private final String translatableName;
    private final String translatableTooltip;

    SupportMode(String translatableName, String translatableTooltip) {
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
