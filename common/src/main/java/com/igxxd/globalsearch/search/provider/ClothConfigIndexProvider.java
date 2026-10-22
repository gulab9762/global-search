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
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;

public class ClothConfigIndexProvider implements SearchIndexProvider {

    private static class ClothFieldEntry {
        final Object holder;
        final Object configObj;
        final Field field;
        final String defaultValueStr;

        ClothFieldEntry(Object holder, Object configObj, Field field, String defaultValueStr) {
            this.holder = holder;
            this.configObj = configObj;
            this.field = field;
            this.defaultValueStr = defaultValueStr != null ? defaultValueStr : "";
        }
    }

    private final Map<String, ClothFieldEntry> clothEntries = new HashMap<>();

    @Override
    public String getName() {
        return "Cloth Config";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        clothEntries.clear();
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
                                String title = ProviderUtils.formatCamelCase(fieldName);
                                String path = modName + " > Config";
                                String id = modId + ":cloth:" + fieldName;

                                String key = SearchResultType.SETTING.name() + ":" + id.toLowerCase(Locale.ROOT);
                                if (seenKeys.add(key)) {
                                    String desc = "";
                                    net.minecraft.locale.Language lang = net.minecraft.locale.Language.getInstance();
                                    String[] candTooltipKeys = new String[]{
                                            "text.autoconfig." + modId + ".option." + fieldName + ".@Tooltip",
                                            "text.autoconfig." + modId + ".option." + fieldName + ".tooltip",
                                            "text.autoconfig." + modId + ".option." + fieldName + ".desc",
                                            modId + ".config." + fieldName + ".tooltip"
                                    };
                                    for (String candKey : candTooltipKeys) {
                                        if (lang.has(candKey)) {
                                            desc = lang.getOrDefault(candKey);
                                            break;
                                        }
                                    }
                                    if (desc.isEmpty()) {
                                        desc = "Cloth Config setting for " + modName;
                                    }

                                    String valStr = String.valueOf(val);
                                    clothEntries.put(id, new ClothFieldEntry(holder, configObj, cfgField, valStr));

                                    SearchResult sr = SearchResult.builder(SearchResultType.SETTING, id, title)
                                            .path(path)
                                            .value(valStr)
                                            .defaultValue(valStr)
                                            .modId(modId)
                                            .modName(modName)
                                            .description(desc)
                                            .editable(true)
                                            .provider(this)
                                            .build();

                                    results.add(sr);
                                }
                            }
                        }
                    }
                }
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable t) {
            Constants.LOG.debug("Cloth config scanner finished with message", t);
        }
    }

    private void saveClothHolder(Object holder) {
        if (holder == null) return;
        try {
            for (Method m : holder.getClass().getMethods()) {
                if ("save".equals(m.getName()) && m.getParameterCount() == 0) {
                    m.invoke(holder);
                    return;
                }
            }
        } catch (Throwable t) {
            Constants.LOG.error("Failed to save Cloth Config: {}", t.getMessage());
        }
    }

    @Override
    public boolean canEdit(SearchResult result) {
        return result != null && clothEntries.containsKey(result.getId());
    }

    @Override
    public boolean handleEdit(Minecraft minecraft, SearchResult result, Screen screen, Consumer<String> onValueChanged) {
        if (result == null) return false;
        ClothFieldEntry entry = clothEntries.get(result.getId());
        if (entry == null) return false;

        try {
            Class<?> type = entry.field.getType();
            Object currentVal = entry.field.get(entry.configObj);

            GlobalSettingEditScreen.SettingType settingType = null;
            if (type == boolean.class || type == Boolean.class) {
                settingType = GlobalSettingEditScreen.SettingType.BOOLEAN;
            } else if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
                settingType = GlobalSettingEditScreen.SettingType.INTEGER;
            } else if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
                settingType = GlobalSettingEditScreen.SettingType.DOUBLE;
            }

            // Choice options for enums
            List<String> choiceOptions = new ArrayList<>();
            if (type.isEnum()) {
                settingType = GlobalSettingEditScreen.SettingType.CHOICE;
                for (Object constant : type.getEnumConstants()) {
                    choiceOptions.add(((Enum<?>) constant).name());
                }
            }

            // Open GlobalSettingEditScreen for all settings
            minecraft.gui.setScreen(new GlobalSettingEditScreen(
                    screen,
                    result,
                    settingType,
                    choiceOptions,
                    entry.defaultValueStr,
                    newValStr -> {
                        try {
                            Object parsed = parseValue(type, newValStr);
                            entry.field.set(entry.configObj, parsed);
                            saveClothHolder(entry.holder);
                            return true;
                        } catch (Exception e) {
                            return false;
                        }
                    },
                    onValueChanged
            ));
            return true;
        } catch (Throwable t) {
            Constants.LOG.error("Failed to edit Cloth config {}: {}", result.getId(), t.getMessage());
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object parseValue(Class<?> targetType, String s) {
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(s.trim());
        }
        if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(s.trim());
        }
        if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(s.trim());
        }
        if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(s.trim());
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(s.trim());
        }
        if (targetType.isEnum()) {
            return Enum.valueOf((Class<Enum>) targetType, s.trim());
        }
        return s;
    }
}
