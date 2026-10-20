package com.igxxd.globalsearch.search;

public enum SearchResultType {
    ALL("All", "ALL", 0xFFFFFFFF, 0x33FFFFFF, 0xFFFFFFFF),
    KEYBIND("Keybind", "KEY", 0xFFFFAA00, 0x44FFAA00, 0xFFFFAA00),
    SETTING("Setting", "SETTING", 0xFF55FFFF, 0x4455FFFF, 0xFF55FFFF),
    ITEM("Item", "ITEM", 0xFF55FF55, 0x4455FF55, 0xFFAAAAAA),
    ENCHANTMENT("Enchantment", "ENCHANT", 0xFFFF77FF, 0x44FF77FF, 0xFFFFAA00),
    POTION("Potion & Effect", "POTION", 0xFFFF5555, 0x44FF5555, 0xFFFF7777),
    BIOME("Biome", "BIOME", 0xFF55FF55, 0x4455FF55, 0xFF88FF88),
    DIMENSION("Dimension", "DIM", 0xFFAA55FF, 0x44AA55FF, 0xFFDD88FF),
    MOD("Mod", "MOD", 0xFF55AAFF, 0x4455AAFF, 0xFF88AAFF);

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

    public net.minecraft.world.item.ItemStack getDefaultIcon() {
        return switch (this) {
            case KEYBIND -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TRIPWIRE_HOOK);
            case SETTING -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COMPARATOR);
            case ITEM -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHEST);
            case ENCHANTMENT -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
            case POTION -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.POTION);
            case BIOME -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GRASS_BLOCK);
            case DIMENSION -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_EYE);
            case MOD -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.KNOWLEDGE_BOOK);
            default -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COMPASS);
        };
    }
}

