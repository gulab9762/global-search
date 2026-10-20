package com.igxxd.globalsearch.api;

import com.igxxd.globalsearch.gui.GlobalSearchScreen;
import com.igxxd.globalsearch.search.SearchIndexer;
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import com.igxxd.globalsearch.search.provider.SearchIndexProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/**
 * Public API for the Global Search mod.
 * Allows other mods to use Global Search as a library to:
 * - Register custom search categories with custom icons and styling.
 * - Register custom search index providers.
 * - Query or rescan the central index programmatically.
 * - Open the Global Search GUI.
 */
public final class GlobalSearchApi {

    private GlobalSearchApi() {}

    /**
     * Registers a custom search index provider.
     * Providers are invoked during indexing to populate search results.
     *
     * @param provider The provider implementation.
     */
    public static void registerProvider(SearchIndexProvider provider) {
        SearchIndexer.registerProvider(provider);
    }

    /**
     * Registers a new search category with default styling derived from text color.
     *
     * @param id Unique identifier (e.g., "quest", "spell", "recipe").
     * @param label Display name shown on the filter button.
     * @param shortLabel Short 3-6 letter code for badges.
     * @param textColor Color used for text in hex (ARGB).
     * @param defaultIconSupplier Supplier for the default in-game ItemStack icon.
     * @return The registered SearchResultType instance.
     */
    public static SearchResultType registerCategory(String id, String label, String shortLabel, int textColor, Supplier<ItemStack> defaultIconSupplier) {
        int bg = (textColor & 0x00FFFFFF) | 0x33000000;
        return SearchResultType.register(id, label, shortLabel, textColor, bg, textColor, defaultIconSupplier);
    }

    /**
     * Registers a new search category with full styling options.
     */
    public static SearchResultType registerCategory(String id, String label, String shortLabel, int textColor, int badgeBgColor, int valueColor, Supplier<ItemStack> defaultIconSupplier) {
        return SearchResultType.register(id, label, shortLabel, textColor, badgeBgColor, valueColor, defaultIconSupplier);
    }

    /**
     * Returns all registered categories.
     */
    public static List<SearchResultType> getCategories() {
        return SearchResultType.getAllCategories();
    }

    /**
     * Looks up a registered category by its ID.
     */
    public static SearchResultType getCategoryById(String id) {
        return SearchResultType.byId(id);
    }

    /**
     * Retrieves all indexed results, executing an index pass if not already indexed.
     */
    public static List<SearchResult> getOrIndex(Minecraft minecraft) {
        return SearchIndexer.getOrIndex(minecraft);
    }

    /**
     * Forces a rescan of all providers.
     */
    public static int rescan(Minecraft minecraft) {
        return SearchIndexer.rescan(minecraft);
    }

    /**
     * Triggers the edit action for a given search result.
     * Tries the result's custom edit handler first, then delegates to providers.
     *
     * @param result The search result to edit.
     * @param screen The active screen.
     * @param onValueChanged Callback to report the new display value.
     * @return true if the edit was handled, false otherwise.
     */
    public static boolean triggerEdit(SearchResult result, Screen screen, java.util.function.Consumer<String> onValueChanged) {
        return SearchIndexer.handleEdit(result, screen, onValueChanged);
    }

    /**
     * Opens the Global Search screen in the client.
     *
     * @param parent The parent screen to return to when closed.
     */
    public static void openSearchScreen(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.gui.setScreen(new GlobalSearchScreen(parent));
        }
    }
}
