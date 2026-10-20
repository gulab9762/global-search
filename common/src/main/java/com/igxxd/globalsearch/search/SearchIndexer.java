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

import java.lang.reflect.Method;
import java.util.*;

public class SearchIndexer {

    public static List<SearchResult> indexAll(Minecraft minecraft) {
        List<SearchResult> results = new ArrayList<>();
        if (minecraft == null) {
            return results;
        }

        try {
            indexKeybinds(minecraft, results);
        } catch (Exception e) {
            Constants.LOG.error("Failed to index keybinds", e);
        }

        try {
            indexSettings(minecraft, results);
        } catch (Exception e) {
            Constants.LOG.error("Failed to index settings", e);
        }

        try {
            indexItems(results);
        } catch (Exception e) {
            Constants.LOG.error("Failed to index items", e);
        }

        try {
            indexMods(results);
        } catch (Exception e) {
            Constants.LOG.error("Failed to index mods", e);
        }

        Constants.LOG.info("Indexed {} total global search entries.", results.size());
        return results;
    }

    private static void indexKeybinds(Minecraft minecraft, List<SearchResult> results) {
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

            results.add(new SearchResult(
                    SearchResultType.KEYBIND,
                    translationKey,
                    title,
                    path,
                    boundKey,
                    modId,
                    modName,
                    ItemStack.EMPTY
            ));
        }
    }

    private static void indexSettings(Minecraft minecraft, List<SearchResult> results) {
        Options options = minecraft.options;
        if (options == null) return;

        // Core known options
        addSetting(results, "Field of View (FOV)", "Options > Video", String.valueOf(options.fov().get()));
        addSetting(results, "Brightness", "Options > Video", String.format(Locale.ROOT, "%.0f%%", options.gamma().get() * 100));
        addSetting(results, "Render Distance", "Options > Video", options.getEffectiveRenderDistance() + " chunks");
        addSetting(results, "Simulation Distance", "Options > Video", options.simulationDistance().get() + " chunks");
        addSetting(results, "GUI Scale", "Options > Video", options.guiScale().get() == 0 ? "Auto" : String.valueOf(options.guiScale().get()));
        addSetting(results, "Max Framerate", "Options > Video", options.framerateLimit().get() >= 260 ? "Unlimited" : options.framerateLimit().get() + " fps");
        addSetting(results, "VSync", "Options > Video", options.enableVsync().get() ? "ON" : "OFF");
        addSetting(results, "Fullscreen", "Options > Video", options.fullscreen().get() ? "ON" : "OFF");
        addSetting(results, "View Bobbing", "Options > Video", options.bobView().get() ? "ON" : "OFF");
        addSetting(results, "Auto-Jump", "Options > Controls", options.autoJump().get() ? "ON" : "OFF");
        addSetting(results, "Mouse Sensitivity", "Options > Controls", String.format(Locale.ROOT, "%.0f%%", options.sensitivity().get() * 100));
        addSetting(results, "Invert Mouse", "Options > Controls", options.invertMouseY().get() ? "ON" : "OFF");
        addSetting(results, "Particles", "Options > Video", String.valueOf(options.particles().get()));
        addSetting(results, "High Contrast", "Options > Accessibility", options.highContrast().get() ? "ON" : "OFF");
        addSetting(results, "Show Subtitles", "Options > Music & Sounds", options.showSubtitles().get() ? "ON" : "OFF");

        // Sound sources
        for (SoundSource source : SoundSource.values()) {
            String name = formatCamelCase(source.getName()) + " Volume";
            String value = String.format(Locale.ROOT, "%.0f%%", options.getSoundSourceVolume(source) * 100);
            addSetting(results, name, "Options > Music & Sounds", value);
        }

        // Introspect other OptionInstance methods
        Set<String> indexedNames = new HashSet<>();
        for (SearchResult r : results) {
            indexedNames.add(r.getTitle().toLowerCase(Locale.ROOT));
        }

        for (Method method : Options.class.getMethods()) {
            if (OptionInstance.class.isAssignableFrom(method.getReturnType()) && method.getParameterCount() == 0) {
                try {
                    String methodName = method.getName();
                    String readableName = formatCamelCase(methodName);
                    if (!indexedNames.contains(readableName.toLowerCase(Locale.ROOT))) {
                        OptionInstance<?> instance = (OptionInstance<?>) method.invoke(options);
                        if (instance != null && instance.get() != null) {
                            addSetting(results, readableName, "Options > General", String.valueOf(instance.get()));
                            indexedNames.add(readableName.toLowerCase(Locale.ROOT));
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void addSetting(List<SearchResult> results, String title, String path, String value) {
        results.add(new SearchResult(
                SearchResultType.SETTING,
                "setting:" + title.toLowerCase(Locale.ROOT).replace(' ', '_'),
                title,
                path,
                value,
                "minecraft",
                "Minecraft",
                ItemStack.EMPTY
        ));
    }

    private static void indexItems(List<SearchResult> results) {
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) continue;

            ItemStack stack = new ItemStack(item);
            String title = stack.getHoverName().getString();
            String modId = id.getNamespace();
            String modName = Services.PLATFORM.getModName(modId);
            String value = "Stack Size: " + stack.getMaxStackSize();
            String path = id.toString();

            results.add(new SearchResult(
                    SearchResultType.ITEM,
                    path,
                    title,
                    path,
                    value,
                    modId,
                    modName,
                    stack
            ));
        }
    }

    private static void indexMods(List<SearchResult> results) {
        Map<String, String> mods = Services.PLATFORM.getLoadedMods();
        for (Map.Entry<String, String> entry : mods.entrySet()) {
            String modId = entry.getKey();
            String modName = entry.getValue();

            results.add(new SearchResult(
                    SearchResultType.MOD,
                    "mod:" + modId,
                    modName,
                    "Mod ID: " + modId,
                    "Loaded",
                    modId,
                    modName,
                    ItemStack.EMPTY
            ));
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
            } else if (c == '_') {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
