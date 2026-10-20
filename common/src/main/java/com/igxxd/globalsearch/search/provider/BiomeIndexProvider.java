package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.platform.Services;
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class BiomeIndexProvider implements SearchIndexProvider {

    @Override
    public String getName() {
        return "Biomes";
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        try {
            HolderLookup.Provider lookup = (minecraft != null && minecraft.level != null)
                    ? minecraft.level.registryAccess()
                    : VanillaRegistries.createWorldLookup();

            lookup.lookup(Registries.BIOME).ifPresent(biomeRegistry -> {
                biomeRegistry.listElements().forEach(ref -> {
                try {
                    Identifier id = ref.key().identifier();
                    Biome biome = ref.value();
                    String transKey = "biome." + id.getNamespace() + "." + id.getPath();
                    String title = Component.translatable(transKey).getString();
                    if (title.equals(transKey)) {
                        title = ProviderUtils.formatCamelCase(id.getPath());
                    }

                    String modId = id.getNamespace();
                    String modName = Services.PLATFORM.getModName(modId);
                    float temp = biome.getBaseTemperature();
                    String val = String.format(Locale.ROOT, "Temp: %.1f", temp);
                    String path = id.toString();
                    String key = SearchResultType.BIOME.name() + ":" + path.toLowerCase(Locale.ROOT);

                    if (seenKeys.add(key)) {
                        String tags = ref.tags()
                                .map(t -> "#" + t.location().getPath())
                                .collect(Collectors.joining(" "));

                        String desc = "Biome in " + modName + " (Temp: " + temp + (biome.hasPrecipitation() ? ", Rain/Snow" : ", Dry") + ")";
                        if (!tags.isEmpty()) {
                            desc += " • Tags: " + tags;
                        }

                        ItemStack icon = pickBiomeIcon(id, tags);

                        results.add(new SearchResult(
                                SearchResultType.BIOME,
                                path,
                                title,
                                path,
                                val,
                                modId,
                                modName,
                                icon,
                                desc
                        ));
                    }
                } catch (Throwable ignored) {}
            });
        });
        } catch (Throwable ignored) {}
    }

    private ItemStack pickBiomeIcon(Identifier id, String tags) {
        String p = id.getPath().toLowerCase(Locale.ROOT);
        String t = tags.toLowerCase(Locale.ROOT);
        if (t.contains("nether") || p.contains("nether") || p.contains("crimson") || p.contains("warped") || p.contains("basalt") || p.contains("soul")) {
            return new ItemStack(Items.NETHERRACK);
        }
        if (t.contains("end") || p.contains("end")) {
            return new ItemStack(Items.END_STONE);
        }
        if (t.contains("ocean") || p.contains("ocean") || p.contains("river") || p.contains("beach")) {
            return new ItemStack(Items.WATER_BUCKET);
        }
        if (p.contains("desert") || p.contains("badlands") || p.contains("mesa")) {
            return new ItemStack(Items.SAND);
        }
        if (p.contains("snow") || p.contains("ice") || p.contains("frozen") || p.contains("tundra")) {
            return new ItemStack(Items.SNOW_BLOCK);
        }
        if (p.contains("jungle") || p.contains("bamboo")) {
            return new ItemStack(Items.JUNGLE_SAPLING);
        }
        if (p.contains("swamp") || p.contains("mangrove")) {
            return new ItemStack(Items.LILY_PAD);
        }
        if (p.contains("cherry")) {
            return new ItemStack(Items.CHERRY_SAPLING);
        }
        if (p.contains("dark_forest")) {
            return new ItemStack(Items.DARK_OAK_SAPLING);
        }
        if (p.contains("cave") || p.contains("deep")) {
            return new ItemStack(Items.DRIPSTONE_BLOCK);
        }
        return new ItemStack(Items.GRASS_BLOCK);
    }
}
