package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.Constants;
import com.igxxd.globalsearch.gui.GlobalSettingEditScreen;
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;

public class XaeroConfigIndexProvider implements SearchIndexProvider {

    private static class XaeroOptionEntry {
        final String modId;
        final Object optionObj;
        final Object channel;
        final String defaultValue;
        final List<String> validChoices;

        XaeroOptionEntry(String modId, Object optionObj, Object channel, String defaultValue, List<String> validChoices) {
            this.modId = modId;
            this.optionObj = optionObj;
            this.channel = channel;
            this.defaultValue = defaultValue;
            this.validChoices = validChoices;
        }
    }

    private final Map<String, XaeroOptionEntry> optionMap = new HashMap<>();

    @Override
    public String getName() {
        return "Xaero's Maps";
    }

    @Override
    public int getPriority() {
        return 80;
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        optionMap.clear();

        // 1. Xaero's Minimap
        Object minimapChannel = getChannel("xaerominimap");
        indexXaeroConfigClass("xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions", "xaerominimap", "Xaero's Minimap", "Minimap Settings", minimapChannel, results, seenKeys);
        indexXaeroConfigClass("xaero.hud.minimap.config.primary.option.MinimapPrimaryClientConfigOptions", "xaerominimap", "Xaero's Minimap", "Minimap Settings", minimapChannel, results, seenKeys);
        indexXaeroConfigClass("xaero.hud.minimap.common.config.primary.option.MinimapPrimaryCommonConfigOptions", "xaerominimap", "Xaero's Minimap", "Minimap Settings", minimapChannel, results, seenKeys);

        // 2. Xaero's World Map
        Object worldMapChannel = getChannel("xaeroworldmap");
        indexXaeroConfigClass("xaero.map.common.config.option.WorldMapProfiledConfigOptions", "xaeroworldmap", "Xaero's World Map", "World Map Settings", worldMapChannel, results, seenKeys);
        indexXaeroConfigClass("xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions", "xaeroworldmap", "Xaero's World Map", "World Map Settings", worldMapChannel, results, seenKeys);
        indexXaeroConfigClass("xaero.map.common.config.primary.option.WorldMapPrimaryCommonConfigOptions", "xaeroworldmap", "Xaero's World Map", "World Map Settings", worldMapChannel, results, seenKeys);
    }

