package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.platform.Services;
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ModListIndexProvider implements SearchIndexProvider {

    @Override
    public String getName() {
        return "Installed Mods";
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        Map<String, String> mods = Services.PLATFORM.getLoadedMods();
        for (Map.Entry<String, String> entry : mods.entrySet()) {
            String modId = entry.getKey();
            String modName = entry.getValue();
            String id = "mod:" + modId;
            String key = SearchResultType.MOD.name() + ":" + id.toLowerCase(Locale.ROOT);

            if (seenKeys.add(key)) {
                results.add(new SearchResult(
                        SearchResultType.MOD,
                        id,
                        modName,
                        "Mod ID: " + modId,
                        "Loaded",
                        modId,
                        modName,
                        ItemStack.EMPTY,
                        "Installed Mod: " + modName + " (" + modId + ")"
                ));
            }
        }
    }
}
