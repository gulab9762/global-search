package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.Constants;
import com.igxxd.globalsearch.platform.Services;
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class NeoForgeConfigIndexProvider implements SearchIndexProvider {

    @Override
    public String getName() {
        return "NeoForge Configs";
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
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

                                            results.add(new SearchResult(
                                                    SearchResultType.SETTING,
                                                    id,
                                                    title,
                                                    path,
                                                    String.valueOf(val),
                                                    modId,
                                                    modName,
                                                    ItemStack.EMPTY,
                                                    desc
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
}
