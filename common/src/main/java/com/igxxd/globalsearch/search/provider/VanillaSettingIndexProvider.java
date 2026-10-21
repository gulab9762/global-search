package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.Constants;
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

public class VanillaSettingIndexProvider implements SearchIndexProvider {

    @FunctionalInterface
    public interface SettingEditor {
        String edit(Minecraft minecraft, Options options, Screen screen, Consumer<String> onValueChanged) throws Exception;
    }

    @FunctionalInterface
    public interface SimpleSettingEditor {
        String edit(Minecraft minecraft, Options options) throws Exception;
    }

    private void addSetting(List<SearchResult> results, Set<String> seenKeys, String title, String path, String value, String transKey, SimpleSettingEditor editor) {
        addSetting(results, seenKeys, title, path, value, transKey, editor != null ? (mc, opts, scr, cb) -> editor.edit(mc, opts) : null);
    }

    private final Map<String, SettingEditor> settingEditors = new HashMap<>();

    @Override
    public String getName() {
        return "Vanilla Settings";
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        Options options = minecraft.options;
        if (options == null) return;

        settingEditors.clear();

        // Core known options with exact end-to-end paths and option keys
        addSetting(results, seenKeys, "Field of View (FOV)", "Options > Video Settings", String.valueOf(options.fov().get()), "options.fov",
                (mc, opts) -> {
                    int cur = opts.fov().get();
                    int next = (cur >= 110) ? 70 : cur + 10;
                    opts.fov().set(next);
                    opts.save();
                    return String.valueOf(next);
                });

        addSetting(results, seenKeys, "Brightness", "Options > Video Settings", String.format(Locale.ROOT, "%.0f%%", options.gamma().get() * 100), "options.gamma",
                (mc, opts) -> {
                    double cur = opts.gamma().get();
                    double next = (cur >= 1.0) ? 0.0 : Math.min(1.0, cur + 0.25);
                    opts.gamma().set(next);
                    opts.save();
                    return String.format(Locale.ROOT, "%.0f%%", next * 100);
                });

        addSetting(results, seenKeys, "Render Distance", "Options > Video Settings", options.getEffectiveRenderDistance() + " chunks", "options.renderDistance",
                (mc, opts) -> {
                    int cur = opts.renderDistance().get();
                    int next = (cur >= 32) ? 8 : cur + 4;
                    opts.renderDistance().set(next);
                    opts.save();
                    return next + " chunks";
                });

        addSetting(results, seenKeys, "Simulation Distance", "Options > Video Settings", options.simulationDistance().get() + " chunks", "options.simulationDistance",
                (mc, opts) -> {
                    int cur = opts.simulationDistance().get();
                    int next = (cur >= 32) ? 8 : cur + 4;
                    opts.simulationDistance().set(next);
                    opts.save();
                    return next + " chunks";
                });

        addSetting(results, seenKeys, "GUI Scale", "Options > Video Settings", options.guiScale().get() == 0 ? "Auto" : String.valueOf(options.guiScale().get()), "options.guiScale",
                (mc, opts) -> {
                    int cur = opts.guiScale().get();
                    int next = (cur + 1) % 5;
                    opts.guiScale().set(next);
                    opts.save();
                    return next == 0 ? "Auto" : String.valueOf(next);
                });

        addSetting(results, seenKeys, "Max Framerate", "Options > Video Settings", options.framerateLimit().get() >= 260 ? "Unlimited" : options.framerateLimit().get() + " fps", "options.framerateLimit",
                (mc, opts) -> {
                    int cur = opts.framerateLimit().get();
                    int next = (cur >= 260) ? 30 : (cur < 60 ? 60 : (cur < 120 ? 120 : (cur < 144 ? 144 : 260)));
                    opts.framerateLimit().set(next);
                    opts.save();
                    return next >= 260 ? "Unlimited" : next + " fps";
                });

        addSetting(results, seenKeys, "VSync", "Options > Video Settings", options.enableVsync().get() ? "ON" : "OFF", "options.enableVsync",
                (mc, opts) -> {
                    boolean val = !opts.enableVsync().get();
                    opts.enableVsync().set(val);
                    opts.save();
                    return val ? "ON" : "OFF";
                });

        addSetting(results, seenKeys, "Fullscreen", "Options > Video Settings", options.fullscreen().get() ? "ON" : "OFF", "options.fullscreen",
                (mc, opts) -> {
                    boolean val = !opts.fullscreen().get();
                    opts.fullscreen().set(val);
                    opts.save();
                    return val ? "ON" : "OFF";
                });

        addSetting(results, seenKeys, "View Bobbing", "Options > Video Settings", options.bobView().get() ? "ON" : "OFF", "options.bobView",
                (mc, opts) -> {
                    boolean val = !opts.bobView().get();
                    opts.bobView().set(val);
                    opts.save();
                    return val ? "ON" : "OFF";
                });

        addSetting(results, seenKeys, "Auto-Jump", "Options > Controls", options.autoJump().get() ? "ON" : "OFF", "options.autoJump",
                (mc, opts) -> {
                    boolean val = !opts.autoJump().get();
                    opts.autoJump().set(val);
                    opts.save();
                    return val ? "ON" : "OFF";
                });

        addSetting(results, seenKeys, "Mouse Sensitivity", "Options > Controls > Mouse Settings", String.format(Locale.ROOT, "%.0f%%", options.sensitivity().get() * 100), "options.sensitivity",
                (mc, opts) -> {
                    double cur = opts.sensitivity().get();
                    double next = (cur >= 1.0) ? 0.25 : cur + 0.25;
                    opts.sensitivity().set(next);
                    opts.save();
                    return String.format(Locale.ROOT, "%.0f%%", next * 100);
                });

        addSetting(results, seenKeys, "Invert Mouse", "Options > Controls > Mouse Settings", options.invertMouseY().get() ? "ON" : "OFF", "options.invertMouse",
                (mc, opts) -> {
                    boolean val = !opts.invertMouseY().get();
                    opts.invertMouseY().set(val);
                    opts.save();
                    return val ? "ON" : "OFF";
                });

        addSetting(results, seenKeys, "Particles", "Options > Video Settings", String.valueOf(options.particles().get()), "options.particles",
                (mc, opts) -> {
                    net.minecraft.server.level.ParticleStatus cur = opts.particles().get();
                    net.minecraft.server.level.ParticleStatus[] all = net.minecraft.server.level.ParticleStatus.values();
                    net.minecraft.server.level.ParticleStatus next = all[(cur.ordinal() + 1) % all.length];
                    opts.particles().set(next);
                    opts.save();
                    return String.valueOf(next);
                });

        addSetting(results, seenKeys, "High Contrast", "Options > Accessibility Settings", options.highContrast().get() ? "ON" : "OFF", "options.accessibility.high_contrast",
                (mc, opts) -> {
                    boolean val = !opts.highContrast().get();
                    opts.highContrast().set(val);
                    opts.save();
                    return val ? "ON" : "OFF";
                });

        addSetting(results, seenKeys, "Show Subtitles", "Options > Music & Sounds", options.showSubtitles().get() ? "ON" : "OFF", "options.showSubtitles",
                (mc, opts) -> {
                    boolean val = !opts.showSubtitles().get();
                    opts.showSubtitles().set(val);
                    opts.save();
                    return val ? "ON" : "OFF";
                });

        // Sound sources with full path
        for (SoundSource source : SoundSource.values()) {
            String name = ProviderUtils.formatCamelCase(source.getName()) + " Volume";
            String value = String.format(Locale.ROOT, "%.0f%%", options.getSoundSourceVolume(source) * 100);
            addSetting(results, seenKeys, name, "Options > Music & Sounds", value, "soundCategory." + source.getName(),
                    (mc, opts) -> {
                        float cur = opts.getSoundSourceVolume(source);
                        int step = Math.round(cur * 10f);
                        int nextStep = (step + 1) % 11;
                        float nextVal = nextStep / 10f;
                        opts.getSoundSourceOptionInstance(source).set((double) nextVal);
                        opts.save();
                        return String.format(Locale.ROOT, "%.0f%%", nextVal * 100);
                    });
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
                        String transKey = null;
                        try {
                            Field captionField = OptionInstance.class.getDeclaredField("caption");
                            captionField.setAccessible(true);
                            Component caption = (Component) captionField.get(instance);
                            if (caption != null) {
                                readableName = caption.getString();
                                if (caption.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc) {
                                    transKey = tc.getKey();
                                }
                            }
                        } catch (Throwable ignored) {}

                        SettingEditor editor = null;
                        Object valObj = instance.get();
                        if (valObj instanceof Boolean) {
                            editor = (mc, opts, scr, cb) -> {
                                boolean val = !((Boolean) instance.get());
                                ((OptionInstance<Boolean>) instance).set(val);
                                opts.save();
                                return val ? "ON" : "OFF";
                            };
                        } else if (valObj instanceof Enum<?> e) {
                            editor = (mc, opts, scr, cb) -> {
                                Enum<?>[] constants = e.getDeclaringClass().getEnumConstants();
                                Enum<?> next = constants[(e.ordinal() + 1) % constants.length];
                                ((OptionInstance<Object>) instance).set(next);
                                opts.save();
                                return next.name();
                            };
                        } else if (valObj instanceof Integer) {
                            String finalReadableName = readableName;
                            String finalPath = path;
                            editor = (mc, opts, scr, cb) -> {
                                mc.gui.setScreen(new com.igxxd.globalsearch.gui.GlobalSettingEditScreen(
                                        scr,
                                        new SearchResult(SearchResultType.SETTING, "options." + methodName, finalReadableName, finalPath, String.valueOf(instance.get()), "minecraft", "Minecraft", ItemStack.EMPTY, ""),
                                        com.igxxd.globalsearch.gui.GlobalSettingEditScreen.SettingType.INTEGER,
                                        List.of(),
                                        String.valueOf(instance.get()),
                                        newValStr -> {
                                            int n = Integer.parseInt(newValStr.trim());
                                            ((OptionInstance<Integer>) instance).set(n);
                                            opts.save();
                                            return true;
                                        },
                                        cb
                                ));
                                return null;
                            };
                        } else if (valObj instanceof Double) {
                            String finalReadableName = readableName;
                            String finalPath = path;
                            editor = (mc, opts, scr, cb) -> {
                                mc.gui.setScreen(new com.igxxd.globalsearch.gui.GlobalSettingEditScreen(
                                        scr,
                                        new SearchResult(SearchResultType.SETTING, "options." + methodName, finalReadableName, finalPath, String.valueOf(instance.get()), "minecraft", "Minecraft", ItemStack.EMPTY, ""),
                                        com.igxxd.globalsearch.gui.GlobalSettingEditScreen.SettingType.DOUBLE,
                                        List.of(),
                                        String.valueOf(instance.get()),
                                        newValStr -> {
                                            double d = Double.parseDouble(newValStr.trim());
                                            ((OptionInstance<Double>) instance).set(d);
                                            opts.save();
                                            return true;
                                        },
                                        cb
                                ));
                                return null;
                            };
                        }

                        addSetting(results, seenKeys, readableName, path, String.valueOf(instance.get()), transKey, editor);
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

    private void addSetting(List<SearchResult> results, Set<String> seenKeys, String title, String path, String value, String transKey, SettingEditor editor) {
        String id = "setting:" + title.toLowerCase(Locale.ROOT).replace(' ', '_');
        String key = SearchResultType.SETTING.name() + ":" + id;
        if (seenKeys.add(key)) {
            if (editor != null) {
                settingEditors.put(id, editor);
            }
            String desc = "";
            Language lang = Language.getInstance();
            if (transKey != null && !transKey.isEmpty()) {
                if (lang.has(transKey + ".tooltip")) {
                    desc = lang.getOrDefault(transKey + ".tooltip");
                } else if (lang.has(transKey + ".desc")) {
                    desc = lang.getOrDefault(transKey + ".desc");
                }
            }
            if (desc.isEmpty()) {
                String fallbackKey = "options." + title.toLowerCase(Locale.ROOT).replace(' ', '_') + ".tooltip";
                if (lang.has(fallbackKey)) {
                    desc = lang.getOrDefault(fallbackKey);
                }
            }
            if (desc.isEmpty()) {
                desc = "Vanilla Setting (" + path + ")";
            }

            SearchResult.Builder builder = SearchResult.builder(SearchResultType.SETTING, id, title)
                    .path(path)
                    .value(value)
                    .modId("minecraft")
                    .modName("Minecraft")
                    .description(desc)
                    .provider(this);

            if (editor != null) {
                builder.editable(true);
            }

            results.add(builder.build());
        }
    }

    @Override
    public boolean canEdit(SearchResult result) {
        return result != null && settingEditors.containsKey(result.getId());
    }

    @Override
    public boolean handleEdit(Minecraft minecraft, SearchResult result, Screen screen, Consumer<String> onValueChanged) {
        if (minecraft.options == null) return false;
        SettingEditor editor = settingEditors.get(result.getId());
        if (editor != null) {
            try {
                String newVal = editor.edit(minecraft, minecraft.options, screen, onValueChanged);
                if (newVal != null) {
                    result.setValue(newVal);
                    if (onValueChanged != null) {
                        onValueChanged.accept(newVal);
                    }
                }
                return true;
            } catch (Throwable t) {
                Constants.LOG.error("Failed to edit setting '{}': {}", result.getId(), t.getMessage());
            }
        }
        return false;
    }
}

