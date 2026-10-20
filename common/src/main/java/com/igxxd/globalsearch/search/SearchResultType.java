package com.igxxd.globalsearch.search;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;
import java.util.function.Supplier;

/**
 * Represents a search category (formerly SearchResultType enum).
 * Converted to an open, extensible registry class so external mods can register
 * their own custom search categories dynamically.
 */
public class SearchResultType {

    private static final Map<String, SearchResultType> REGISTRY = new LinkedHashMap<>();

    public static final SearchResultType ALL = register(new SearchResultType("all", "All", "ALL", 0xFFFFFFFF, 0x33FFFFFF, 0xFFFFFFFF, () -> new ItemStack(Items.COMPASS)));
    public static final SearchResultType KEYBIND = register(new SearchResultType("keybind", "Keybind", "KEY", 0xFFFFAA00, 0x44FFAA00, 0xFFFFAA00, () -> new ItemStack(Items.TRIPWIRE_HOOK)));
    public static final SearchResultType SETTING = register(new SearchResultType("setting", "Setting", "SETTING", 0xFF55FFFF, 0x4455FFFF, 0xFF55FFFF, () -> new ItemStack(Items.COMPARATOR)));
    public static final SearchResultType ITEM = register(new SearchResultType("item", "Item", "ITEM", 0xFF55FF55, 0x4455FF55, 0xFFAAAAAA, () -> new ItemStack(Items.CHEST)));
    public static final SearchResultType ENCHANTMENT = register(new SearchResultType("enchantment", "Enchantment", "ENCHANT", 0xFFFF77FF, 0x44FF77FF, 0xFFFFAA00, () -> new ItemStack(Items.ENCHANTED_BOOK)));
    public static final SearchResultType POTION = register(new SearchResultType("potion", "Potion & Effect", "POTION", 0xFFFF5555, 0x44FF5555, 0xFFFF7777, () -> new ItemStack(Items.POTION)));
    public static final SearchResultType BIOME = register(new SearchResultType("biome", "Biome", "BIOME", 0xFF55FF55, 0x4455FF55, 0xFF88FF88, () -> new ItemStack(Items.GRASS_BLOCK)));
    public static final SearchResultType DIMENSION = register(new SearchResultType("dimension", "Dimension", "DIM", 0xFFAA55FF, 0x44AA55FF, 0xFFDD88FF, () -> new ItemStack(Items.ENDER_EYE)));
    public static final SearchResultType MOD = register(new SearchResultType("mod", "Mod", "MOD", 0xFF55AAFF, 0x4455AAFF, 0xFF88AAFF, () -> new ItemStack(Items.KNOWLEDGE_BOOK)));

    private final String id;
    private final String label;
    private final String shortLabel;
    private final int textColor;
    private final int badgeBackgroundColor;
    private final int valueColor;
    private final Supplier<ItemStack> defaultIconSupplier;

    public SearchResultType(String id, String label, String shortLabel, int textColor, int badgeBackgroundColor, int valueColor, Supplier<ItemStack> defaultIconSupplier) {
        this.id = id != null ? id.toLowerCase(Locale.ROOT) : "";
        this.label = label != null ? label : this.id;
        this.shortLabel = shortLabel != null ? shortLabel : this.label;
        this.textColor = textColor;
        this.badgeBackgroundColor = badgeBackgroundColor;
        this.valueColor = valueColor;
        this.defaultIconSupplier = defaultIconSupplier != null ? defaultIconSupplier : () -> ItemStack.EMPTY;
    }

    public static synchronized SearchResultType register(SearchResultType type) {
        if (type != null) {
            REGISTRY.put(type.getId(), type);
        }
        return type;
    }

    public static synchronized SearchResultType register(String id, String label, String shortLabel, int textColor, int badgeBackgroundColor, int valueColor, Supplier<ItemStack> defaultIconSupplier) {
        return register(new SearchResultType(id, label, shortLabel, textColor, badgeBackgroundColor, valueColor, defaultIconSupplier));
    }

    public static synchronized SearchResultType byId(String id) {
        if (id == null) return null;
        return REGISTRY.get(id.toLowerCase(Locale.ROOT));
    }

    public static synchronized List<SearchResultType> getAllCategories() {
        return new ArrayList<>(REGISTRY.values());
    }

    public static synchronized SearchResultType[] values() {
        return REGISTRY.values().toArray(new SearchResultType[0]);
    }

    public String getId() {
        return id;
    }

    public String name() {
        return id.toUpperCase(Locale.ROOT);
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

    public ItemStack getDefaultIcon() {
        try {
            ItemStack stack = defaultIconSupplier.get();
            return stack != null ? stack : ItemStack.EMPTY;
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchResultType that = (SearchResultType) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return label;
    }
}