    private Object getChannel(String modId) {
        try {
            if ("xaerominimap".equals(modId)) {
                Class<?> mmCls = Class.forName("xaero.minimap.XaeroMinimap");
                Object inst = mmCls.getField("instance").get(null);
                if (inst != null) {
                    return inst.getClass().getMethod("getHudConfigs").invoke(inst);
                }
            } else if ("xaeroworldmap".equals(modId)) {
                Class<?> wmCls = Class.forName("xaero.map.WorldMap");
                Object inst = wmCls.getField("INSTANCE").get(null);
                if (inst != null) {
                    return inst.getClass().getMethod("getConfigs").invoke(inst);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private void indexXaeroConfigClass(String className, String modId, String modName, String baseMenu, Object channel, List<SearchResult> results, Set<String> seenKeys) {
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

                    String defValStr = "";
                    try {
                        Method getDefaultValueM = optionObj.getClass().getMethod("getDefaultValue");
                        Object defVal = getDefaultValueM.invoke(optionObj);
                        if (defVal != null) {
                            defValStr = String.valueOf(defVal);
                        }
                    } catch (Throwable ignored) {}

                    // Get live effective value if available, otherwise default value
                    String currentValStr = defValStr;
                    Object effectiveVal = getEffectiveOptionValue(channel, optionObj);
                    if (effectiveVal != null) {
                        currentValStr = String.valueOf(effectiveVal);
                    }

                    // Extract valid choices if IndexedConfigOption
                    List<String> validChoices = new ArrayList<>();
                    try {
                        Method getValidValuesM = optionObj.getClass().getMethod("getValidValues");
                        Object listObj = getValidValuesM.invoke(optionObj);
                        if (listObj instanceof List<?> list) {
                            for (Object item : list) {
                                validChoices.add(String.valueOf(item));
                            }
                        }
                    } catch (Throwable ignored) {}

                    String subCategory = decodeXaeroCategory(id, field.getName(), transKey);
                    String fullPath = subCategory.isEmpty() ? (modName + " > " + baseMenu) : (modName + " > " + baseMenu + " > " + subCategory);
                    String resultId = modId + ":setting:" + id;
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

                        optionMap.put(resultId, new XaeroOptionEntry(modId, optionObj, channel, defValStr, validChoices));

                        SearchResult sr = SearchResult.builder(SearchResultType.SETTING, resultId, title)
                                .path(fullPath)
                                .value(currentValStr)
                                .modId(modId)
                                .modName(modName)
                                .description(desc)
                                .editable(true)
                                .provider(this)
                                .build();

                        results.add(sr);
                    }
                } catch (Throwable ignored) {}
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable t) {
            Constants.LOG.debug("Could not scan Xaero config class " + className, t);
        }
    }

    private Object getEffectiveOptionValue(Object channel, Object optionObj) {
        if (channel == null || optionObj == null) return null;
        try {
            Method getClientConfigManagerM = channel.getClass().getMethod("getClientConfigManager");
            Object clientConfigManager = getClientConfigManagerM.invoke(channel);
            if (clientConfigManager != null) {
                for (Method m : clientConfigManager.getClass().getMethods()) {
                    if ("getEffective".equals(m.getName()) && m.getParameterCount() == 1) {
                        return m.invoke(clientConfigManager, optionObj);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public boolean setXaeroOptionValue(Object channel, Object optionObj, Object newVal) {
        if (channel == null || optionObj == null) return false;
        try {
            Method getClientConfigManagerM = channel.getClass().getMethod("getClientConfigManager");
            Object clientConfigManager = getClientConfigManagerM.invoke(channel);
            if (clientConfigManager == null) return false;

            // 1. Try profiled config profile first (covers >90% of options)
            try {
                Method getCurrentProfileM = clientConfigManager.getClass().getMethod("getCurrentProfile");
                Object profile = getCurrentProfileM.invoke(clientConfigManager);
                if (profile != null) {
                    boolean modified = false;
                    for (Method m : profile.getClass().getMethods()) {
                        if ("set".equals(m.getName()) && m.getParameterCount() == 2) {
                            m.invoke(profile, optionObj, newVal);
                            modified = true;
                            break;
                        }
                    }
                    if (modified) {
                        try {
                            Method getIO_M = channel.getClass().getMethod("getClientConfigProfileIO");
                            Object profileIO = getIO_M.invoke(channel);
                            if (profileIO != null) {
                                for (Method m : profileIO.getClass().getMethods()) {
                                    if ("save".equals(m.getName()) && m.getParameterCount() == 1) {
                                        m.invoke(profileIO, profile);
                                        break;
                                    }
                                }
                            }
                        } catch (Throwable ignored) {}
                        return true;
                    }
                }
            } catch (Throwable ignored) {}

            // 2. Try primary client config manager
            try {
                Method getPrimaryManagerM = clientConfigManager.getClass().getMethod("getPrimaryConfigManager");
                Object primaryMgr = getPrimaryManagerM.invoke(clientConfigManager);
                if (primaryMgr != null) {
                    Method getM = primaryMgr.getClass().getMethod("get");
                    Object primaryCfg = getM.invoke(primaryMgr);
                    if (primaryCfg != null) {
                        boolean modified = false;
                        for (Method m : primaryCfg.getClass().getMethods()) {
                            if ("set".equals(m.getName()) && m.getParameterCount() == 2) {
                                m.invoke(primaryCfg, optionObj, newVal);
                                modified = true;
                                break;
                            }
                        }
                        if (modified) {
                            try {
                                Method getPrimaryIOM = channel.getClass().getMethod("getPrimaryClientConfigManagerIO");
                                Object primaryIO = getPrimaryIOM.invoke(channel);
                                if (primaryIO != null) {
                                    for (Method m : primaryIO.getClass().getMethods()) {
                                        if ("save".equals(m.getName()) && m.getParameterCount() == 1) {
                                            m.invoke(primaryIO, primaryCfg);
                                            break;
                                        }
                                    }
                                }
                            } catch (Throwable ignored) {}
                            return true;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        } catch (Throwable t) {
            Constants.LOG.error("Failed to set Xaero option: {}", t.getMessage());
        }
        return false;
    }

    private Object parseXaeroValue(Object optionObj, String valStr) {
        if (valStr == null) return null;
        String s = valStr.trim();
        String clsName = optionObj.getClass().getSimpleName();

        if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(s);
        }
        if (clsName.contains("Range")) {
            try { return Integer.parseInt(s); } catch (Exception ignored) {}
        }
        if (clsName.contains("Stepped")) {
            try { return Double.parseDouble(s); } catch (Exception ignored) {}
        }

        try { return Integer.parseInt(s); } catch (Exception ignored) {}
        try { return Double.parseDouble(s); } catch (Exception ignored) {}

        return s;
    }

    @Override
    public boolean canEdit(SearchResult result) {
        return result != null && optionMap.containsKey(result.getId());
    }

    @Override
    public boolean handleEdit(Minecraft minecraft, SearchResult result, Screen screen, Consumer<String> onValueChanged) {
        if (result == null) return false;
        XaeroOptionEntry entry = optionMap.get(result.getId());
        if (entry == null) return false;

        String currentVal = result.getValue() != null ? result.getValue().trim() : "";
        String clsName = entry.optionObj.getClass().getSimpleName();

        GlobalSettingEditScreen.SettingType type = null;
        if (clsName.contains("Boolean") || currentVal.equalsIgnoreCase("true") || currentVal.equalsIgnoreCase("false")) {
            type = GlobalSettingEditScreen.SettingType.BOOLEAN;
        } else if (clsName.contains("Range")) {
            type = GlobalSettingEditScreen.SettingType.INTEGER;
        } else if (clsName.contains("Stepped")) {
            type = GlobalSettingEditScreen.SettingType.DOUBLE;
        } else if (!entry.validChoices.isEmpty()) {
            type = GlobalSettingEditScreen.SettingType.CHOICE;
        }

        minecraft.gui.setScreen(new GlobalSettingEditScreen(
                screen,
                result,
                type,
                entry.validChoices,
                entry.defaultValue,
                newValStr -> {
                    Object parsed = parseXaeroValue(entry.optionObj, newValStr);
                    return setXaeroOptionValue(entry.channel, entry.optionObj, parsed);
                },
                onValueChanged
        ));
        return true;
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
