package com.igxxd.globalsearch.platform;

import com.igxxd.globalsearch.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.HashMap;
import java.util.Map;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public String getModName(String modId) {
        if ("minecraft".equalsIgnoreCase(modId)) {
            return "Minecraft";
        }
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getName())
                .orElse(modId);
    }

    @Override
    public Map<String, String> getLoadedMods() {
        Map<String, String> mods = new HashMap<>();
        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            String id = container.getMetadata().getId();
            // Hide internal Fabric API submodules
            if (container.getContainingMod().isPresent()) {
                continue;
            }
            if (container.getMetadata().containsCustomValue("fabric-api:module-lifecycle")) {
                continue;
            }
            if (id.startsWith("fabric-") && !id.equals("fabric-api")) {
                continue;
            }
            mods.put(id, container.getMetadata().getName());
        }
        return mods;
    }

    @Override
    public String getModDescription(String modId) {
        if ("minecraft".equalsIgnoreCase(modId)) {
            return "The base Minecraft game.";
        }
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getDescription())
                .orElse("");
    }

    @Override
    public java.util.Optional<java.io.InputStream> getModIconStream(String modId) {
        if ("minecraft".equalsIgnoreCase(modId)) {
            return java.util.Optional.empty();
        }
        try {
            var containerOpt = FabricLoader.getInstance().getModContainer(modId);
            if (containerOpt.isPresent()) {
                ModContainer container = containerOpt.get();
                for (int size : new int[]{64, 128, 32, 16, 0}) {
                    var iconPathOpt = container.getMetadata().getIconPath(size);
                    if (iconPathOpt.isPresent()) {
                        var pathOpt = container.findPath(iconPathOpt.get());
                        if (pathOpt.isPresent() && java.nio.file.Files.exists(pathOpt.get())) {
                            return java.util.Optional.of(java.nio.file.Files.newInputStream(pathOpt.get()));
                        }
                    }
                }
                for (String fallback : new String[]{"icon.png", "assets/" + modId + "/icon.png", "logo.png"}) {
                    var pathOpt = container.findPath(fallback);
                    if (pathOpt.isPresent() && java.nio.file.Files.exists(pathOpt.get())) {
                        return java.util.Optional.of(java.nio.file.Files.newInputStream(pathOpt.get()));
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return java.util.Optional.empty();
    }
}
