package com.igxxd.globalsearch.gui;

import com.igxxd.globalsearch.platform.Services;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ModIconManager {

    private static final Map<String, Identifier> LOADED_ICONS = new HashMap<>();
    private static final Set<String> FAILED_MODS = new HashSet<>();

    public static Identifier getModIcon(String modId) {
        if (modId == null || modId.isEmpty()) {
            return null;
        }
        if (FAILED_MODS.contains(modId)) {
            return null;
        }
        if (LOADED_ICONS.containsKey(modId)) {
            return LOADED_ICONS.get(modId);
        }

        try {
            Optional<InputStream> streamOpt = Services.PLATFORM.getModIconStream(modId);
            if (streamOpt.isPresent()) {
                try (InputStream is = streamOpt.get()) {
                    NativeImage nativeImage = NativeImage.read(is);
                    DynamicTexture dynamicTexture = new DynamicTexture(() -> "modicon_" + modId, nativeImage);
                    String sanitized = modId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
                    Identifier id = Identifier.fromNamespaceAndPath("globalsearch", "modicon_" + sanitized);
                    Minecraft.getInstance().getTextureManager().register(id, dynamicTexture);
                    LOADED_ICONS.put(modId, id);
                    return id;
                }
            }
        } catch (Throwable ignored) {
        }

        FAILED_MODS.add(modId);
        return null;
    }
}
