package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.platform.Services;
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ItemIndexProvider implements SearchIndexProvider {

    @Override
    public String getName() {
        return "Items";
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        for (Item item : BuiltInRegistries.ITEM) {
            try {
                Identifier id = BuiltInRegistries.ITEM.getKey(item);
                if (id == null) continue;

                String descId = item.getDescriptionId();
                String title = Component.translatable(descId).getString();
                String modId = id.getNamespace();
                String modName = Services.PLATFORM.getModName(modId);
                String value = "Max: " + item.getDefaultMaxStackSize();
                String path = id.toString();
                String key = SearchResultType.ITEM.name() + ":" + path.toLowerCase(Locale.ROOT);

                if (seenKeys.add(key)) {
                    String desc = "";
                    net.minecraft.locale.Language lang = net.minecraft.locale.Language.getInstance();
                    if (lang.has(descId + ".desc")) {
                        desc = lang.getOrDefault(descId + ".desc");
                    } else if (lang.has(descId + ".description")) {
                        desc = lang.getOrDefault(descId + ".description");
                    } else if (lang.has(descId + ".tooltip")) {
                        desc = lang.getOrDefault(descId + ".tooltip");
                    }
                    if (desc.isEmpty()) {
                        desc = "Item: " + title + " (" + path + ")";
                    }

                    results.add(new SearchResult(
                            SearchResultType.ITEM,
                            path,
                            title,
                            path,
                            value,
                            modId,
                            modName,
                            item,
                            desc
                    ));
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
