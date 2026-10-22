package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.Constants;
import com.igxxd.globalsearch.gui.GlobalSettingEditScreen;
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
        String edit(Minecraft minecraft, Options options, SearchResult result, Screen screen, Consumer<String> onValueChanged) throws Exception;
    }

    private final Map<String, SettingEditor> settingEditors = new HashMap<>();

    @Override
    public String getName() {
        return "Vanilla Settings";
    }

    @Override
    public int getPriority() {
        return 90;
    }

    private void openModal(Minecraft mc, Screen scr, SearchResult result,
                           GlobalSettingEditScreen.SettingType type,
                           List<String> choices, String defaultVal,
                           GlobalSettingEditScreen.SettingSaveAction saveAction,
                           Consumer<String> cb) {
        mc.gui.setScreen(new GlobalSettingEditScreen(scr, result, type, choices, defaultVal, saveAction, cb));
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        Options options = minecraft.options;
        if (options == null) return;

        settingEditors.clear();

        // 1. Core known options with dedicated modal popup dialogs
        addSetting(results, seenKeys, "Field of View (FOV)", "Options > Video Settings", String.valueOf(options.fov().get()), "options.fov",
                (mc, opts, res, scr, cb) -> {
                    openModal(mc, scr, res, GlobalSettingEditScreen.SettingType.INTEGER, null, "70", newVal -> {
                        int n = Integer.parseInt(newVal.replaceAll("[^0-9]", ""));
                        opts.fov().set(n);
                        opts.save();
                        return true;
                    }, cb);
                    return null;
                });

        addSetting(results, seenKeys, "Brightness", "Options > Video Settings", String.format(Locale.ROOT, "%.0f%%", options.gamma().get() * 100), "options.gamma",
                (mc, opts, res, scr, cb) -> {
                    openModal(mc, scr, res, GlobalSettingEditScreen.SettingType.DOUBLE, null, "0.5", newVal -> {
                        double d = Double.parseDouble(newVal.replaceAll("[^0-9.]", ""));
                        if (d > 1.0) d /= 100.0;
                        opts.gamma().set(d);
                        opts.save();
                        return true;
                    }, cb);
                    return null;
                });

        addSetting(results, seenKeys, "Render Distance", "Options > Video Settings", options.getEffectiveRenderDistance() + " chunks", "options.renderDistance",
                (mc, opts, res, scr, cb) -> {
                    openModal(mc, scr, res, GlobalSettingEditScreen.SettingType.INTEGER, null, "12", newVal -> {
                        int n = Integer.parseInt(newVal.replaceAll("[^0-9]", ""));
                        opts.renderDistance().set(n);
                        opts.save();
                        return true;
                    }, cb);
                    return null;
                });

        addSetting(results, seenKeys, "Simulation Distance", "Options > Video Settings", options.simulationDistance().get() + " chunks", "options.simulationDistance",
                (mc, opts, res, scr, cb) -> {
                    openModal(mc, scr, res, GlobalSettingEditScreen.SettingType.INTEGER, null, "10", newVal -> {
                        int n = Integer.parseInt(newVal.replaceAll("[^0-9]", ""));
                        opts.simulationDistance().set(n);
                        opts.save();
                        return true;
                    }, cb);
                    return null;
                });

        addSetting(results, seenKeys, "GUI Scale", "Options > Video Settings", options.guiScale().get() == 0 ? "Auto" : String.valueOf(options.guiScale().get()), "options.guiScale",
                (mc, opts, res, scr, cb) -> {
                    openModal(mc, scr, res, GlobalSettingEditScreen.SettingType.CHOICE, List.of("Auto", "1", "2", "3", "4"), "Auto", newVal -> {
                        int s = newVal.equalsIgnoreCase("Auto") ? 0 : Integer.parseInt(newVal.trim());
                        opts.guiScale().set(s);
                        opts.save();
                        return true;
                    }, cb);
                    return null;
                });

        addSetting(results, seenKeys, "Max Framerate", "Options > Video Settings", options.framerateLimit().get() >= 260 ? "Unlimited" : options.framerateLimit().get() + " fps", "options.framerateLimit",
                (mc, opts, res, scr, cb) -> {
                    openModal(mc, scr, res, GlobalSettingEditScreen.SettingType.INTEGER, null, "120", newVal -> {
                        int fps = newVal.equalsIgnoreCase("Unlimited") ? 260 : Integer.parseInt(newVal.replaceAll("[^0-9]", ""));
                        opts.framerateLimit().set(fps);
                        opts.save();
                        return true;
                    }, cb);
                    return null;
                });

        addSetting(results, seenKeys, "VSync", "Options > Video Settings", options.enableVsync().get() ? "ON" : "OFF", "options.enableVsync",
                (mc, opts, res, scr, cb) -> {
                    openModal(mc, scr, res, GlobalSettingEditScreen.SettingType.BOOLEAN, null, "false", newVal -> {
                        boolean b = Boolean.parseBoolean(newVal);
                        opts.enableVsync().set(b);
                        opts.save();
                        return true;
                    }, cb);
                    return null;
                });

        addSetting(results, seenKeys, "Fullscreen", "Options > Video Settings", options.fullscreen().get() ? "ON" : "OFF", "options.fullscreen",
                (mc, opts, res, scr, cb) -> {
                    openModal(mc, scr, res, GlobalSettingEditScreen.SettingType.BOOLEAN, null, "false", newVal -> {
                        boolean b = Boolean.parseBoolean(newVal);
                        opts.fullscreen().set(b);
                        opts.save();
                        return true;
                    }, cb);
                    return null;
                });

        addSetting(results, seenKeys, "View Bobbing", "Options > Video Settings", options.bobView().get() ? "ON" : "OFF", "options.bobView",
                (mc, opts, res, scr, cb) -> {
                    openModal(mc, scr, res, GlobalSettingEditScreen.SettingType.BOOLEAN, null, "true", newVal -> {
                        boolean b = Boolean.parseBoolean(newVal);
                        opts.bobView().set(b);
                        opts.save();
                        return true;
                    }, cb);
                    return null;
                });

        addSetting(results, seenKeys, "Auto-Jump", "Options > Controls", options.autoJump().get() ? "ON" : "OFF", "options.autoJump",
                (mc, opts, res, scr, cb) -> {
                    openModal(mc, scr, res, GlobalSettingEditScreen.SettingType.BOOLEAN, null, "false", newVal -> {
                        boolean b = Boolean.parseBoolean(newVal);
                        opts.autoJump().set(b);
                        opts.save();
                        return true;
                    }, cb);
                    return null;
                });

        addSetting(results, seenKeys, "Mouse Sensitivity", "Options > Controls > Mouse Settings", String.format(Locale.ROOT, "%.0f%%", options.sensitivity().get() * 100), "options.sensitivity",
                (mc, opts, res, scr, cb) -> {
                    openModal(mc, scr, res, GlobalSettingEditScreen.SettingType.DOUBLE, null, "0.5", newVal -> {
                        double d = Double.parseDouble(newVal.replaceAll("[^0-9.]", ""));
                        if (d > 1.0) d /= 100.0;
                        opts.sensitivity().set(d);
                        opts.save();
                        return true;
                    }, cb);
                    return null;
                });

        addSetting(results, seenKeys, "Invert Mouse", "Options > Controls > Mouse Settings", options.invertMouseY().get() ? "ON" : "OFF", "options.invertMouse",
                (mc, opts, res, scr, cb) -> {
                    openModal(mc, scr, res, GlobalSettingEditScreen.SettingType.BOOLEAN, null, "false", newVal -> {
                        boolean b = Boolean.parseBoolean(newVal);
                        opts.invertMouseY().set(b);
                        opts.save();
                        return true;
                    }, cb);
                    return null;
                });

        addSetting(results, seenKeys, "Particles", "Options > Video Settings", String.valueOf(options.particles().get()), "options.particles",
                (mc, opts, res, scr, cb) -> {
                    List<String> partChoices = List.of("ALL", "DECREASED", "MINIMAL");
                    openModal(mc, scr, res, GlobalSettingEditScreen.SettingType.CHOICE, partChoices, "ALL", newVal -> {
                        opts.particles().set(net.minecraft.server.level.ParticleStatus.valueOf(newVal.toUpperCase(Locale.ROOT)));
                        opts.save();
                        return true;
                    }, cb);
                    return null;
                });

        // 2. Sound sources with modal volume slider
        for (SoundSource source : SoundSource.values()) {
            String name = ProviderUtils.formatCamelCase(source.getName()) + " Volume";
            String value = String.format(Locale.ROOT, "%.0f%%", options.getSoundSourceVolume(source) * 100);
            addSetting(results, seenKeys, name, "Options > Music & Sounds", value, "soundCategory." + source.getName(),
                    (mc, opts, res, scr, cb) -> {
                        openModal(mc, scr, res, GlobalSettingEditScreen.SettingType.DOUBLE, null, "1.0", newVal -> {
                            double d = Double.parseDouble(newVal.replaceAll("[^0-9.]", ""));
                            if (d > 1.0) d /= 100.0;
                            opts.getSoundSourceOptionInstance(source).set(d);
                            opts.save();
                            return true;
                        }, cb);
                        return null;
                    });
        }

        // 3. Introspect other OptionInstance methods
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
                            editor = (mc, opts, res, scr, cb) -> {
                                openModal(mc, scr, res, GlobalSettingEditScreen.SettingType.BOOLEAN, null, String.valueOf(instance.get()), newVal -> {
                                    ((OptionInstance<Boolean>) instance).set(Boolean.parseBoolean(newVal));
                                    opts.save();
                                    return true;
                                }, cb);
                                return null;
                            };
                        } else if (valObj instanceof Enum<?> e) {
                            Enum<?>[] constants = e.getDeclaringClass().getEnumConstants();
                            List<String> enumChoices = new ArrayList<>();
                            for (Enum<?> constant : constants) {
                                enumChoices.add(constant.name());
                            }
                            editor = (mc, opts, res, scr, cb) -> {
                                openModal(mc, scr, res, GlobalSettingEditScreen.SettingType.CHOICE, enumChoices, ((Enum<?>) instance.get()).name(), newVal -> {
                                    for (Enum<?> constant : constants) {
                                        if (constant.name().equalsIgnoreCase(newVal.trim())) {
                                            ((OptionInstance<Object>) instance).set(constant);
                                            opts.save();
                                            return true;
                                        }
                                    }
                                    return false;
                                }, cb);
                                return null;
                            };
                        } else if (valObj instanceof Integer) {
                            editor = (mc, opts, res, scr, cb) -> {
                                openModal(mc, scr, res, GlobalSettingEditScreen.SettingType.INTEGER, null, String.valueOf(instance.get()), newVal -> {
                                    ((OptionInstance<Integer>) instance).set(Integer.parseInt(newVal.replaceAll("[^0-9]", "")));
                                    opts.save();
                                    return true;
                                }, cb);
                                return null;
                            };
                        } else if (valObj instanceof Double) {
                            editor = (mc, opts, res, scr, cb) -> {
                                openModal(mc, scr, res, GlobalSettingEditScreen.SettingType.DOUBLE, null, String.valueOf(instance.get()), newVal -> {
                                    ((OptionInstance<Double>) instance).set(Double.parseDouble(newVal.trim()));
                                    opts.save();
                                    return true;
                                }, cb);
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
                String newVal = editor.edit(minecraft, minecraft.options, result, screen, onValueChanged);
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
