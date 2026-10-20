package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.Constants;
import com.igxxd.globalsearch.platform.Services;
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ClothConfigIndexProvider implements SearchIndexProvider {

    @Override
    public String getName() {
        return "Cloth Config";
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
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
            Constants.LOG.debug("Cloth config scanner finished with message", t);
        }
    }
}
