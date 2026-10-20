package com.igxxd.globalsearch.search;

import com.igxxd.globalsearch.Constants;
import com.igxxd.globalsearch.platform.Services;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class SearchIndexer {

    private static volatile List<SearchResult> CACHED_RESULTS = null;

    public static synchronized List<SearchResult> getOrIndex(Minecraft minecraft) {
        if (CACHED_RESULTS == null) {
            CACHED_RESULTS = indexAll(minecraft);
        }
        return CACHED_RESULTS;
    }

    public static synchronized int rescan(Minecraft minecraft) {
        CACHED_RESULTS = indexAll(minecraft);
        return CACHED_RESULTS.size();
    }

    public static List<SearchResult> indexAll(Minecraft minecraft) {
        List<SearchResult> results = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        if (minecraft == null) {
            return results;
        }

        try {
            indexKeybinds(minecraft, results, seenKeys);
        } catch (Exception e) {
            Constants.LOG.error("Failed to index keybinds", e);
        }

        try {
            indexSettings(minecraft, results, seenKeys);
        } catch (Exception e) {
            Constants.LOG.error("Failed to index settings", e);
        }

        try {
            indexItems(results, seenKeys);
        } catch (Exception e) {
            Constants.LOG.error("Failed to index items", e);
        }

        try {
            indexMods(results, seenKeys);
        } catch (Exception e) {
            Constants.LOG.error("Failed to index mods", e);
        }

        try {
            indexMalilibAndLitematica(minecraft, results, seenKeys);
        } catch (Exception e) {
            Constants.LOG.error("Failed to index Malilib/Litematica configs", e);
        }

        try {
            indexClothConfig(results, seenKeys);
        } catch (Exception e) {
            Constants.LOG.debug("Cloth config scanner finished with message", e);
        }

        try {
            indexNeoForgeConfigs(results, seenKeys);
        } catch (Exception e) {
            Constants.LOG.debug("NeoForge config scanner finished with message", e);
        }

        Constants.LOG.info("Indexed {} total global search entries.", results.size());
        CACHED_RESULTS = Collections.unmodifiableList(results);
        return CACHED_RESULTS;
    }

    private static void indexKeybinds(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        if (minecraft.options == null || minecraft.options.keyMappings == null) {
            return;
        }

        for (KeyMapping km : minecraft.options.keyMappings) {
            if (km == null) continue;

            String translationKey = km.getName();
            String title = Component.translatable(translationKey).getString();
            String category = km.getCategory() != null ? km.getCategory().label().getString() : "Miscellaneous";
            String boundKey = km.getTranslatedKeyMessage().getString();

            // Detect mod ID
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
            String path = "Controls > " + category;
            String key = SearchResultType.KEYBIND.name() + ":" + translationKey.toLowerCase(Locale.ROOT);

            if (seenKeys.add(key)) {
                results.add(new SearchResult(
                        SearchResultType.KEYBIND,
                        translationKey,
                        title,
                        path,
                        boundKey,
                        modId,
                        modName,
                        ItemStack.EMPTY,
                        "Keybind: " + title
                ));
            }
        }
    }

    private static void indexSettings(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        Options options = minecraft.options;
        if (options == null) return;

        // Core known options
        addSetting(results, seenKeys, "Field of View (FOV)", "Options > Video", String.valueOf(options.fov().get()));
        addSetting(results, seenKeys, "Brightness", "Options > Video", String.format(Locale.ROOT, "%.0f%%", options.gamma().get() * 100));
        addSetting(results, seenKeys, "Render Distance", "Options > Video", options.getEffectiveRenderDistance() + " chunks");
        addSetting(results, seenKeys, "Simulation Distance", "Options > Video", options.simulationDistance().get() + " chunks");
        addSetting(results, seenKeys, "GUI Scale", "Options > Video", options.guiScale().get() == 0 ? "Auto" : String.valueOf(options.guiScale().get()));
        addSetting(results, seenKeys, "Max Framerate", "Options > Video", options.framerateLimit().get() >= 260 ? "Unlimited" : options.framerateLimit().get() + " fps");
        addSetting(results, seenKeys, "VSync", "Options > Video", options.enableVsync().get() ? "ON" : "OFF");
        addSetting(results, seenKeys, "Fullscreen", "Options > Video", options.fullscreen().get() ? "ON" : "OFF");
        addSetting(results, seenKeys, "View Bobbing", "Options > Video", options.bobView().get() ? "ON" : "OFF");
        addSetting(results, seenKeys, "Auto-Jump", "Options > Controls", options.autoJump().get() ? "ON" : "OFF");
        addSetting(results, seenKeys, "Mouse Sensitivity", "Options > Controls", String.format(Locale.ROOT, "%.0f%%", options.sensitivity().get() * 100));
        addSetting(results, seenKeys, "Invert Mouse", "Options > Controls", options.invertMouseY().get() ? "ON" : "OFF");
        addSetting(results, seenKeys, "Particles", "Options > Video", String.valueOf(options.particles().get()));
        addSetting(results, seenKeys, "High Contrast", "Options > Accessibility", options.highContrast().get() ? "ON" : "OFF");
        addSetting(results, seenKeys, "Show Subtitles", "Options > Music & Sounds", options.showSubtitles().get() ? "ON" : "OFF");

        // Sound sources
        for (SoundSource source : SoundSource.values()) {
            String name = formatCamelCase(source.getName()) + " Volume";
            String value = String.format(Locale.ROOT, "%.0f%%", options.getSoundSourceVolume(source) * 100);
            addSetting(results, seenKeys, name, "Options > Music & Sounds", value);
        }

        // Introspect other OptionInstance methods
        for (Method method : Options.class.getMethods()) {
            if (OptionInstance.class.isAssignableFrom(method.getReturnType()) && method.getParameterCount() == 0) {
                try {
                    String methodName = method.getName();
                    String readableName = formatCamelCase(methodName);
                    OptionInstance<?> instance = (OptionInstance<?>) method.invoke(options);
                    if (instance != null && instance.get() != null) {
                        addSetting(results, seenKeys, readableName, "Options > General", String.valueOf(instance.get()));
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void addSetting(List<SearchResult> results, Set<String> seenKeys, String title, String path, String value) {
        String id = "setting:" + title.toLowerCase(Locale.ROOT).replace(' ', '_');
        String key = SearchResultType.SETTING.name() + ":" + id;
        if (seenKeys.add(key)) {
            results.add(new SearchResult(
                    SearchResultType.SETTING,
                    id,
                    title,
                    path,
                    value,
                    "minecraft",
                    "Minecraft",
                    ItemStack.EMPTY,
                    "Setting: " + title
            ));
        }
    }

    private static void indexItems(List<SearchResult> results, Set<String> seenKeys) {
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) continue;

            ItemStack stack = new ItemStack(item);
            String title = stack.getHoverName().getString();
            String modId = id.getNamespace();
            String modName = Services.PLATFORM.getModName(modId);
            String value = "Max: " + stack.getMaxStackSize();
            String path = id.toString();
            String key = SearchResultType.ITEM.name() + ":" + path.toLowerCase(Locale.ROOT);

            if (seenKeys.add(key)) {
                results.add(new SearchResult(
                        SearchResultType.ITEM,
                        path,
                        title,
                        path,
                        value,
                        modId,
                        modName,
                        stack,
                        "Item: " + title + " (" + path + ")"
                ));
            }
        }
    }

    private static void indexMods(List<SearchResult> results, Set<String> seenKeys) {
        Map<String, String> mods = Services.PLATFORM.getLoadedMods();
        for (Map.Entry<String, String> entry : mods.entrySet()) {
            String modId = entry.getKey();
            String modName = entry.getValue();
            String id = "mod:" + modId;
            String key = SearchResultType.MOD.name() + ":" + id.toLowerCase(Locale.ROOT);

            if (seenKeys.add(key)) {
                results.add(new SearchResult(
                        SearchResultType.MOD,
                        id,
                        modName,
                        "Mod ID: " + modId,
                        "Loaded",
                        modId,
                        modName,
                        ItemStack.EMPTY,
                        "Installed Mod: " + modName + " (" + modId + ")"
                ));
            }
        }
    }

    /**
     * Deep scanner for Malilib and mods utilizing Malilib (such as Litematica, Tweakeroo, MiniHUD, ItemScroller).
     */
    private static void indexMalilibAndLitematica(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
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

                            String modName = (String) getModNameM.invoke(cat);
                            String category = (String) getCategoryM.invoke(cat);
                            List<?> hotkeys = (List<?>) getHotkeysM.invoke(cat);

                            String modId = modName.toLowerCase(Locale.ROOT).replace(" ", "");

                            if (hotkeys != null) {
                                for (Object hk : hotkeys) {
                                    addMalilibHotkey(hk, modId, modName, category, results, seenKeys);
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable t) {
            Constants.LOG.warn("Malilib InputEventHandler scan skipped: {}", t.getMessage());
        }

        // 2. Config screens via Registry.CONFIG_SCREEN
        try {
            Class<?> registryCls = Class.forName("fi.dy.masa.malilib.registry.Registry");
            Field configScreenF = registryCls.getField("CONFIG_SCREEN");
            Object configScreenReg = configScreenF.get(null);
            if (configScreenReg != null) {
                Method getAllModsM = configScreenReg.getClass().getMethod("getAllModsWithConfigScreens");
                List<?> modInfos = (List<?>) getAllModsM.invoke(configScreenReg);
                if (modInfos != null) {
                    for (Object modInfo : modInfos) {
                        try {
                            Method modIdM = modInfo.getClass().getMethod("modId");
                            Method modNameM = modInfo.getClass().getMethod("modName");
                            Method supplierM = modInfo.getClass().getMethod("configScreenSupplier");

                            String modId = (String) modIdM.invoke(modInfo);
                            String modName = (String) modNameM.invoke(modInfo);
                            java.util.function.Supplier<?> supplier = (java.util.function.Supplier<?>) supplierM.invoke(modInfo);

                            if (supplier != null) {
                                Object gui = supplier.get();
                                if (gui != null) {
                                    Method getConfigsM = null;
                                    try {
                                        getConfigsM = gui.getClass().getMethod("getAllConfigs");
                                    } catch (NoSuchMethodException e) {
                                        try {
                                            getConfigsM = gui.getClass().getMethod("getConfigs");
                                        } catch (NoSuchMethodException ignored) {}
                                    }

                                    if (getConfigsM != null) {
                                        List<?> wrappers = (List<?>) getConfigsM.invoke(gui);
                                        if (wrappers != null) {
                                            for (Object wrapper : wrappers) {
                                                try {
                                                    Method getConfigM = wrapper.getClass().getMethod("getConfig");
                                                    Object config = getConfigM.invoke(wrapper);
                                                    if (config != null) {
                                                        addMalilibConfig(config, modId, modName, "Configs", results, seenKeys);
                                                    }
                                                } catch (Throwable ignored) {}
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable t) {
            Constants.LOG.warn("Malilib ConfigScreenRegistry scan skipped: {}", t.getMessage());
        }

        // 3. Direct inspection of Litematica static config & hotkey classes (fail-safe fallback)
        scanLitematicaStaticClasses(results, seenKeys);
    }

    private static void scanLitematicaStaticClasses(List<SearchResult> results, Set<String> seenKeys) {
        String modId = "litematica";
        String modName = Services.PLATFORM.getModName(modId);

        // Scan fi.dy.masa.litematica.config.Hotkeys
        try {
            Class<?> hotkeysCls = Class.forName("fi.dy.masa.litematica.config.Hotkeys");
            Field hotkeyListF = hotkeysCls.getField("HOTKEY_LIST");
            List<?> hotkeys = (List<?>) hotkeyListF.get(null);
            if (hotkeys != null) {
                for (Object hk : hotkeys) {
                    addMalilibHotkey(hk, modId, modName, "Hotkeys", results, seenKeys);
                }
            }
        } catch (Throwable ignored) {}

        // Scan Litematica Configs inner classes
        String[] configClasses = new String[]{
                "fi.dy.masa.litematica.config.Configs$Generic",
                "fi.dy.masa.litematica.config.Configs$Visuals",
                "fi.dy.masa.litematica.config.Configs$InfoOverlays",
                "fi.dy.masa.litematica.config.Configs$Colors"
        };
        for (String clsName : configClasses) {
            try {
                Class<?> cls = Class.forName(clsName);
                String category = cls.getSimpleName().replace("Configs$", "").replace("$", " > ");
                Field optionsF = cls.getField("OPTIONS");
                List<?> options = (List<?>) optionsF.get(null);
                if (options != null) {
                    for (Object opt : options) {
                        addMalilibConfig(opt, modId, modName, category, results, seenKeys);
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    private static void addMalilibHotkey(Object hk, String modId, String modName, String category, List<SearchResult> results, Set<String> seenKeys) {
        try {
            Method getNameM = hk.getClass().getMethod("getName");
            String name = (String) getNameM.invoke(hk);
            if (name == null || name.isEmpty()) return;

            String key = SearchResultType.KEYBIND.name() + ":" + modId + ":" + name.toLowerCase(Locale.ROOT);
            if (!seenKeys.add(key)) return;

            String comment = "";
            try {
                Method getCommentM = hk.getClass().getMethod("getComment");
                Object c = getCommentM.invoke(hk);
                if (c instanceof String s) comment = s;
            } catch (Throwable ignored) {}

            String keyVal = "";
            try {
                Method getStringValueM = hk.getClass().getMethod("getStringValue");
                Object val = getStringValueM.invoke(hk);
                if (val instanceof String s) keyVal = s;
            } catch (Throwable ignored) {}

            String title = formatCamelCase(name);
            // Enhanced alias matching for Litematica's Area Editor setting
            if (name.equalsIgnoreCase("openGuiAreaSettings")) {
                title = "Area Editor / Area Settings GUI";
                if (comment.isEmpty()) {
                    comment = "Open the Area Settings / Area Editor GUI for the currently selected area";
                } else if (!comment.toLowerCase(Locale.ROOT).contains("area editor")) {
                    comment = comment + " (Area Editor)";
                }
            }

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
                    comment
            ));
        } catch (Throwable ignored) {}
    }

    private static void addMalilibConfig(Object config, String modId, String modName, String category, List<SearchResult> results, Set<String> seenKeys) {
        try {
            Method getNameM = config.getClass().getMethod("getName");
            String name = (String) getNameM.invoke(config);
            if (name == null || name.isEmpty()) return;

            String comment = "";
            try {
                Method getCommentM = config.getClass().getMethod("getComment");
                Object c = getCommentM.invoke(config);
                if (c instanceof String s) comment = s;
            } catch (Throwable ignored) {}

            String val = "";
            try {
                Method getJsonM = config.getClass().getMethod("getAsJsonElement");
                Object json = getJsonM.invoke(config);
                if (json != null) {
                    val = json.toString().replace("\"", "");
                }
            } catch (Throwable ignored) {}

            boolean isHotkey = false;
            try {
                Class<?> hotkeyCls = Class.forName("fi.dy.masa.malilib.hotkeys.IHotkey");
                if (hotkeyCls.isInstance(config)) {
                    isHotkey = true;
                    try {
                        Method getStringValueM = config.getClass().getMethod("getStringValue");
                        Object v = getStringValueM.invoke(config);
                        if (v instanceof String s) val = s;
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            SearchResultType type = isHotkey ? SearchResultType.KEYBIND : SearchResultType.SETTING;
            String key = type.name() + ":" + modId + ":" + name.toLowerCase(Locale.ROOT);
            if (!seenKeys.add(key)) return;

            String title = formatCamelCase(name);
            if (name.equalsIgnoreCase("openGuiAreaSettings")) {
                title = "Area Editor / Area Settings GUI";
                if (comment.isEmpty()) {
                    comment = "Open the Area Settings / Area Editor GUI for the currently selected area";
                } else if (!comment.toLowerCase(Locale.ROOT).contains("area editor")) {
                    comment = comment + " (Area Editor)";
                }
            }

            String path = modName + " > " + category;
            String id = modId + ":config:" + name;

            results.add(new SearchResult(
                    type,
                    id,
                    title,
                    path,
                    val,
                    modId,
                    modName,
                    ItemStack.EMPTY,
                    comment
            ));
        } catch (Throwable ignored) {}
    }

    private static void indexClothConfig(List<SearchResult> results, Set<String> seenKeys) {
        try {
            Class<?> autoConfigCls = Class.forName("me.shedaniel.autoconfig.AutoConfig");
            Field holdersField = null;
            for (Field f : autoConfigCls.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    holdersField = f;
                    break;
                }
            }
            if (holdersField != null) {
                Map<?, ?> holders = (Map<?, ?>) holdersField.get(null);
                if (holders != null) {
                    for (Map.Entry<?, ?> entry : holders.entrySet()) {
                        Object holder = entry.getValue();
                        Method getConfigM = holder.getClass().getMethod("getConfig");
                        Object configObj = getConfigM.invoke(holder);
                        if (configObj != null) {
                            Class<?> configCls = configObj.getClass();
                            String modId = configCls.getSimpleName().toLowerCase(Locale.ROOT).replace("config", "");
                            if (modId.isEmpty()) modId = "mod";
                            String modName = Services.PLATFORM.getModName(modId);

                            for (Field cfgField : configCls.getDeclaredFields()) {
                                if (Modifier.isStatic(cfgField.getModifiers())) continue;
                                cfgField.setAccessible(true);
                                String fieldName = cfgField.getName();
                                Object val = cfgField.get(configObj);
                                String title = formatCamelCase(fieldName);
                                String path = modName + " > Config";
                                String id = modId + ":cloth:" + fieldName;

                                String key = SearchResultType.SETTING.name() + ":" + id.toLowerCase(Locale.ROOT);
                                if (seenKeys.add(key)) {
                                    results.add(new SearchResult(
                                            SearchResultType.SETTING,
                                            id,
                                            title,
                                            path,
                                            String.valueOf(val),
                                            modId,
                                            modName,
                                            ItemStack.EMPTY,
                                            "Cloth Config setting for " + modName
                                    ));
                                }
                            }
                        }
                    }
                }
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable t) {
            Constants.LOG.debug("Cloth config scanner skipped", t);
        }
    }

    private static void indexNeoForgeConfigs(List<SearchResult> results, Set<String> seenKeys) {
        try {
            Class<?> trackerCls = Class.forName("net.neoforged.fml.config.ConfigTracker");
            Field instF = trackerCls.getField("INSTANCE");
            Object tracker = instF.get(null);
            Method configSetsM = tracker.getClass().getMethod("configSets");
            Map<?, ?> sets = (Map<?, ?>) configSetsM.invoke(tracker);
            if (sets != null) {
                for (Object setObj : sets.values()) {
                    if (setObj instanceof Set<?> configSet) {
                        for (Object modCfg : configSet) {
                            Method getModIdM = modCfg.getClass().getMethod("getModId");
                            String modId = (String) getModIdM.invoke(modCfg);
                            String modName = Services.PLATFORM.getModName(modId);

                            Method getSpecM = modCfg.getClass().getMethod("getSpec");
                            Object spec = getSpecM.invoke(modCfg);
                            if (spec != null) {
                                Method isLoadedM = spec.getClass().getMethod("isLoaded");
                                boolean isLoaded = (Boolean) isLoadedM.invoke(spec);
                                if (!isLoaded) continue;

                                Method getValuesM = spec.getClass().getMethod("getValues");
                                Map<?, ?> values = (Map<?, ?>) getValuesM.invoke(spec);
                                if (values != null) {
                                    for (Map.Entry<?, ?> valEntry : values.entrySet()) {
                                        String pathKey = String.valueOf(valEntry.getKey());
                                        Object configValue = valEntry.getValue();
                                        Method getValM = configValue.getClass().getMethod("get");
                                        Object val = getValM.invoke(configValue);

                                        String title = pathKey;
                                        int lastDot = pathKey.lastIndexOf('.');
                                        if (lastDot >= 0 && lastDot < pathKey.length() - 1) {
                                            title = formatCamelCase(pathKey.substring(lastDot + 1));
                                        }

                                        String path = modName + " > " + pathKey;
                                        String id = modId + ":neoforge:" + pathKey;
                                        String key = SearchResultType.SETTING.name() + ":" + id.toLowerCase(Locale.ROOT);
                                        if (seenKeys.add(key)) {
                                            results.add(new SearchResult(
                                                    SearchResultType.SETTING,
                                                    id,
                                                    title,
                                                    path,
                                                    String.valueOf(val),
                                                    modId,
                                                    modName,
                                                    ItemStack.EMPTY,
                                                    "NeoForge Config (" + pathKey + ")"
                                            ));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable t) {
            Constants.LOG.debug("NeoForge config scanner skipped", t);
        }
    }

    private static String formatCamelCase(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(input.charAt(0)));
        for (int i = 1; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append(' ').append(c);
            } else if (c == '_' || c == '-') {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
