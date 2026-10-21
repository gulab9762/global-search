package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.gui.GlobalSettingEditScreen;
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
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class StructureIndexProvider implements SearchIndexProvider {

    @Override
    public String getName() {
        return "Structures";
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        try {
            HolderLookup.Provider lookup = (minecraft != null && minecraft.level != null)
                    ? minecraft.level.registryAccess()
                    : VanillaRegistries.createWorldLookup();

            lookup.lookup(Registries.STRUCTURE).ifPresent(reg -> {
                reg.listElements().forEach(ref -> {
                    try {
                        Identifier id = ref.key().identifier();
                        String path = id.toString();
                        String key = SearchResultType.STRUCTURE.name() + ":" + path.toLowerCase(Locale.ROOT);

                        if (seenKeys.add(key)) {
                            String modId = id.getNamespace();
                            String modName = Services.PLATFORM.getModName(modId);
                            String transKey = "structure." + id.getNamespace() + "." + id.getPath();
                            String transTitle = Component.translatable(transKey).getString();
                            final String title = transTitle.equals(transKey) ? ProviderUtils.formatCamelCase(id.getPath()) : transTitle;

                            ItemStack icon = pickStructureIcon(id);
                            String locateCmd = "/locate structure " + path;
                            String desc = "World structure in " + modName + ". Locate with: " + locateCmd;

                            List<String> actions = List.of("Copy /locate", "Locate in Chat", "Copy Structure ID");

                            SearchResult result = new SearchResult(
                                    SearchResultType.STRUCTURE,
                                    path,
                                    title,
                                    path,
                                    "Structure",
                                    modId,
                                    modName,
                                    icon,
                                    null,
                                    desc,
                                    true,
                                    (mc, screen, onValueChanged) -> {
                                        mc.gui.setScreen(new GlobalSettingEditScreen(
                                                screen,
                                                new SearchResult(SearchResultType.STRUCTURE, path, title, path, "Copy /locate", modId, modName, icon),
                                                GlobalSettingEditScreen.SettingType.CHOICE,
                                                actions,
                                                "Copy /locate",
                                                selectedAction -> {
                                                    if ("Locate in Chat".equalsIgnoreCase(selectedAction)) {
                                                        if (mc.player != null && mc.player.connection != null) {
                                                            mc.player.connection.sendCommand("locate structure " + path);
                                                        }
                                                    } else if ("Copy Structure ID".equalsIgnoreCase(selectedAction)) {
                                                        mc.keyboardHandler.setClipboard(path);
                                                    } else {
                                                        mc.keyboardHandler.setClipboard(locateCmd);
                                                    }
                                                    return true;
                                                },
                                                onValueChanged
                                        ));
                                        return true;
                                    },
                                    StructureIndexProvider.this
                            );
                            results.add(result);
                        }
                    } catch (Throwable ignored) {}
                });
            });
        } catch (Throwable ignored) {}
    }

    private ItemStack pickStructureIcon(Identifier id) {
        String p = id.getPath().toLowerCase(Locale.ROOT);
        if (p.contains("fortress") || p.contains("bastion") || p.contains("nether")) {
            return new ItemStack(Items.NETHER_BRICKS);
        }
        if (p.contains("end_city") || p.contains("end")) {
            return new ItemStack(Items.PURPUR_BLOCK);
        }
        if (p.contains("ancient_city")) {
            return new ItemStack(Items.REINFORCED_DEEPSLATE);
        }
        if (p.contains("trial_chamber")) {
            return new ItemStack(Items.TRIAL_KEY);
        }
        if (p.contains("monument") || p.contains("ocean")) {
            return new ItemStack(Items.PRISMARINE_BRICKS);
        }
        if (p.contains("mansion") || p.contains("woodland")) {
            return new ItemStack(Items.DARK_OAK_LOG);
        }
        if (p.contains("village")) {
            return new ItemStack(Items.EMERALD);
        }
        if (p.contains("desert_pyramid") || p.contains("pyramid")) {
            return new ItemStack(Items.CHISELED_SANDSTONE);
        }
        if (p.contains("jungle_pyramid") || p.contains("temple")) {
            return new ItemStack(Items.MOSSY_COBBLESTONE);
        }
        if (p.contains("mineshaft")) {
            return new ItemStack(Items.RAIL);
        }
        if (p.contains("shipwreck")) {
            return new ItemStack(Items.SPRUCE_BOAT);
        }
        if (p.contains("buried_treasure")) {
            return new ItemStack(Items.HEART_OF_THE_SEA);
        }
        if (p.contains("swamp_hut") || p.contains("witch")) {
            return new ItemStack(Items.CAULDRON);
        }
        if (p.contains("pillager")) {
            return new ItemStack(Items.CROSSBOW);
        }
        return new ItemStack(Items.CHISELED_STONE_BRICKS);
    }
}
