package com.igxxd.globalsearch.search.provider;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.igxxd.globalsearch.Constants;
import com.igxxd.globalsearch.gui.GlobalSettingEditScreen;
import com.igxxd.globalsearch.platform.Services;
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/**
 * Universal fallback configuration indexer (Tier 3).
 * Automatically discovers, parses, and edits config files from .minecraft/config/:
 * - JSON and JSON5 (.json)
 * - TOML (.toml)
 * - Java Properties (.properties)
 *
 * Runs with low priority (20) so higher-tier runtime providers (Cloth Config, NeoForge,
 * Sodium/Iris, Vanilla) supersede these entries and avoid duplicate cards.
 */
public class GenericConfigFileIndexProvider implements SearchIndexProvider {

    public static class FileConfigTarget {
        final Path filePath;
        final String fileType; // "json", "toml", "properties"
        final String keyPath;   // e.g. "general.overworld_region_size" or "enabled.value"
        final String modId;
        final String modName;

        public FileConfigTarget(Path filePath, String fileType, String keyPath, String modId, String modName) {
            this.filePath = filePath;
            this.fileType = fileType;
            this.keyPath = keyPath;
            this.modId = modId;
            this.modName = modName;
        }
    }

    private final Map<String, FileConfigTarget> targetMap = new HashMap<>();

    @Override
    public String getName() {
        return "Config File Fallback";
    }

    @Override
    public int getPriority() {
        return 20; // Lowest tier: superseded by runtime mod & vanilla managers
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        targetMap.clear();

        Path configDir = null;
        try {
            if (minecraft != null && minecraft.gameDirectory != null) {
                configDir = minecraft.gameDirectory.toPath().resolve("config");
            }
        } catch (Throwable ignored) {}

        if (configDir == null || !Files.exists(configDir) || !Files.isDirectory(configDir)) {
            configDir = Path.of("config");
        }

        if (!Files.exists(configDir) || !Files.isDirectory(configDir)) {
            return;
        }

        try {
            scanDirectory(configDir, configDir, results, seenKeys, 0);
        } catch (Throwable t) {
            Constants.LOG.debug("Error while scanning config files: {}", t.getMessage());
        }
    }

