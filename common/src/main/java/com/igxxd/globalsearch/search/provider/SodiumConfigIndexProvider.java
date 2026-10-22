package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.Constants;
import com.igxxd.globalsearch.gui.GlobalSettingEditScreen;
import com.igxxd.globalsearch.platform.Services;
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

/**
 * Provider for indexing settings from Sodium and Iris (and any mod integrating into Sodium options).
 * Supports modern Sodium 0.6+ / 0.9+ Config architecture:
 * - Color Space (Iris)
 * - Shaders and Shadow Distance
 * - Performance (Culling, Threads, Occlusion)
 * - Quality, General, and Advanced settings
 */
public class SodiumConfigIndexProvider implements SearchIndexProvider {

    private static class SodiumOptionEntry {
        final Object configObj;
        final Object optionObj;
        final Identifier id;
        final Class<?> enumClass;
        final List<String> choices;
        final String defaultValueStr;

        SodiumOptionEntry(Object configObj, Object optionObj, Identifier id, Class<?> enumClass, List<String> choices, String defaultValueStr) {
            this.configObj = configObj;
            this.optionObj = optionObj;
            this.id = id;
            this.enumClass = enumClass;
            this.choices = choices;
            this.defaultValueStr = defaultValueStr != null ? defaultValueStr : "";
        }
    }

    private final Map<String, SodiumOptionEntry> optionMap = new HashMap<>();

