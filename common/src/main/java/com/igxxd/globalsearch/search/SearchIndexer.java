package com.igxxd.globalsearch.search;

import com.igxxd.globalsearch.Constants;
import com.igxxd.globalsearch.search.provider.*;
import net.minecraft.client.Minecraft;

import java.util.*;

/**
 * Main coordinator for indexing search entries across Minecraft and installed mods.
 * Utilizes a pluggable {@link SearchIndexProvider} architecture so providers can be
 * maintained independently without hardcoded dependencies in this core class.
 */
public class SearchIndexer {

    private static volatile List<SearchResult> CACHED_RESULTS = null;

    private static final List<SearchIndexProvider> PROVIDERS = new ArrayList<>(List.of(
            new KeybindIndexProvider(),
            new VanillaSettingIndexProvider(),
            new ItemIndexProvider(),
            new EnchantmentIndexProvider(),
            new PotionIndexProvider(),
            new BiomeIndexProvider(),
            new DimensionIndexProvider(),
            new ModListIndexProvider(),
            new MalilibConfigIndexProvider(),
            new ClothConfigIndexProvider(),
            new NeoForgeConfigIndexProvider(),
            new XaeroConfigIndexProvider()
    ));

    /**
     * Register a custom search index provider dynamically.
     */
    public static synchronized void registerProvider(SearchIndexProvider provider) {
        if (provider != null && !PROVIDERS.contains(provider)) {
            PROVIDERS.add(provider);
        }
    }

    public static synchronized List<SearchResult> getOrIndex(Minecraft minecraft) {
        if (CACHED_RESULTS == null) {
            CACHED_RESULTS = indexAll(minecraft);
        }
        return CACHED_RESULTS;
    }

    public static synchronized int rescan(Minecraft minecraft) {
        CACHED_RESULTS = indexAll(minecraft);
        return CACHED_RESULTS.size();
    }

    public static List<SearchResult> indexAll(Minecraft minecraft) {
        List<SearchResult> results = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        if (minecraft == null) {
            return results;
        }

        for (SearchIndexProvider provider : PROVIDERS) {
            try {
                provider.index(minecraft, results, seenKeys);
            } catch (Throwable t) {
                Constants.LOG.debug("Provider '{}' failed or was skipped: {}", provider.getName(), t.getMessage());
            }
        }

        Constants.LOG.info("Indexed {} total global search entries across {} providers.", results.size(), PROVIDERS.size());
        CACHED_RESULTS = Collections.unmodifiableList(results);
        return CACHED_RESULTS;
    }

    /**
     * Delegates an edit action to the result's custom handler, its bound provider,
     * or queries registered providers.
     */
    public static boolean handleEdit(SearchResult result, net.minecraft.client.gui.screens.Screen screen, java.util.function.Consumer<String> onValueChanged) {
        if (result == null) return false;
        Minecraft mc = Minecraft.getInstance();

        // 1. Direct handler on SearchResult
        if (result.getEditHandler() != null) {
            try {
                if (result.getEditHandler().handleEdit(mc, screen, onValueChanged)) {
                    return true;
                }
            } catch (Throwable t) {
                Constants.LOG.error("Error executing result edit handler: {}", t.getMessage());
            }
        }

        // 2. Bound provider on SearchResult
        if (result.getProvider() != null) {
            try {
                if (result.getProvider().handleEdit(mc, result, screen, onValueChanged)) {
                    return true;
                }
            } catch (Throwable t) {
                Constants.LOG.error("Error in provider '{}' edit handler: {}", result.getProvider().getName(), t.getMessage());
            }
        }

        // 3. Fallback across all registered providers
        for (SearchIndexProvider provider : PROVIDERS) {
            try {
                if (provider.handleEdit(mc, result, screen, onValueChanged)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }

        return false;
    }
}
