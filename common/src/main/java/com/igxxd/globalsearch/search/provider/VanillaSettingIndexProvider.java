package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class VanillaSettingIndexProvider implements SearchIndexProvider {

    @Override
    public String getName() {
        return "Vanilla Settings";
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        Options options = minecraft.options;
        if (options == null) return;

        // Core known options with exact end-to-end paths
        addSetting(results, seenKeys, "Field of View (FOV)", "Options > Video Settings", String.valueOf(options.fov().get()));
        addSetting(results, seenKeys, "Brightness", "Options > Video Settings", String.format(Locale.ROOT, "%.0f%%", options.gamma().get() * 100));
        addSetting(results, seenKeys, "Render Distance", "Options > Video Settings", options.getEffectiveRenderDistance() + " chunks");
        addSetting(results, seenKeys, "Simulation Distance", "Options > Video Settings", options.simulationDistance().get() + " chunks");
        addSetting(results, seenKeys, "GUI Scale", "Options > Video Settings", options.guiScale().get() == 0 ? "Auto" : String.valueOf(options.guiScale().get()));
        addSetting(results, seenKeys, "Max Framerate", "Options > Video Settings", options.framerateLimit().get() >= 260 ? "Unlimited" : options.framerateLimit().get() + " fps");
        addSetting(results, seenKeys, "VSync", "Options > Video Settings", options.enableVsync().get() ? "ON" : "OFF");
        addSetting(results, seenKeys, "Fullscreen", "Options > Video Settings", options.fullscreen().get() ? "ON" : "OFF");
        addSetting(results, seenKeys, "View Bobbing", "Options > Video Settings", options.bobView().get() ? "ON" : "OFF");
        addSetting(results, seenKeys, "Auto-Jump", "Options > Controls", options.autoJump().get() ? "ON" : "OFF");
        addSetting(results, seenKeys, "Mouse Sensitivity", "Options > Controls > Mouse Settings", String.format(Locale.ROOT, "%.0f%%", options.sensitivity().get() * 100));
        addSetting(results, seenKeys, "Invert Mouse", "Options > Controls > Mouse Settings", options.invertMouseY().get() ? "ON" : "OFF");
        addSetting(results, seenKeys, "Particles", "Options > Video Settings", String.valueOf(options.particles().get()));
        addSetting(results, seenKeys, "High Contrast", "Options > Accessibility Settings", options.highContrast().get() ? "ON" : "OFF");
        addSetting(results, seenKeys, "Show Subtitles", "Options > Music & Sounds", options.showSubtitles().get() ? "ON" : "OFF");

        // Sound sources with full path
        for (SoundSource source : SoundSource.values()) {
            String name = ProviderUtils.formatCamelCase(source.getName()) + " Volume";
            String value = String.format(Locale.ROOT, "%.0f%%", options.getSoundSourceVolume(source) * 100);
            addSetting(results, seenKeys, name, "Options > Music & Sounds", value);
        }

        // Introspect other OptionInstance methods
        for (Method method : Options.class.getMethods()) {
            if (OptionInstance.class.isAssignableFrom(method.getReturnType()) && method.getParameterCount() == 0) {
                try {
                    String methodName = method.getName();
                    String readableName = ProviderUtils.formatCamelCase(methodName);
                    OptionInstance<?> instance = (OptionInstance<?>) method.invoke(options);
                    if (instance != null && instance.get() != null) {
                        String path = getVanillaOptionPath(methodName);
                        addSetting(results, seenKeys, readableName, path, String.valueOf(instance.get()));
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private String getVanillaOptionPath(String methodName) {
        String n = methodName.toLowerCase(Locale.ROOT);
        if (n.equals("fov") || n.equals("gamma") || n.contains("guiscale") || n.contains("framerate") ||
                n.contains("vsync") || n.contains("fullscreen") || n.contains("bobview") || n.contains("particle") ||
                n.contains("cloud") || n.contains("render") || n.contains("simulation") || n.contains("chunk") ||
                n.contains("mipmap") || n.contains("texture") || n.contains("ambientocclusion") || n.contains("leaves") ||
                n.contains("vignette") || n.contains("transparency") || n.contains("attackindicator") ||
                n.contains("autosave") || n.contains("graphics") || n.contains("weather") || n.contains("blurriness") ||
                n.contains("panorama")) {
            return "Options > Video Settings";
        }
        if (n.contains("sensitivity") || n.contains("mouse") || n.contains("cursor")) {
            return "Options > Controls > Mouse Settings";
        }
        if (n.contains("crouch") || n.contains("sprint") || n.contains("autojump") || n.contains("operatoritems") ||
                n.contains("toggle") || n.contains("mainhand") || n.contains("minecart")) {
            return "Options > Controls";
        }
        if (n.contains("chat") || n.contains("suggest")) {
            return "Options > Chat Settings";
        }
        if (n.contains("sound") || n.contains("volume") || n.contains("audio") || n.contains("subtitles") ||
                n.contains("music") || n.contains("device")) {
            return "Options > Music & Sounds";
        }
        if (n.contains("contrast") || n.contains("narrator") || n.contains("effectscale") || n.contains("glint") ||
                n.contains("damagetilt") || n.contains("notificationdisplay") || n.contains("darkness")) {
            return "Options > Accessibility Settings";
        }
        if (n.contains("font") || n.contains("glyph") || n.contains("language")) {
            return "Options > Language";
        }
        if (n.contains("telemetry")) {
            return "Options > Telemetry";
        }
        return "Options > Miscellaneous";
    }

    private void addSetting(List<SearchResult> results, Set<String> seenKeys, String title, String path, String value) {
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
                    "Vanilla Setting (" + path + ")"
            ));
        }
    }
}
