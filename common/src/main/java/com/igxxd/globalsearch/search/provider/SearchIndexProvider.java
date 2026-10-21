package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.search.SearchResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Pluggable provider for indexing search results from a specific subsystem or mod framework.
 */
public interface SearchIndexProvider {

    /**
     * Human-readable identifier for logging and debugging.
     */
    String getName();

    /**
     * Index available entries and append them to the results list.
     *
     * @param minecraft Minecraft client instance
     * @param results   Target list of search results
     * @param seenKeys  Set of deduplication keys
     */
    void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys);

    /**
     * Checks if this provider can handle editing for the given result.
     *
     * @param result The search result to check.
     * @return true if editable, false otherwise.
     */
    default boolean canEdit(SearchResult result) {
        return false;
    }

    /**
     * Handle an edit action triggered from a search result produced by this provider.
     *
     * @param minecraft Minecraft client instance
     * @param result The search result being edited
     * @param screen The active screen (typically GlobalSearchScreen)
     * @param onValueChanged Callback to report the new display value
     * @return true if the edit was handled, false otherwise
     */
    default boolean handleEdit(Minecraft minecraft, SearchResult result, Screen screen, Consumer<String> onValueChanged) {
        return false;
    }
}

