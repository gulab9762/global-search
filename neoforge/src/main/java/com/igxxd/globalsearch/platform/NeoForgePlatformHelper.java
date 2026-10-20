package com.igxxd.globalsearch.platform;

import com.igxxd.globalsearch.platform.services.IPlatformHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.language.IModInfo;

import java.util.HashMap;
import java.util.Map;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }

    @Override
    public String getModName(String modId) {
        if ("minecraft".equalsIgnoreCase(modId)) {
            return "Minecraft";
        }
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(modId);
    }

    @Override
    public Map<String, String> getLoadedMods() {
        Map<String, String> mods = new HashMap<>();
        for (IModInfo info : ModList.get().getMods()) {
            mods.put(info.getModId(), info.getDisplayName());
        }
        return mods;
    }
}
