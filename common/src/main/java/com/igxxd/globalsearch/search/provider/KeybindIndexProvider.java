package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.platform.Services;
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class KeybindIndexProvider implements SearchIndexProvider {

    @Override
    public String getName() {
        return "Keybinds";
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        if (minecraft.options == null || minecraft.options.keyMappings == null) {
            return;
        }

        for (KeyMapping km : minecraft.options.keyMappings) {
            if (km == null) continue;

            String translationKey = km.getName();
            String title = Component.translatable(translationKey).getString();
            String category = km.getCategory() != null ? km.getCategory().label().getString() : "Miscellaneous";
            String boundKey = km.getTranslatedKeyMessage().getString();

            String modId = "minecraft";
            if (km.getCategory() != null && km.getCategory().id() != null) {
                String catNamespace = km.getCategory().id().getNamespace();
                if (!"minecraft".equalsIgnoreCase(catNamespace)) {
                    modId = catNamespace;
                }
            }
            if ("minecraft".equals(modId) && translationKey.startsWith("key.")) {
                String[] parts = translationKey.split("\\.");
                if (parts.length > 2 && !"minecraft".equals(parts[1]) && !"categories".equals(parts[1])) {
                    modId = parts[1];
                }
            }

            String modName = Services.PLATFORM.getModName(modId);
            String path = "minecraft".equals(modId)
                    ? "Options > Controls > " + category
                    : modName + " > Controls > " + category;

            String key = SearchResultType.KEYBIND.name() + ":" + translationKey.toLowerCase(Locale.ROOT);

            if (seenKeys.add(key)) {
                String desc = "";
                net.minecraft.locale.Language lang = net.minecraft.locale.Language.getInstance();
                if (lang.has(translationKey + ".desc")) {
                    desc = lang.getOrDefault(translationKey + ".desc");
                } else if (lang.has(translationKey + ".description")) {
                    desc = lang.getOrDefault(translationKey + ".description");
                } else if (lang.has(translationKey + ".tooltip")) {
                    desc = lang.getOrDefault(translationKey + ".tooltip");
                }
                if (desc.isEmpty()) {
                    desc = "Keybind: " + title + " [" + boundKey + "] (" + path + ")";
                }

                results.add(new SearchResult(
                        SearchResultType.KEYBIND,
                        translationKey,
                        title,
                        path,
                        boundKey,
                        modId,
                        modName,
                        ItemStack.EMPTY,
                        desc
                ));
            }
        }
    }
}