    @Override
    public String getName() {
        return "Sodium & Iris";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        optionMap.clear();

        try {
            Class<?> configManagerCls = Class.forName("net.caffeinemc.mods.sodium.client.config.ConfigManager");
            Field configField = configManagerCls.getField("CONFIG");
            Object configObj = configField.get(null);
            if (configObj == null) return;

            Method getModOptionsM = configObj.getClass().getMethod("getModOptions");
            Object modOptionsListObj = getModOptionsM.invoke(configObj);
            if (!(modOptionsListObj instanceof List<?> modOptionsList)) return;

            Field optIdField = null;
            try {
                Class<?> optCls = Class.forName("net.caffeinemc.mods.sodium.client.config.structure.Option");
                optIdField = optCls.getDeclaredField("id");
                optIdField.setAccessible(true);
            } catch (Throwable ignored) {}

            for (Object modOpt : modOptionsList) {
                if (modOpt == null) continue;
                String modId = "";
                String modName = "";
                try {
                    Method configIdM = modOpt.getClass().getMethod("configId");
                    modId = (String) configIdM.invoke(modOpt);
                } catch (Throwable ignored) {}

                try {
                    Method nameM = modOpt.getClass().getMethod("name");
                    modName = (String) nameM.invoke(modOpt);
                } catch (Throwable ignored) {}

                if (modName.isEmpty()) {
                    modName = !modId.isEmpty() ? Services.PLATFORM.getModName(modId) : "Sodium";
                }
                if (modId.isEmpty()) {
                    modId = modName.toLowerCase(Locale.ROOT);
                }

                Method pagesM = modOpt.getClass().getMethod("pages");
                Object pagesObj = pagesM.invoke(modOpt);
                if (!(pagesObj instanceof List<?> pages)) continue;

                for (Object page : pages) {
                    if (page == null) continue;
                    String pageName = "";
                    try {
                        Method pageNameM = page.getClass().getMethod("name");
                        Object pageComp = pageNameM.invoke(page);
                        if (pageComp instanceof Component c) {
                            pageName = c.getString();
                        }
                    } catch (Throwable ignored) {}

                    if (pageName.isEmpty()) pageName = "Settings";

                    Method groupsM = page.getClass().getMethod("groups");
                    Object groupsObj = groupsM.invoke(page);
                    if (!(groupsObj instanceof List<?> groups)) continue;

                    for (Object group : groups) {
                        if (group == null) continue;
                        String groupName = "";
                        try {
                            Method groupNameM = group.getClass().getMethod("name");
                            Object grpComp = groupNameM.invoke(group);
                            if (grpComp instanceof Component c) {
                                groupName = c.getString();
                            }
                        } catch (Throwable ignored) {}

                        Method optionsM = group.getClass().getMethod("options");
                        Object optionsListObj = optionsM.invoke(group);
                        if (!(optionsListObj instanceof List<?> optionsList)) continue;

                        for (Object opt : optionsList) {
                            if (opt == null) continue;
                            try {
                                Method getNameM = opt.getClass().getMethod("getName");
                                Object nameComp = getNameM.invoke(opt);
                                String title = (nameComp instanceof Component c) ? c.getString() : "";
                                if (title.isEmpty()) continue;

                                Identifier id = null;
                                if (optIdField != null) {
                                    id = (Identifier) optIdField.get(opt);
                                }

                                String optIdStr = id != null ? id.toString() : (modId + ":" + pageName + ":" + title);

                                String tooltip = "";
                                try {
                                    Method getTooltipM = opt.getClass().getMethod("getTooltip");
                                    Object tipComp = getTooltipM.invoke(opt);
                                    if (tipComp instanceof Component c) {
                                        tooltip = c.getString();
                                    }
                                } catch (Throwable ignored) {}

                                String valStr = "";
                                Class<?> enumCls = null;
                                List<String> choices = new ArrayList<>();

                                try {
                                    Method getAppliedValueM = opt.getClass().getMethod("getAppliedValue");
                                    Object valObj = getAppliedValueM.invoke(opt);
                                    if (valObj != null) {
                                        if (valObj instanceof Enum<?> e) {
                                            valStr = e.name();
                                            enumCls = valObj.getClass();
                                            for (Object constant : enumCls.getEnumConstants()) {
                                                choices.add(((Enum<?>) constant).name());
                                            }
                                        } else {
                                            valStr = String.valueOf(valObj);
                                        }
                                    }
                                } catch (Throwable ignored) {}

                                if (enumCls == null) {
                                    try {
                                        Method getEnumClassM = opt.getClass().getMethod("getEnumClass");
                                        Object clsObj = getEnumClassM.invoke(opt);
                                        if (clsObj instanceof Class<?> c) {
                                            enumCls = c;
                                            for (Object constant : enumCls.getEnumConstants()) {
                                                choices.add(((Enum<?>) constant).name());
                                            }
                                        }
                                    } catch (Throwable ignored) {}
                                }

                                String path = groupName.isEmpty() || groupName.equalsIgnoreCase(pageName)
                                        ? (modName + " > " + pageName)
                                        : (modName + " > " + pageName + " > " + groupName);

                                String defValStr = "";
                                try {
                                    Method getDefaultValM = opt.getClass().getMethod("getDefaultValue");
                                    Object depVal = getDefaultValM.invoke(opt);
                                    if (depVal != null) {
                                        for (Method m : depVal.getClass().getMethods()) {
                                            if ("get".equals(m.getName()) && m.getParameterCount() == 1) {
                                                Object defObj = m.invoke(depVal, configObj);
                                                if (defObj != null) {
                                                    if (defObj instanceof Enum<?> e) {
                                                        defValStr = e.name();
                                                    } else {
                                                        defValStr = String.valueOf(defObj);
                                                    }
                                                }
                                                break;
                                            }
                                        }
                                    }
                                } catch (Throwable ignored) {}

                                if (defValStr.isEmpty()) {
                                    defValStr = valStr;
                                }

                                String uniqueKey = SearchResultType.SETTING.name() + ":" + modId + ":" + optIdStr.toLowerCase(Locale.ROOT);
                                if (seenKeys.add(uniqueKey)) {
                                    String desc = tooltip.isEmpty() ? (title + " in " + path) : tooltip;
                                    ItemStack icon = pickSodiumIcon(modId, pageName);

                                    optionMap.put(optIdStr, new SodiumOptionEntry(configObj, opt, id, enumCls, choices, defValStr));

                                    results.add(SearchResult.builder(SearchResultType.SETTING, optIdStr, title)
                                            .path(path)
                                            .value(valStr)
                                            .defaultValue(defValStr)
                                            .modId(modId)
                                            .modName(modName)
                                            .icon(icon)
                                            .description(desc)
                                            .editable(true)
                                            .provider(SodiumConfigIndexProvider.this)
                                            .build()
                                    );
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            }
        } catch (ClassNotFoundException ignored) {
            // Sodium not installed
        } catch (Throwable t) {
            Constants.LOG.debug("Could not scan Sodium / Iris options: {}", t.getMessage());
        }
    }

    private ItemStack pickSodiumIcon(String modId, String pageName) {
        if ("iris".equalsIgnoreCase(modId) || pageName.toLowerCase(Locale.ROOT).contains("shader")) {
            return new ItemStack(Items.AMETHYST_SHARD);
        }
        if (pageName.toLowerCase(Locale.ROOT).contains("performance")) {
            return new ItemStack(Items.REDSTONE);
        }
        if (pageName.toLowerCase(Locale.ROOT).contains("quality")) {
            return new ItemStack(Items.DIAMOND);
        }
        return new ItemStack(Items.COMPARATOR);
    }

    @Override
    public boolean canEdit(SearchResult result) {
        return result != null && optionMap.containsKey(result.getId());
    }

    @Override
    public boolean handleEdit(Minecraft minecraft, SearchResult result, Screen screen, Consumer<String> onValueChanged) {
        if (result == null) return false;
        SodiumOptionEntry entry = optionMap.get(result.getId());
        if (entry == null) return false;

        String currentVal = result.getValue() != null ? result.getValue().trim() : "";
        String clsName = entry.optionObj.getClass().getSimpleName();

        GlobalSettingEditScreen.SettingType type = null;
        if (clsName.contains("Boolean") || currentVal.equalsIgnoreCase("true") || currentVal.equalsIgnoreCase("false")) {
            type = GlobalSettingEditScreen.SettingType.BOOLEAN;
        } else if (clsName.contains("Integer")) {
            type = GlobalSettingEditScreen.SettingType.INTEGER;
        } else if (!entry.choices.isEmpty()) {
            type = GlobalSettingEditScreen.SettingType.CHOICE;
        }

        minecraft.gui.setScreen(new GlobalSettingEditScreen(
                screen,
                result,
                type,
                entry.choices,
                entry.defaultValueStr,
                newValStr -> setSodiumOptionValue(entry, newValStr),
                onValueChanged
        ));
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean setSodiumOptionValue(SodiumOptionEntry entry, String newValStr) {
        if (entry == null || entry.optionObj == null) return false;
        try {
            Object parsedValue = null;
            if (entry.enumClass != null) {
                for (Object c : entry.enumClass.getEnumConstants()) {
                    Enum<?> e = (Enum<?>) c;
                    if (e.name().equalsIgnoreCase(newValStr)) {
                        parsedValue = e;
                        break;
                    }
                }
            } else if (newValStr.equalsIgnoreCase("true") || newValStr.equalsIgnoreCase("false")) {
                parsedValue = Boolean.parseBoolean(newValStr);
            } else {
                try {
                    parsedValue = Integer.parseInt(newValStr);
                } catch (Exception e) {
                    parsedValue = newValStr;
                }
            }

            if (parsedValue != null) {
                for (Method m : entry.optionObj.getClass().getMethods()) {
                    if ("modifyValue".equals(m.getName()) && m.getParameterCount() == 1) {
                        m.invoke(entry.optionObj, parsedValue);
                        break;
                    }
                }

                // Traverse class hierarchy for applyChanges (declared in StatefulOption)
                Class<?> optCls = entry.optionObj.getClass();
                while (optCls != null && optCls != Object.class) {
                    try {
                        Method applyChangesM = optCls.getDeclaredMethod("applyChanges");
                        applyChangesM.setAccessible(true);
                        applyChangesM.invoke(entry.optionObj);
                        break;
                    } catch (NoSuchMethodException e) {
                        optCls = optCls.getSuperclass();
                    }
                }

                if (entry.configObj != null) {
                    if (entry.id != null) {
                        try {
                            Method applyOptM = entry.configObj.getClass().getMethod("applyOption", Identifier.class);
                            applyOptM.invoke(entry.configObj, entry.id);
                        } catch (Throwable ignored) {}
                    }
                    try {
                        Method applyAllM = entry.configObj.getClass().getMethod("applyAllOptions");
                        applyAllM.invoke(entry.configObj);
                    } catch (Throwable ignored) {}
                    try {
                        Method flushM = entry.configObj.getClass().getDeclaredMethod("flushStorageHandlers");
                        flushM.setAccessible(true);
                        flushM.invoke(entry.configObj);
                    } catch (Throwable ignored) {}
                }
                return true;
            }
        } catch (Throwable t) {
            Constants.LOG.error("Failed to set Sodium / Iris option: {}", t.getMessage());
        }
        return false;
    }
}
