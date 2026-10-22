package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.Constants;
import com.igxxd.globalsearch.platform.Services;
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MalilibConfigIndexProvider implements SearchIndexProvider {

    private static class MalilibConfigEntry {
        final Object config;
        final String modId;
        final String defaultValueStr;

        MalilibConfigEntry(Object config, String modId, String defaultValueStr) {
            this.config = config;
            this.modId = modId;
            this.defaultValueStr = defaultValueStr != null ? defaultValueStr : "";
        }
    }

    private final Map<String, MalilibConfigEntry> malilibConfigs = new HashMap<>();

    @Override
    public String getName() {
        return "Malilib Mods";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        malilibConfigs.clear();
        // 1. Hotkeys from InputEventHandler.getKeybindManager()
        try {
            Class<?> inputEventHandlerCls = Class.forName("fi.dy.masa.malilib.event.InputEventHandler");
            Method getKeybindManagerM = inputEventHandlerCls.getMethod("getKeybindManager");
            Object keybindManager = getKeybindManagerM.invoke(null);
            if (keybindManager != null) {
                try {
                    Method updateKeysM = keybindManager.getClass().getMethod("updateUsedKeys");
                    updateKeysM.invoke(keybindManager);
                } catch (Throwable ignored) {}

                Method getCategoriesM = keybindManager.getClass().getMethod("getKeybindCategories");
                List<?> categories = (List<?>) getCategoriesM.invoke(keybindManager);
                if (categories != null) {
                    for (Object cat : categories) {
                        try {
                            Method getModNameM = cat.getClass().getMethod("getModName");
                            Method getCategoryM = cat.getClass().getMethod("getCategory");
                            Method getHotkeysM = cat.getClass().getMethod("getHotkeys");

                            String rawModName = (String) getModNameM.invoke(cat);
                            String rawCat = (String) getCategoryM.invoke(cat);
                            List<?> hotkeys = (List<?>) getHotkeysM.invoke(cat);

                            String modId = rawModName.toLowerCase(Locale.ROOT).replace(" ", "");
                            String modName = Services.PLATFORM.getModName(modId);
                            String category = resolveTranslatedCategory(modId, rawCat);

                            if (hotkeys != null) {
                                for (Object hk : hotkeys) {
                                    addGenericMalilibHotkey(hk, modId, modName, category, results, seenKeys);
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable t) {
            Constants.LOG.debug("Malilib InputEventHandler scan skipped: {}", t.getMessage());
        }

        // 2. Generic scan across all registered IConfigHandler implementations in ConfigManager
        try {
            Class<?> configManagerCls = Class.forName("fi.dy.masa.malilib.config.ConfigManager");
            Method getInstanceM = configManagerCls.getMethod("getInstance");
            Object configManager = getInstanceM.invoke(null);
            if (configManager != null) {
                Field handlersField = configManager.getClass().getDeclaredField("configHandlers");
                handlersField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, Object> handlers = (Map<String, Object>) handlersField.get(configManager);

                if (handlers != null) {
                    for (Map.Entry<String, Object> entry : handlers.entrySet()) {
                        String modId = entry.getKey();
                        Object handler = entry.getValue();
                        if (handler == null) continue;

                        String modName = Services.PLATFORM.getModName(modId);
                        Class<?> handlerClass = handler.getClass();

                        Class<?>[] innerClasses = handlerClass.getDeclaredClasses();
                        if (innerClasses.length > 0) {
                            for (Class<?> innerCls : innerClasses) {
                                String rawCat = innerCls.getSimpleName();
                                String category = resolveTranslatedCategory(modId, rawCat);
                                scanMalilibOptionsFromClass(innerCls, modId, modName, category, results, seenKeys);
                            }
                        } else {
                            scanMalilibOptionsFromClass(handlerClass, modId, modName, "General Settings", results, seenKeys);
                        }
                    }
                }
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable t) {
            Constants.LOG.debug("Malilib ConfigManager scan skipped: {}", t.getMessage());
        }
    }

    private void scanMalilibOptionsFromClass(Class<?> cls, String modId, String modName, String category, List<SearchResult> results, Set<String> seenKeys) {
        for (Field field : cls.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;

            try {
                Object val = field.get(null);
                if (val instanceof Iterable<?> iterable) {
                    for (Object opt : iterable) {
                        addGenericMalilibConfig(opt, modId, modName, category, results, seenKeys);
                    }
                } else if (val != null && val.getClass().isArray()) {
                    int len = java.lang.reflect.Array.getLength(val);
                    for (int i = 0; i < len; i++) {
                        addGenericMalilibConfig(java.lang.reflect.Array.get(val, i), modId, modName, category, results, seenKeys);
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    private String resolveTranslatedCategory(String modId, String categoryRaw) {
        if (categoryRaw == null || categoryRaw.isEmpty()) return "General Settings";
        String lower = categoryRaw.toLowerCase(Locale.ROOT);

        String[] candidateKeys = new String[] {
                modId + ".gui.tab." + lower,
                modId + ".gui.button.config_gui." + lower,
                modId + ".config.tab." + lower,
                modId + ".config.category." + lower,
                modId + ".category." + lower,
                "category." + modId + "." + lower
        };

        for (String candidate : candidateKeys) {
            Component comp = Component.translatable(candidate);
            String translated = comp.getString();
            if (!translated.equals(candidate) && !translated.isEmpty()) {
                return translated.endsWith("Settings") ? translated : (translated + " Settings");
            }
        }

        return ProviderUtils.formatCamelCase(categoryRaw) + " Settings";
    }

    private void addGenericMalilibHotkey(Object hk, String modId, String modName, String category, List<SearchResult> results, Set<String> seenKeys) {
        if (hk == null) return;
        try {
            Method getNameM = hk.getClass().getMethod("getName");
            String name = (String) getNameM.invoke(hk);
            if (name == null || name.isEmpty()) return;

            String key = SearchResultType.KEYBIND.name() + ":" + modId + ":" + name.toLowerCase(Locale.ROOT);
            if (!seenKeys.add(key)) return;

            String title = "";
            try {
                Method getTranslatedNameM = hk.getClass().getMethod("getTranslatedName");
                Object t = getTranslatedNameM.invoke(hk);
                if (t instanceof String s && !s.isEmpty() && !s.equals(name)) {
                    title = s;
                }
            } catch (Throwable ignored) {}

            if (title.isEmpty()) {
                try {
                    Method getGuiNameM = hk.getClass().getMethod("getConfigGuiDisplayName");
                    Object t = getGuiNameM.invoke(hk);
                    if (t instanceof String s && !s.isEmpty() && !s.equals(name)) {
                        title = s;
                    }
                } catch (Throwable ignored) {}
            }

            if (title.isEmpty()) {
                title = ProviderUtils.formatCamelCase(name);
            }

            String comment = "";
            try {
                Method getCommentM = hk.getClass().getMethod("getComment");
                Object c = getCommentM.invoke(hk);
                if (c instanceof String s) comment = s;
            } catch (Throwable ignored) {}
            try {
                Method getCommentCompM = hk.getClass().getMethod("getCommentComponent");
                Object comp = getCommentCompM.invoke(hk);
                if (comp instanceof Component c) {
                    String str = c.getString();
                    if (!str.isEmpty()) comment = str;
                }
            } catch (Throwable ignored) {}

            String resolvedDesc = resolveMalilibDescription(modId, category, name, comment);

            String keyVal = "";
            try {
                Method getStringValueM = hk.getClass().getMethod("getStringValue");
                Object val = getStringValueM.invoke(hk);
                if (val instanceof String s) keyVal = s;
            } catch (Throwable ignored) {}

            String path = modName + " > " + category;
            String id = modId + ":hotkey:" + name;

            results.add(new SearchResult(
                    SearchResultType.KEYBIND,
                    id,
                    title,
                    path,
                    keyVal,
                    modId,
                    modName,
                    ItemStack.EMPTY,
                    resolvedDesc.isEmpty() ? (title + " (" + path + ")") : resolvedDesc
            ));
        } catch (Throwable ignored) {}
    }

    private void addGenericMalilibConfig(Object config, String modId, String modName, String category, List<SearchResult> results, Set<String> seenKeys) {
        if (config == null) return;
        try {
            Method getNameM = config.getClass().getMethod("getName");
            String name = (String) getNameM.invoke(config);
            if (name == null || name.isEmpty()) return;

            String title = "";
            try {
                Method getTranslatedNameM = config.getClass().getMethod("getTranslatedName");
                Object t = getTranslatedNameM.invoke(config);
                if (t instanceof String s && !s.isEmpty() && !s.equals(name)) {
                    title = s;
                }
            } catch (Throwable ignored) {}

            if (title.isEmpty()) {
                try {
                    Method getGuiNameM = config.getClass().getMethod("getConfigGuiDisplayName");
                    Object t = getGuiNameM.invoke(config);
                    if (t instanceof String s && !s.isEmpty() && !s.equals(name)) {
                        title = s;
                    }
                } catch (Throwable ignored) {}
            }

            if (title.isEmpty()) {
                title = ProviderUtils.formatCamelCase(name);
            }

            String comment = "";
            try {
                Method getCommentM = config.getClass().getMethod("getComment");
                Object c = getCommentM.invoke(config);
                if (c instanceof String s) comment = s;
            } catch (Throwable ignored) {}
            try {
                Method getCommentCompM = config.getClass().getMethod("getCommentComponent");
                Object comp = getCommentCompM.invoke(config);
                if (comp instanceof Component c) {
                    String str = c.getString();
                    if (!str.isEmpty()) comment = str;
                }
            } catch (Throwable ignored) {}

            String resolvedDesc = resolveMalilibDescription(modId, category, name, comment);

            String val = "";
            try {
                Method getJsonM = config.getClass().getMethod("getAsJsonElement");
                Object json = getJsonM.invoke(config);
                if (json != null) {
                    val = json.toString().replace("\"", "");
                }
            } catch (Throwable ignored) {}

            String path = modName + " > " + category;
            String id = modId + ":config:" + name;
            String key = SearchResultType.SETTING.name() + ":" + id.toLowerCase(Locale.ROOT);

            String defValStr = "";
            try {
                Method getDefM = config.getClass().getMethod("getDefaultAsString");
                Object defObj = getDefM.invoke(config);
                if (defObj != null) {
                    defValStr = String.valueOf(defObj);
                }
            } catch (Throwable ignored) {}
            if (defValStr.isEmpty()) {
                defValStr = val;
            }

            if (seenKeys.add(key)) {
                malilibConfigs.put(id, new MalilibConfigEntry(config, modId, defValStr));

                results.add(SearchResult.builder(SearchResultType.SETTING, id, title)
                        .path(path)
                        .value(val)
                        .defaultValue(defValStr)
                        .modId(modId)
                        .modName(modName)
                        .description(resolvedDesc.isEmpty() ? (title + " (" + path + ")") : resolvedDesc)
                        .editable(true)
                        .provider(this)
                        .build()
                );
            }
        } catch (Throwable ignored) {}
    }

    private String resolveMalilibDescription(String modId, String category, String name, String rawComment) {
        Language lang = Language.getInstance();
        if (rawComment != null && !rawComment.isEmpty()) {
            if (lang.has(rawComment)) {
                return lang.getOrDefault(rawComment);
            }
            String trans = Component.translatable(rawComment).getString();
            if (!trans.equals(rawComment) && !trans.isEmpty()) {
                return trans;
            }
        }

        String lowerCat = category != null ? category.toLowerCase(Locale.ROOT).replace(" settings", "").replace(" ", "_") : "";
        String[] candidates = new String[] {
                modId + ".config." + lowerCat + ".comment." + name,
                modId + ".config.comment." + name,
                modId + ".config." + name + ".comment",
                modId + ".config." + name + ".tooltip",
                modId + ".hotkey.comment." + name,
                modId + ".comment." + name,
                modId + ".tooltip." + name,
                modId + "." + name + ".desc"
        };
        for (String cand : candidates) {
            if (lang.has(cand)) {
                return lang.getOrDefault(cand);
            }
            String trans = Component.translatable(cand).getString();
            if (!trans.equals(cand) && !trans.isEmpty()) {
                return trans;
            }
        }
        return rawComment != null ? rawComment : "";
    }

    @Override
    public boolean canEdit(SearchResult result) {
        return result != null && malilibConfigs.containsKey(result.getId());
    }

    @Override
    public boolean handleEdit(Minecraft minecraft, SearchResult result, net.minecraft.client.gui.screens.Screen screen, java.util.function.Consumer<String> onValueChanged) {
        if (result == null) return false;
        MalilibConfigEntry entry = malilibConfigs.get(result.getId());
        if (entry == null) return false;

        String currentVal = result.getValue() != null ? result.getValue().trim() : "";

        com.igxxd.globalsearch.gui.GlobalSettingEditScreen.SettingType settingType = null;
        if ("true".equalsIgnoreCase(currentVal) || "false".equalsIgnoreCase(currentVal)) {
            settingType = com.igxxd.globalsearch.gui.GlobalSettingEditScreen.SettingType.BOOLEAN;
        }

        // Open GlobalSettingEditScreen for all settings
        minecraft.gui.setScreen(new com.igxxd.globalsearch.gui.GlobalSettingEditScreen(
                screen,
                result,
                settingType,
                java.util.List.of(),
                entry.defaultValueStr,
                newValStr -> setMalilibValue(entry, newValStr),
                onValueChanged
        ));
        return true;
    }

    private boolean setMalilibValue(MalilibConfigEntry entry, String newValStr) {
        try {
            Method setValM = entry.config.getClass().getMethod("setValueFromString", String.class);
            setValM.invoke(entry.config, newValStr);

            // Save mod config
            Class<?> cmCls = Class.forName("fi.dy.masa.malilib.config.ConfigManager");
            Object cm = cmCls.getMethod("getInstance").invoke(null);
            Object handler = cm.getClass().getMethod("getConfigHandler", String.class).invoke(cm, entry.modId);
            if (handler != null) {
                handler.getClass().getMethod("save").invoke(handler);
            }
            return true;
        } catch (Throwable t) {
            Constants.LOG.error("Failed to set Malilib option: {}", t.getMessage());
            return false;
        }
    }
}