    private void scanDirectory(Path baseDir, Path currentDir, List<SearchResult> results, Set<String> seenKeys, int depth) {
        if (depth > 4) return; // Prevent infinite loops or excessively deep folders

        File[] files = currentDir.toFile().listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(baseDir, file.toPath(), results, seenKeys, depth + 1);
            } else if (file.isFile()) {
                String name = file.getName().toLowerCase(Locale.ROOT);
                // Exclude known non-config or binary/huge files
                if (name.endsWith(".bak") || name.endsWith(".old") || name.endsWith(".tmp")
                        || name.startsWith(".") || name.contains("fingerprint") || name.contains("cache")) {
                    continue;
                }

                try {
                    if (name.endsWith(".json")) {
                        indexJsonFile(baseDir, file.toPath(), results, seenKeys);
                    } else if (name.endsWith(".toml")) {
                        indexTomlFile(baseDir, file.toPath(), results, seenKeys);
                    } else if (name.endsWith(".properties")) {
                        indexPropertiesFile(baseDir, file.toPath(), results, seenKeys);
                    }
                } catch (Throwable t) {
                    Constants.LOG.debug("Failed parsing config file {}: {}", file.getName(), t.getMessage());
                }
            }
        }
    }

    private String deriveModId(Path baseDir, Path file) {
        String fileName = file.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;

        // If file is nested in a directory like config/modid/common.json, use parent folder name
        Path parent = file.getParent();
        if (parent != null && !parent.equals(baseDir)) {
            String parentName = parent.getFileName().toString().toLowerCase(Locale.ROOT);
            if (!parentName.equals("config")) {
                return parentName;
            }
        }

        // Clean up common suffixes like -common, -client, -server, _config
        String modId = base.toLowerCase(Locale.ROOT)
                .replaceAll("(-common|-client|-server|_common|_client|_server|config|_config|-config)$", "");
        return modId.isEmpty() ? base.toLowerCase(Locale.ROOT) : modId;
    }

    // -------------------------------------------------------------
    // JSON PARSER
    // -------------------------------------------------------------
    private void indexJsonFile(Path baseDir, Path file, List<SearchResult> results, Set<String> seenKeys) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8).trim();
            if (content.isEmpty() || !content.startsWith("{")) return;

            // Strip // comments for JSON5 compatibility
            String cleaned = stripJsonComments(content);
            JsonElement root = JsonParser.parseString(cleaned);
            if (!root.isJsonObject()) return;

            String modId = deriveModId(baseDir, file);
            String modName = Services.PLATFORM.getModName(modId);
            String fileName = file.getFileName().toString();

            flattenJsonObject(root.getAsJsonObject(), "", (keyPath, valStr) -> {
                addEntry(file, "json", keyPath, valStr, modId, modName, fileName, results, seenKeys);
            });
        } catch (Throwable ignored) {}
    }

    private void flattenJsonObject(JsonObject obj, String prefix, java.util.function.BiConsumer<String, String> consumer) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("//") || key.equals("comment") || key.equals("description")) {
                continue;
            }
            JsonElement val = entry.getValue();
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (val.isJsonObject()) {
                JsonObject childObj = val.getAsJsonObject();
                // Check if this is an annotated wrapper like { "value": 10, "default": 5 }
                if (childObj.has("value") && childObj.get("value").isJsonPrimitive()) {
                    consumer.accept(fullKey, childObj.get("value").getAsString());
                } else {
                    flattenJsonObject(childObj, fullKey, consumer);
                }
            } else if (val.isJsonPrimitive()) {
                consumer.accept(fullKey, val.getAsString());
            }
        }
    }

    private String stripJsonComments(String json) {
        StringBuilder sb = new StringBuilder();
        String[] lines = json.split("\r?\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("//") || trimmed.startsWith("#")) {
                continue;
            }
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    // -------------------------------------------------------------
    // TOML PARSER
    // -------------------------------------------------------------
    private void indexTomlFile(Path baseDir, Path file, List<SearchResult> results, Set<String> seenKeys) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            String currentSection = "";
            String modId = deriveModId(baseDir, file);
            String modName = Services.PLATFORM.getModName(modId);
            String fileName = file.getFileName().toString();

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    currentSection = trimmed.substring(1, trimmed.length() - 1).trim();
                    continue;
                }

                int eqIdx = trimmed.indexOf('=');
                if (eqIdx > 0) {
                    String rawKey = trimmed.substring(0, eqIdx).trim();
                    String rawVal = trimmed.substring(eqIdx + 1).trim();

                    // Strip inline comment if any
                    int hashIdx = rawVal.indexOf('#');
                    if (hashIdx >= 0) {
                        rawVal = rawVal.substring(0, hashIdx).trim();
                    }
                    if (rawVal.startsWith("\"") && rawVal.endsWith("\"") && rawVal.length() >= 2) {
                        rawVal = rawVal.substring(1, rawVal.length() - 1);
                    }

                    String keyPath = currentSection.isEmpty() ? rawKey : currentSection + "." + rawKey;
                    addEntry(file, "toml", keyPath, rawVal, modId, modName, fileName, results, seenKeys);
                }
            }
        } catch (Throwable ignored) {}
    }

    // -------------------------------------------------------------
    // PROPERTIES PARSER
    // -------------------------------------------------------------
    private void indexPropertiesFile(Path baseDir, Path file, List<SearchResult> results, Set<String> seenKeys) {
        try (InputStream in = Files.newInputStream(file)) {
            Properties props = new Properties();
            props.load(in);

            String modId = deriveModId(baseDir, file);
            String modName = Services.PLATFORM.getModName(modId);
            String fileName = file.getFileName().toString();

            for (String key : props.stringPropertyNames()) {
                String val = props.getProperty(key);
                addEntry(file, "properties", key, val, modId, modName, fileName, results, seenKeys);
            }
        } catch (Throwable ignored) {}
    }

    // -------------------------------------------------------------
    // ENTRY GENERATOR & DEDUPLICATION
    // -------------------------------------------------------------
    private void addEntry(Path file, String fileType, String keyPath, String value,
                          String modId, String modName, String fileName,
                          List<SearchResult> results, Set<String> seenKeys) {
        if (keyPath == null || keyPath.isEmpty()) return;

        // Title from last segment: e.g. "overworld_region_size" -> "Overworld Region Size"
        String lastSegment = keyPath;
        String section = "";
        int dotIdx = keyPath.lastIndexOf('.');
        if (dotIdx >= 0) {
            section = keyPath.substring(0, dotIdx);
            lastSegment = keyPath.substring(dotIdx + 1);
        }

        String title = ProviderUtils.formatCamelCase(lastSegment);
        String path = modName + " > " + fileName + (section.isEmpty() ? "" : (" > " + section));
        String id = "cfgfile:" + modId + ":" + fileName + ":" + keyPath;

        String seenKey = SearchResultType.SETTING.name() + ":" + id.toLowerCase(Locale.ROOT);
        if (seenKeys.add(seenKey)) {
            targetMap.put(id, new FileConfigTarget(file, fileType, keyPath, modId, modName));

            String desc = "File Config (" + fileName + " > " + keyPath + ")";
            results.add(SearchResult.builder(SearchResultType.SETTING, id, title)
                    .path(path)
                    .value(value != null ? value : "")
                    .modId(modId)
                    .modName(modName)
                    .icon(new ItemStack(Items.WRITABLE_BOOK))
                    .description(desc)
                    .editable(true)
                    .provider(this)
                    .build());
        }
    }

    // -------------------------------------------------------------
    // EDIT & WRITE-BACK HANDLER
    // -------------------------------------------------------------
    @Override
    public boolean canEdit(SearchResult result) {
        return result != null && targetMap.containsKey(result.getId());
    }

    @Override
    public boolean handleEdit(Minecraft minecraft, SearchResult result, Screen screen, Consumer<String> onValueChanged) {
        FileConfigTarget target = targetMap.get(result.getId());
        if (target == null) return false;

        GlobalSettingEditScreen.SettingType detectedType = null;
        String rawVal = result.getValue() != null ? result.getValue().trim() : "";

        if (rawVal.equalsIgnoreCase("true") || rawVal.equalsIgnoreCase("false")) {
            detectedType = GlobalSettingEditScreen.SettingType.BOOLEAN;
        }

        minecraft.gui.setScreen(new GlobalSettingEditScreen(
                screen,
                result,
                detectedType,
                List.of(),
                rawVal,
                newVal -> {
                    boolean success = updateConfigFile(target, newVal);
                    if (success) {
                        result.setValue(newVal);
                        if (onValueChanged != null) {
                            onValueChanged.accept(newVal);
                        }
                    }
                    return success;
                },
                onValueChanged
        ));
        return true;
    }

    private boolean updateConfigFile(FileConfigTarget target, String newValue) {
        try {
            if ("properties".equalsIgnoreCase(target.fileType)) {
                return updateProperties(target.filePath, target.keyPath, newValue);
            } else if ("toml".equalsIgnoreCase(target.fileType)) {
                return updateToml(target.filePath, target.keyPath, newValue);
            } else if ("json".equalsIgnoreCase(target.fileType)) {
                return updateJson(target.filePath, target.keyPath, newValue);
            }
        } catch (Throwable t) {
            Constants.LOG.error("Failed writing config to {}: {}", target.filePath, t.getMessage());
        }
        return false;
    }

    private boolean updateProperties(Path path, String key, String newVal) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        boolean found = false;
        List<String> newLines = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("#") && trimmed.contains("=")) {
                int eq = trimmed.indexOf('=');
                String k = trimmed.substring(0, eq).trim();
                if (k.equals(key)) {
                    newLines.add(key + "=" + newVal);
                    found = true;
                    continue;
                }
            }
            newLines.add(line);
        }

        if (!found) {
            newLines.add(key + "=" + newVal);
        }

        Files.write(path, newLines, StandardCharsets.UTF_8);
        return true;
    }

    private boolean updateToml(Path path, String fullKey, String newVal) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        String targetSection = "";
        String targetKey = fullKey;
        int dot = fullKey.lastIndexOf('.');
        if (dot >= 0) {
            targetSection = fullKey.substring(0, dot);
            targetKey = fullKey.substring(dot + 1);
        }

        List<String> newLines = new ArrayList<>();
        String currentSection = "";
        boolean updated = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                currentSection = trimmed.substring(1, trimmed.length() - 1).trim();
            }

            if (currentSection.equals(targetSection) && !trimmed.startsWith("#") && trimmed.contains("=")) {
                int eq = trimmed.indexOf('=');
                String k = trimmed.substring(0, eq).trim();
                if (k.equals(targetKey)) {
                    // Retain format (number/boolean unquoted, string quoted)
                    String formattedVal = newVal;
                    if (!newVal.equalsIgnoreCase("true") && !newVal.equalsIgnoreCase("false")
                            && !newVal.matches("^-?\\d+(\\.\\d+)?$") && !newVal.startsWith("\"")) {
                        formattedVal = "\"" + newVal + "\"";
                    }
                    newLines.add("\t" + targetKey + " = " + formattedVal);
                    updated = true;
                    continue;
                }
            }
            newLines.add(line);
        }

        if (updated) {
            Files.write(path, newLines, StandardCharsets.UTF_8);
            return true;
        }
        return false;
    }

    private boolean updateJson(Path path, String fullKey, String newVal) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        JsonElement root = JsonParser.parseString(stripJsonComments(content));
        if (!root.isJsonObject()) return false;

        String[] parts = fullKey.split("\\.");
        JsonObject current = root.getAsJsonObject();
        for (int i = 0; i < parts.length - 1; i++) {
            if (current.has(parts[i]) && current.get(parts[i]).isJsonObject()) {
                current = current.getAsJsonObject(parts[i]);
            } else {
                return false;
            }
        }

        String lastKey = parts[parts.length - 1];
        if (current.has(lastKey)) {
            JsonElement el = current.get(lastKey);
            if (el.isJsonObject() && el.getAsJsonObject().has("value")) {
                current = el.getAsJsonObject();
                lastKey = "value";
            }
        }

        if (newVal.equalsIgnoreCase("true") || newVal.equalsIgnoreCase("false")) {
            current.addProperty(lastKey, Boolean.parseBoolean(newVal));
        } else if (newVal.matches("^-?\\d+$")) {
            current.addProperty(lastKey, Long.parseLong(newVal));
        } else if (newVal.matches("^-?\\d+\\.\\d+$")) {
            current.addProperty(lastKey, Double.parseDouble(newVal));
        } else {
            current.addProperty(lastKey, newVal);
        }

        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        Files.writeString(path, gson.toJson(root), StandardCharsets.UTF_8);
        return true;
    }
}
