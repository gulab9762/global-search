package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.search.SearchResult;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Set;

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
}
