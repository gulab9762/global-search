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
}
