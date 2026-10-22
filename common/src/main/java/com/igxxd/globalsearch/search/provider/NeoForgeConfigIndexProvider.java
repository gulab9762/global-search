package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.Constants;
import com.igxxd.globalsearch.gui.GlobalSettingEditScreen;
import com.igxxd.globalsearch.platform.Services;
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

public class NeoForgeConfigIndexProvider implements SearchIndexProvider {

    private static class NeoForgeConfigEntry {
        final Object modConfig;
        final Object configValue;

        NeoForgeConfigEntry(Object modConfig, Object configValue) {
            this.modConfig = modConfig;
            this.configValue = configValue;
        }
    }

    private final Map<String, NeoForgeConfigEntry> configEntries = new HashMap<>();

    @Override
    public String getName() {
        return "NeoForge Configs";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        configEntries.clear();
        try {
            Class<?> configTrackerCls = Class.forName("net.neoforged.fml.config.ConfigTracker");
            Field instanceF = configTrackerCls.getField("INSTANCE");
            Object tracker = instanceF.get(null);
            if (tracker != null) {
                Field fileMapF = configTrackerCls.getDeclaredField("fileMap");
                fileMapF.setAccessible(true);
                Map<?, ?> fileMap = (Map<?, ?>) fileMapF.get(tracker);
                if (fileMap != null) {
                    for (Object modCfg : fileMap.values()) {
                        if (modCfg != null) {
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
                                            title = ProviderUtils.formatCamelCase(pathKey.substring(lastDot + 1));
                                        }

                                        String path = modName + " > " + pathKey;
                                        String id = modId + ":neoforge:" + pathKey;
                                        String key = SearchResultType.SETTING.name() + ":" + id.toLowerCase(Locale.ROOT);
                                        if (seenKeys.add(key)) {
                                            String desc = "";
                                            net.minecraft.locale.Language lang = net.minecraft.locale.Language.getInstance();
                                            String[] candidates = new String[]{
                                                    modId + ".config." + pathKey + ".tooltip",
                                                    modId + ".config." + pathKey + ".desc",
                                                    modId + ".config." + pathKey,
                                                    modId + "." + pathKey + ".tooltip",
                                                    modId + "." + pathKey
                                            };
                                            for (String cand : candidates) {
                                                if (lang.has(cand)) {
                                                    desc = lang.getOrDefault(cand);
                                                    break;
                                                }
                                            }
                                            if (desc.isEmpty()) {
                                                desc = "NeoForge Config (" + pathKey + ")";
                                            }

                                            configEntries.put(id, new NeoForgeConfigEntry(modCfg, configValue));

                                            results.add(SearchResult.builder(SearchResultType.SETTING, id, title)
                                                    .path(path)
                                                    .value(String.valueOf(val))
                                                    .modId(modId)
                                                    .modName(modName)
                                                    .description(desc)
                                                    .editable(true)
                                                    .provider(this)
                                                    .build()
                                            );
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

    @Override
    public boolean canEdit(SearchResult result) {
        return result != null && configEntries.containsKey(result.getId());
    }

    @Override
    public boolean handleEdit(Minecraft minecraft, SearchResult result, Screen screen, Consumer<String> onValueChanged) {
        if (result == null) return false;
        NeoForgeConfigEntry entry = configEntries.get(result.getId());
        if (entry == null) return false;

        try {
            Method getValM = entry.configValue.getClass().getMethod("get");
            Object currentVal = getValM.invoke(entry.configValue);

            GlobalSettingEditScreen.SettingType settingType = null;
            if (currentVal instanceof Boolean) {
                settingType = GlobalSettingEditScreen.SettingType.BOOLEAN;
            } else if (currentVal instanceof Integer || currentVal instanceof Long) {
                settingType = GlobalSettingEditScreen.SettingType.INTEGER;
            } else if (currentVal instanceof Double || currentVal instanceof Float) {
                settingType = GlobalSettingEditScreen.SettingType.DOUBLE;
            }

            // Open GlobalSettingEditScreen for all settings
            minecraft.gui.setScreen(new GlobalSettingEditScreen(
                    screen,
                    result,
                    settingType,
                    List.of(),
                    String.valueOf(currentVal),
                    newValStr -> {
                        Object parsed = parseValue(currentVal != null ? currentVal.getClass() : String.class, newValStr);
                        return setNeoForgeValue(entry, parsed);
                    },
                    onValueChanged
            ));
            return true;
        } catch (Throwable t) {
            Constants.LOG.error("Failed to edit NeoForge config {}: {}", result.getId(), t.getMessage());
        }
        return false;
    }

    private boolean setNeoForgeValue(NeoForgeConfigEntry entry, Object newVal) {
        try {
            for (Method m : entry.configValue.getClass().getMethods()) {
                if ("set".equals(m.getName()) && m.getParameterCount() == 1) {
                    m.invoke(entry.configValue, newVal);
                    break;
                }
            }
            for (Method m : entry.modConfig.getClass().getMethods()) {
                if ("save".equals(m.getName()) && m.getParameterCount() == 0) {
                    m.invoke(entry.modConfig);
                    break;
                }
            }
            return true;
        } catch (Throwable t) {
            Constants.LOG.error("Failed to save NeoForge config: {}", t.getMessage());
            return false;
        }
    }

    private Object parseValue(Class<?> targetType, String s) {
        String trimmed = s.trim();
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(trimmed);
        }
        if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(trimmed);
        }
        if (targetType == Float.class || targetType == float.class) {
            return Float.parseFloat(trimmed);
        }
        if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(trimmed);
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(trimmed);
        }
        return s;
    }
}
