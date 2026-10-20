package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.Constants;
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class XaeroConfigIndexProvider implements SearchIndexProvider {

    @Override
    public String getName() {
        return "Xaero's Maps";
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        // 1. Xaero's Minimap
        indexXaeroConfigClass("xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions", "xaerominimap", "Xaero's Minimap", "Minimap Settings", results, seenKeys);
        indexXaeroConfigClass("xaero.hud.minimap.config.primary.option.MinimapPrimaryClientConfigOptions", "xaerominimap", "Xaero's Minimap", "Minimap Settings", results, seenKeys);
        indexXaeroConfigClass("xaero.hud.minimap.common.config.primary.option.MinimapPrimaryCommonConfigOptions", "xaerominimap", "Xaero's Minimap", "Minimap Settings", results, seenKeys);

        // 2. Xaero's World Map
        indexXaeroConfigClass("xaero.map.common.config.option.WorldMapProfiledConfigOptions", "xaeroworldmap", "Xaero's World Map", "World Map Settings", results, seenKeys);
        indexXaeroConfigClass("xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions", "xaeroworldmap", "Xaero's World Map", "World Map Settings", results, seenKeys);
        indexXaeroConfigClass("xaero.map.common.config.primary.option.WorldMapPrimaryCommonConfigOptions", "xaeroworldmap", "Xaero's World Map", "World Map Settings", results, seenKeys);
    }

    private void indexXaeroConfigClass(String className, String modId, String modName, String baseMenu, List<SearchResult> results, Set<String> seenKeys) {
        try {
            Class<?> cls = Class.forName(className);
            for (Field field : cls.getFields()) {
                if (!Modifier.isStatic(field.getModifiers())) continue;

                Object optionObj = field.get(null);
                if (optionObj == null) continue;

                try {
                    Method getIdM = optionObj.getClass().getMethod("getId");
                    String id = (String) getIdM.invoke(optionObj);

                    String title = ProviderUtils.formatCamelCase(field.getName());
                    String transKey = null;
                    try {
                        Method getDisplayNameM = optionObj.getClass().getMethod("getDisplayName");
                        Component comp = (Component) getDisplayNameM.invoke(optionObj);
                        if (comp != null) {
                            if (!comp.getString().isEmpty()) {
                                title = comp.getString();
                            }
                            if (comp.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc) {
                                transKey = tc.getKey();
                            }
                        }
                    } catch (Throwable ignored) {}

                    String tooltip = "";
                    try {
                        Method getTooltipM = optionObj.getClass().getMethod("getTooltip");
                        Component tComp = (Component) getTooltipM.invoke(optionObj);
                        if (tComp != null) {
                            tooltip = tComp.getString();
                        }
                    } catch (Throwable ignored) {}

                    String val = "";
                    try {
                        Method getDefaultValueM = optionObj.getClass().getMethod("getDefaultValue");
                        Object defVal = getDefaultValueM.invoke(optionObj);
                        if (defVal != null) {
                            val = String.valueOf(defVal);
                        }
                    } catch (Throwable ignored) {}

                    String subCategory = decodeXaeroCategory(id, field.getName(), transKey);
                    String fullPath = subCategory.isEmpty() ? (modName + " > " + baseMenu) : (modName + " > " + baseMenu + " > " + subCategory);
                    String uniqueKey = SearchResultType.SETTING.name() + ":" + modId + ":" + id.toLowerCase(Locale.ROOT);

                    if (seenKeys.add(uniqueKey)) {
                        String desc = tooltip;
                        if (desc.isEmpty() && transKey != null) {
                            net.minecraft.locale.Language lang = net.minecraft.locale.Language.getInstance();
                            if (lang.has(transKey + ".tooltip")) {
                                desc = lang.getOrDefault(transKey + ".tooltip");
                            } else if (lang.has(transKey + "_tooltip")) {
                                desc = lang.getOrDefault(transKey + "_tooltip");
                            } else if (lang.has(transKey + ".desc")) {
                                desc = lang.getOrDefault(transKey + ".desc");
                            }
                        }
                        if (desc.isEmpty()) {
                            desc = title + " (" + fullPath + ")";
                        }

                        results.add(new SearchResult(
                                SearchResultType.SETTING,
                                modId + ":setting:" + id,
                                title,
                                fullPath,
                                val,
                                modId,
                                modName,
                                ItemStack.EMPTY,
                                desc
                        ));
                    }
                } catch (Throwable ignored) {}
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable t) {
            Constants.LOG.debug("Could not scan Xaero config class " + className, t);
        }
    }

    private String decodeXaeroCategory(String optionId, String fieldName, String transKey) {
        String token = (optionId + "_" + (fieldName != null ? fieldName : "") + "_" + (transKey != null ? transKey : "")).toLowerCase(Locale.ROOT);

        if (token.contains("block") || token.contains("biome") || token.contains("cave") || token.contains("terrain")) {
            return Component.translatable("gui.xaero_minimap_block_map_settings").getString();
        }
        if (token.contains("radar") || token.contains("entity")) {
            return Component.translatable("gui.xaero_entity_radar_settings").getString();
        }
        if (token.contains("waypoint") || token.contains("deathpoint") || token.contains("teleport")) {
            return Component.translatable("gui.xaero_waypoint_settings").getString();
        }
        if (token.contains("overlay") || token.contains("grid") || token.contains("claim") || token.contains("slime") || token.contains("light_overlay")) {
            return Component.translatable("gui.xaero_overlay_settings").getString();
        }
        if (token.contains("info")) {
            return Component.translatable("gui.xaero_minimap_info_settings").getString();
        }
        if (token.contains("view") || token.contains("north") || token.contains("zoom") || token.contains("size") || token.contains("shape") || token.contains("lighting") || token.contains("opacity") || token.contains("frame")) {
            return Component.translatable("gui.xaero_minimap_view_settings").getString();
        }
        return Component.translatable("gui.xaero_minimap_misc_settings").getString();
    }
}
