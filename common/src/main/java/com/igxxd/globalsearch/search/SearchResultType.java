package com.igxxd.globalsearch.search;

public enum SearchResultType {
    ALL("All", "ALL", 0xFFFFFFFF, 0x33FFFFFF, 0xFFFFFFFF),
    KEYBIND("Keybind", "KEY", 0xFFFFAA00, 0x44FFAA00, 0xFFFFAA00),
    SETTING("Setting", "SETTING", 0xFF55FFFF, 0x4455FFFF, 0xFF55FFFF),
    ITEM("Item", "ITEM", 0xFF55FF55, 0x4455FF55, 0xFFAAAAAA),
    MOD("Mod", "MOD", 0xFFFF77FF, 0x44FF77FF, 0xFF88AAFF);

    private final String label;
    private final String shortLabel;
    private final int textColor;
    private final int badgeBackgroundColor;
    private final int valueColor;

    SearchResultType(String label, String shortLabel, int textColor, int badgeBackgroundColor, int valueColor) {
        this.label = label;
        this.shortLabel = shortLabel;
        this.textColor = textColor;
        this.badgeBackgroundColor = badgeBackgroundColor;
        this.valueColor = valueColor;
    }

    public String getLabel() {
        return label;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public int getTextColor() {
        return textColor;
    }

    public int getBadgeBackgroundColor() {
        return badgeBackgroundColor;
    }

    public int getValueColor() {
        return valueColor;
    }
}
