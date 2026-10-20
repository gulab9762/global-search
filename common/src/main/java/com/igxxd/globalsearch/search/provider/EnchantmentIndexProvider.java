package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.platform.Services;
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class EnchantmentIndexProvider implements SearchIndexProvider {

    @Override
    public String getName() {
        return "Enchantments";
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        try {
            HolderLookup.Provider lookup = (minecraft != null && minecraft.level != null)
                    ? minecraft.level.registryAccess()
                    : VanillaRegistries.createWorldLookup();

            lookup.lookup(Registries.ENCHANTMENT).ifPresent(enchRegistry -> {
                enchRegistry.listElements().forEach(ref -> {
                try {
                    Identifier id = ref.key().identifier();
                    Enchantment ench = ref.value();
                    String title = ench.description().getString();
                    String modId = id.getNamespace();
                    String modName = Services.PLATFORM.getModName(modId);
                    int maxLvl = ench.definition().maxLevel();
                    String val = "Max Lvl: " + maxLvl;
                    String path = id.toString();
                    String key = SearchResultType.ENCHANTMENT.name() + ":" + path.toLowerCase(Locale.ROOT);

                    if (seenKeys.add(key)) {
                        String tags = ref.tags()
                                .map(t -> "#" + t.location().getPath())
                                .collect(Collectors.joining(" "));

                        String descKey = "enchantment." + id.getNamespace() + "." + id.getPath() + ".desc";
                        String altDescKey = "description.enchantment." + id.getNamespace() + "." + id.getPath();
                        Language lang = Language.getInstance();
                        String desc = "";
                        if (lang.has(descKey)) {
                            desc = lang.getOrDefault(descKey);
                        } else if (lang.has(altDescKey)) {
                            desc = lang.getOrDefault(altDescKey);
                        }

                        if (desc.isEmpty() && "minecraft".equalsIgnoreCase(id.getNamespace())) {
                            String vanillaDesc = VanillaEnchantmentDescriptions.getDescription(id.getPath());
                            if (vanillaDesc != null) {
                                desc = vanillaDesc;
                            }
                        }

                        if (desc.isEmpty()) {
                            desc = "Enchantment (Max Lvl " + maxLvl + ")";
                        }
                        if (!tags.isEmpty()) {
                            desc += " • Tags: " + tags;
                        }

                        results.add(new SearchResult(
                                SearchResultType.ENCHANTMENT,
                                path,
                                title,
                                path,
                                val,
                                modId,
                                modName,
                                new ItemStack(Items.ENCHANTED_BOOK),
                                desc
                        ));
                    }
                } catch (Throwable ignored) {}
            });
        });
        } catch (Throwable ignored) {}
    }
}
