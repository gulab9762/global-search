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
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class DimensionIndexProvider implements SearchIndexProvider {

    @Override
    public String getName() {
        return "Dimensions";
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        // 1. Default vanilla dimensions
        addDimension(results, seenKeys, Level.OVERWORLD.identifier(), "The Overworld", "Scale: 1:1",
                new ItemStack(Items.GRASS_BLOCK), "The default starting dimension with day/night cycle and weather.");
        addDimension(results, seenKeys, Level.NETHER.identifier(), "The Nether", "Scale: 1:8",
                new ItemStack(Items.NETHERRACK), "A dangerous, hot dimension covered in lava and netherrack.");
        addDimension(results, seenKeys, Level.END.identifier(), "The End", "Scale: 1:1",
                new ItemStack(Items.END_STONE), "A dark, void dimension filled with floating islands and endermen.");

        // 2. Active server/client levels from connection
        if (minecraft != null && minecraft.getConnection() != null) {
            try {
                Set<ResourceKey<Level>> levels = minecraft.getConnection().levels();
                for (ResourceKey<Level> levelKey : levels) {
                    Identifier id = levelKey.identifier();
                    String path = id.toString();
                    String key = SearchResultType.DIMENSION.name() + ":" + path.toLowerCase(Locale.ROOT);
                    if (!seenKeys.contains(key)) {
                        String modId = id.getNamespace();
                        String modName = Services.PLATFORM.getModName(modId);
                        String title = ProviderUtils.formatCamelCase(id.getPath());
                        addDimension(results, seenKeys, id, title, "Custom Dimension",
                                new ItemStack(Items.ENDER_EYE), "Active dimension from " + modName + " (" + path + ")");
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 3. Dynamic DimensionTypes from registries (vanilla + modded)
        try {
            HolderLookup.Provider lookup = (minecraft != null && minecraft.level != null)
                    ? minecraft.level.registryAccess()
                    : VanillaRegistries.createWorldLookup();

            lookup.lookup(Registries.DIMENSION_TYPE).ifPresent(dimLookup -> {
                dimLookup.listElements().forEach(ref -> {
                    try {
                        Identifier id = ref.key().identifier();
                        String path = id.toString();
                        String key = SearchResultType.DIMENSION.name() + ":" + path.toLowerCase(Locale.ROOT);
                        if (seenKeys.add(key)) {
                            DimensionType dim = ref.value();
                            String modId = id.getNamespace();
                            String modName = Services.PLATFORM.getModName(modId);
                            String title = ProviderUtils.formatCamelCase(id.getPath());
                            double scale = dim.coordinateScale();
                            String val = String.format(Locale.ROOT, "Scale 1:%.0f", scale);

                            String desc = "Dimension Type in " + modName
                                    + " • Ceiling: " + (dim.hasCeiling() ? "Yes" : "No")
                                    + " • Skylight: " + (dim.hasSkyLight() ? "Yes" : "No")
                                    + " • Fixed Time: " + (dim.hasFixedTime() ? "Yes" : "No");

                            ItemStack icon = pickDimensionIcon(id);

                            results.add(new SearchResult(
                                    SearchResultType.DIMENSION,
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

    private void addDimension(List<SearchResult> results, Set<String> seenKeys, Identifier id, String title, String val, ItemStack icon, String desc) {
        String path = id.toString();
        String key = SearchResultType.DIMENSION.name() + ":" + path.toLowerCase(Locale.ROOT);
        if (seenKeys.add(key)) {
            String modId = id.getNamespace();
            String modName = Services.PLATFORM.getModName(modId);
            results.add(new SearchResult(
                    SearchResultType.DIMENSION,
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
    }

    private ItemStack pickDimensionIcon(Identifier id) {
        String p = id.getPath().toLowerCase(Locale.ROOT);
        if (p.contains("nether")) return new ItemStack(Items.NETHERRACK);
        if (p.contains("end")) return new ItemStack(Items.END_STONE);
        if (p.contains("overworld")) return new ItemStack(Items.GRASS_BLOCK);
        if (p.contains("mining")) return new ItemStack(Items.DIAMOND_PICKAXE);
        if (p.contains("twilight") || p.contains("forest")) return new ItemStack(Items.OAK_SAPLING);
        if (p.contains("aether") || p.contains("sky")) return new ItemStack(Items.FEATHER);
        return new ItemStack(Items.COMPASS);
    }
}
