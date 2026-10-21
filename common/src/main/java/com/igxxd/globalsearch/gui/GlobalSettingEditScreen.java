package com.igxxd.globalsearch.gui;

import com.igxxd.globalsearch.search.SearchResult;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Universal modal popup editor for settings across Minecraft and all installed mods.
 * Intelligently identifies value types (Boolean, Integer, Decimal, Choice, Text)
 * and renders specialized interactive controls:
 * - Boolean: Distinct dual toggle buttons ([ TRUE / ON ] vs [ FALSE / OFF ])
 * - Numeric: Draggable slider bar alongside direct number input with real-time two-way synchronization
 * - Choice: Clickable option pills and cycling buttons
 * - Text: Themed input box
 */
public class GlobalSettingEditScreen extends Screen {

    public enum SettingType {
        BOOLEAN("Boolean Switch", "◩", 0xFF10B981),
        INTEGER("Integer Number", "🔢", 0xFF3B82F6),
        DOUBLE("Decimal / Float", "📐", 0xFF8B5CF6),
        CHOICE("Option Choice", "📋", 0xFFF59E0B),
        STRING("Text Value", "🔤", 0xFF94A3B8);

        private final String displayName;
        private final String icon;
        private final int accentColor;

        SettingType(String displayName, String icon, int accentColor) {
            this.displayName = displayName;
            this.icon = icon;
            this.accentColor = accentColor;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIcon() {
            return icon;
        }

        public int getAccentColor() {
            return accentColor;
        }
    }

    @FunctionalInterface
    public interface SettingSaveAction {
        boolean save(String newValue) throws Exception;
    }

    private final Screen parentScreen;
    private final SearchResult result;
    private final SettingType settingType;
    private final List<String> choiceOptions;
    private final String defaultValue;
    private final SettingSaveAction saveAction;
    private final Consumer<String> onValueChanged;

    // UI State
    private boolean currentBoolean;
    private int currentChoiceIndex = 0;
    private EditBox valueInput;
    private ThemedSlider numericSlider;
    private ThemedButton boolTrueBtn;
    private ThemedButton boolFalseBtn;
    private ThemedButton choiceButton;
    private final List<ThemedButton> choicePills = new ArrayList<>();
    private String statusMessage = null;
    private boolean statusError = false;
    private boolean updatingFromSlider = false;

    public GlobalSettingEditScreen(Screen parentScreen, SearchResult result,
                                  SettingType settingType, List<String> choiceOptions,
                                  String defaultValue, SettingSaveAction saveAction,
                                  Consumer<String> onValueChanged) {
        super(Component.literal("Edit Setting: " + result.getTitle()));
        this.parentScreen = parentScreen;
        this.result = result;
        this.choiceOptions = choiceOptions != null ? choiceOptions : List.of();
        this.defaultValue = defaultValue;
        this.saveAction = saveAction;
        this.onValueChanged = onValueChanged;

        String rawVal = result.getValue() != null ? result.getValue().trim() : "";

        // Auto-detect setting type if not explicitly supplied or generic
        if (settingType != null) {
            this.settingType = settingType;
        } else if (!this.choiceOptions.isEmpty()) {
            this.settingType = SettingType.CHOICE;
        } else if (rawVal.equalsIgnoreCase("true") || rawVal.equalsIgnoreCase("false")
                || rawVal.equalsIgnoreCase("on") || rawVal.equalsIgnoreCase("off")
                || rawVal.equalsIgnoreCase("yes") || rawVal.equalsIgnoreCase("no")) {
            this.settingType = SettingType.BOOLEAN;
        } else if (isInteger(rawVal)) {
            this.settingType = SettingType.INTEGER;
        } else if (isDouble(rawVal)) {
            this.settingType = SettingType.DOUBLE;
        } else {
            this.settingType = SettingType.STRING;
        }

        // Initialize values
        if (this.settingType == SettingType.BOOLEAN) {
            this.currentBoolean = rawVal.equalsIgnoreCase("true") || rawVal.equalsIgnoreCase("on") || rawVal.equalsIgnoreCase("yes");
        } else if (this.settingType == SettingType.CHOICE && !this.choiceOptions.isEmpty()) {
            int foundIdx = this.choiceOptions.indexOf(rawVal);
            this.currentChoiceIndex = foundIdx >= 0 ? foundIdx : 0;
        }
    }

    private static boolean isInteger(String s) {
        if (s == null || s.isEmpty()) return false;
        try {
            Long.parseLong(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isDouble(String s) {
        if (s == null || s.isEmpty()) return false;
        try {
            Double.parseDouble(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void init() {
        int cardW = 360;
        int cardH = 220;
        int cardX = (this.width - cardW) / 2;
        int cardY = (this.height - cardH) / 2;

        SearchTheme theme = SearchTheme.getCurrent();
        String initialVal = result.getValue() != null ? result.getValue().trim() : "";
        choicePills.clear();

        int contentY = cardY + 74;

        if (this.settingType == SettingType.BOOLEAN) {
            // Dual Option Buttons: [ TRUE / ON ] vs [ FALSE / OFF ]
            int btnW = (cardW - 48 - 12) / 2;
            int btnH = 32;

            this.boolTrueBtn = new ThemedButton(
                    cardX + 24, contentY, btnW, btnH,
                    Component.literal("✓  TRUE / ON").withStyle(ChatFormatting.BOLD),
                    () -> setBooleanValue(true)
            ).setCentered(true).setHasDot(false)
                    .setCustomActiveColors(0xFF105936, 0xFF10B981)
                    .setActiveThemeHighlight(this.currentBoolean);

            this.boolFalseBtn = new ThemedButton(
                    cardX + 24 + btnW + 12, contentY, btnW, btnH,
                    Component.literal("✕  FALSE / OFF").withStyle(ChatFormatting.BOLD),
                    () -> setBooleanValue(false)
            ).setCentered(true).setHasDot(false)
                    .setCustomActiveColors(0xFF5A1E24, 0xFFEF4444)
                    .setActiveThemeHighlight(!this.currentBoolean);

            this.addRenderableWidget(this.boolTrueBtn);
            this.addRenderableWidget(this.boolFalseBtn);

        } else if (this.settingType == SettingType.INTEGER || this.settingType == SettingType.DOUBLE) {
            // Draggable bar alongside number input box (like FOV setting)
            boolean isInt = (this.settingType == SettingType.INTEGER);
            double initialNum = 0.0;
            try {
                initialNum = Double.parseDouble(initialVal);
            } catch (Exception ignored) {}

            double[] range = calculateRange(result.getTitle(), result.getPath(), initialNum, isInt);
            double min = range[0];
            double max = range[1];
            double step = range[2];

            int inputW = 74;
            int spacing = 10;
            int sliderW = cardW - 48 - inputW - spacing;
            int rowH = 22;

            int sliderX = cardX + 24;
            int inputX = sliderX + sliderW + spacing;

            // Draggable slider bar
            this.numericSlider = new ThemedSlider(
                    sliderX, contentY, sliderW, rowH,
                    min, max, initialNum, step, isInt,
                    val -> {
                        if (this.valueInput != null) {
                            this.updatingFromSlider = true;
                            if (isInt) {
                                this.valueInput.setValue(String.valueOf(Math.round(val)));
                            } else {
                                this.valueInput.setValue(String.format(Locale.ROOT, "%.2f", val));
                            }
                            this.updatingFromSlider = false;
                        }
                    }
            );
            this.addRenderableWidget(this.numericSlider);

            // Direct Number Input EditBox alongside the slider
            this.valueInput = new EditBox(this.font, inputX + 4, contentY + 2, inputW - 8, rowH - 4, Component.literal("Value"));
            this.valueInput.setBordered(false);
            this.valueInput.setTextColor(theme.getTitleColor());
            this.valueInput.setValue(initialVal);
            this.valueInput.setResponder(text -> {
                if (updatingFromSlider || numericSlider == null) return;
                try {
                    double typedVal = Double.parseDouble(text.trim());
                    numericSlider.ensureRange(typedVal);
                    numericSlider.setValue(typedVal, false);
                } catch (Exception ignored) {}
            });
            this.addRenderableWidget(this.valueInput);
            this.setInitialFocus(this.valueInput);

            // Step adjustment buttons underneath the slider & input
            int btnW = isInt ? 40 : 46;
            int btnH = 18;
            int stepY = contentY + 30;
            int startX = cardX + (cardW - (btnW * 4 + 18)) / 2;

            if (isInt) {
                addStepButton(startX, stepY, btnW, btnH, "-10", () -> adjustInteger(-10));
                addStepButton(startX + btnW + 6, stepY, btnW, btnH, "-1", () -> adjustInteger(-1));
                addStepButton(startX + (btnW + 6) * 2, stepY, btnW, btnH, "+1", () -> adjustInteger(1));
                addStepButton(startX + (btnW + 6) * 3, stepY, btnW, btnH, "+10", () -> adjustInteger(10));
            } else {
                addStepButton(startX, stepY, btnW, btnH, "-1.0", () -> adjustDouble(-1.0));
                addStepButton(startX + btnW + 6, stepY, btnW, btnH, "-0.1", () -> adjustDouble(-0.1));
                addStepButton(startX + (btnW + 6) * 2, stepY, btnW, btnH, "+0.1", () -> adjustDouble(0.1));
                addStepButton(startX + (btnW + 6) * 3, stepY, btnW, btnH, "+1.0", () -> adjustDouble(1.0));
            }

        } else if (this.settingType == SettingType.CHOICE && !this.choiceOptions.isEmpty()) {
            if (this.choiceOptions.size() <= 4) {
                // Render choice pill buttons side by side
                int count = this.choiceOptions.size();
                int pillSpacing = 6;
                int pillW = (cardW - 48 - (pillSpacing * (count - 1))) / count;
                int pillH = 28;

                for (int i = 0; i < count; i++) {
                    final int idx = i;
                    String optText = this.choiceOptions.get(i);
                    boolean isSelected = (idx == this.currentChoiceIndex);
                    ThemedButton pill = new ThemedButton(
                            cardX + 24 + i * (pillW + pillSpacing), contentY + 4, pillW, pillH,
                            Component.literal(optText),
                            () -> selectChoiceIndex(idx)
                    ).setCentered(true).setHasDot(false).setActiveThemeHighlight(isSelected);
                    this.choicePills.add(pill);
                    this.addRenderableWidget(pill);
                }
            } else {
                // Cycling choice button with arrows
                this.choiceButton = new ThemedButton(
                        cardX + 24, contentY + 4, cardW - 48, 28,
                        getChoiceLabel(),
                        () -> {
                            this.currentChoiceIndex = (this.currentChoiceIndex + 1) % this.choiceOptions.size();
                            if (this.choiceButton != null) {
                                this.choiceButton.setMessage(getChoiceLabel());
                            }
                        }
                ).setCentered(true).setHasDot(false).setActiveThemeHighlight(true);
                this.addRenderableWidget(this.choiceButton);
            }
        } else {
            // Text Input Box
            int inputH = 20;
            this.valueInput = new EditBox(this.font, cardX + 26, contentY + 4, cardW - 52, inputH, Component.literal("Value"));
            this.valueInput.setBordered(false);
            this.valueInput.setTextColor(theme.getTitleColor());
            this.valueInput.setValue(initialVal);
            this.addRenderableWidget(this.valueInput);
            this.setInitialFocus(this.valueInput);
        }

        // Bottom Action Buttons: Apply, Default (if available), Cancel
        int btnY = cardY + cardH - 34;
        int actionBtnH = 22;

        if (this.defaultValue != null && !this.defaultValue.isEmpty()) {
            int w3 = (cardW - 48 - 12) / 3;
            ThemedButton applyBtn = new ThemedButton(cardX + 24, btnY, w3, actionBtnH, Component.literal("✓ Apply"), this::applyChanges)
                    .setCentered(true).setHasDot(false).setActiveThemeHighlight(true);
            ThemedButton resetBtn = new ThemedButton(cardX + 24 + w3 + 6, btnY, w3, actionBtnH, Component.literal("↺ Default"), this::resetToDefault)
                    .setCentered(true).setHasDot(false);
            ThemedButton cancelBtn = new ThemedButton(cardX + 24 + (w3 + 6) * 2, btnY, w3, actionBtnH, Component.literal("✕ Cancel"), this::cancel)
                    .setCentered(true).setHasDot(false);

            this.addRenderableWidget(applyBtn);
            this.addRenderableWidget(resetBtn);
            this.addRenderableWidget(cancelBtn);
        } else {
            int w2 = (cardW - 48 - 8) / 2;
            ThemedButton applyBtn = new ThemedButton(cardX + 24, btnY, w2, actionBtnH, Component.literal("✓ Apply"), this::applyChanges)
                    .setCentered(true).setHasDot(false).setActiveThemeHighlight(true);
            ThemedButton cancelBtn = new ThemedButton(cardX + 24 + w2 + 8, btnY, w2, actionBtnH, Component.literal("✕ Cancel"), this::cancel)
                    .setCentered(true).setHasDot(false);

            this.addRenderableWidget(applyBtn);
            this.addRenderableWidget(cancelBtn);
        }
    }

    private void setBooleanValue(boolean value) {
        this.currentBoolean = value;
        if (this.boolTrueBtn != null) {
            this.boolTrueBtn.setActiveThemeHighlight(this.currentBoolean);
        }
        if (this.boolFalseBtn != null) {
            this.boolFalseBtn.setActiveThemeHighlight(!this.currentBoolean);
        }
    }

    private void selectChoiceIndex(int idx) {
        this.currentChoiceIndex = idx;
        for (int i = 0; i < this.choicePills.size(); i++) {
            this.choicePills.get(i).setActiveThemeHighlight(i == idx);
        }
    }

    private double[] calculateRange(String title, String path, double current, boolean isInteger) {
        String token = (title + " " + path).toLowerCase(Locale.ROOT);

        if (token.contains("fov") || token.contains("field of view")) {
            return new double[]{30.0, 110.0, 1.0};
        }
        if (token.contains("render distance") || token.contains("simulation distance")) {
            return new double[]{2.0, 32.0, 1.0};
        }
        if (token.contains("framerate") || token.contains("max fps") || token.contains("fps")) {
            return new double[]{10.0, 260.0, 10.0};
        }
        if (token.contains("gui scale")) {
            return new double[]{0.0, 5.0, 1.0};
        }
        if (token.contains("cave mode depth") || token.contains("depth")) {
            return new double[]{0.0, 64.0, 1.0};
        }
        if (token.contains("toggle timer") || token.contains("timer")) {
            return new double[]{0.0, 5000.0, 50.0};
        }
        if (token.contains("sensitivity") || token.contains("volume") || token.contains("brightness") ||
                token.contains("gamma") || token.contains("opacity") || token.contains("alpha") || token.contains("transparency")) {
            return new double[]{0.0, 1.0, 0.05};
        }

        if (isInteger) {
            if (current >= 0) {
                double max = Math.max(100.0, Math.max(current * 1.5, 10.0));
                return new double[]{0.0, max, 1.0};
            } else {
                double min = current * 2.0;
                double max = Math.max(10.0, Math.abs(current) * 2.0);
                return new double[]{min, max, 1.0};
            }
        } else {
            if (current >= 0.0 && current <= 1.0) {
                return new double[]{0.0, 1.0, 0.05};
            } else {
                double max = Math.max(100.0, current * 1.5);
                return new double[]{0.0, max, 0.1};
            }
        }
    }

    private Component getChoiceLabel() {
        String opt = (currentChoiceIndex >= 0 && currentChoiceIndex < choiceOptions.size())
                ? choiceOptions.get(currentChoiceIndex) : "Unknown";
        return Component.literal("◀  " + opt + "  ▶").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
    }

    private void addStepButton(int x, int y, int w, int h, String label, Runnable action) {
        ThemedButton btn = new ThemedButton(x, y, w, h, Component.literal(label), action)
                .setCentered(true).setHasDot(false);
        this.addRenderableWidget(btn);
    }

    private void adjustInteger(int delta) {
        if (valueInput == null) return;
        try {
            long current = Long.parseLong(valueInput.getValue().trim());
            long next = current + delta;
            valueInput.setValue(String.valueOf(next));
            if (numericSlider != null) {
                numericSlider.ensureRange(next);
                numericSlider.setValue(next, false);
            }
        } catch (Exception ignored) {
            valueInput.setValue(String.valueOf(delta));
            if (numericSlider != null) {
                numericSlider.ensureRange(delta);
                numericSlider.setValue(delta, false);
            }
        }
    }

    private void adjustDouble(double delta) {
        if (valueInput == null) return;
        try {
            double current = Double.parseDouble(valueInput.getValue().trim());
            double next = current + delta;
            double rounded = Math.round(next * 1000.0) / 1000.0;
            valueInput.setValue(String.valueOf(rounded));
            if (numericSlider != null) {
                numericSlider.ensureRange(rounded);
                numericSlider.setValue(rounded, false);
            }
        } catch (Exception ignored) {
            valueInput.setValue(String.valueOf(delta));
            if (numericSlider != null) {
                numericSlider.ensureRange(delta);
                numericSlider.setValue(delta, false);
            }
        }
    }

    private void resetToDefault() {
        if (defaultValue == null) return;
        if (settingType == SettingType.BOOLEAN) {
            setBooleanValue(defaultValue.equalsIgnoreCase("true") || defaultValue.equalsIgnoreCase("on") || defaultValue.equalsIgnoreCase("yes"));
        } else if (settingType == SettingType.CHOICE && !this.choiceOptions.isEmpty()) {
            int idx = this.choiceOptions.indexOf(defaultValue);
            selectChoiceIndex(idx >= 0 ? idx : 0);
            if (this.choiceButton != null) {
                this.choiceButton.setMessage(getChoiceLabel());
            }
        } else if (valueInput != null) {
            valueInput.setValue(defaultValue);
            if (numericSlider != null) {
                try {
                    double d = Double.parseDouble(defaultValue.trim());
                    numericSlider.ensureRange(d);
                    numericSlider.setValue(d, false);
                } catch (Exception ignored) {}
            }
        }
    }

    private void applyChanges() {
        String finalValue;
        if (settingType == SettingType.BOOLEAN) {
            finalValue = String.valueOf(this.currentBoolean);
        } else if (settingType == SettingType.CHOICE && !this.choiceOptions.isEmpty()) {
            finalValue = this.choiceOptions.get(this.currentChoiceIndex);
        } else if (valueInput != null) {
            finalValue = valueInput.getValue().trim();
        } else {
            cancel();
            return;
        }

        try {
            boolean success = true;
            if (this.saveAction != null) {
                success = this.saveAction.save(finalValue);
            }
            if (success) {
                this.result.setValue(finalValue);
                if (this.onValueChanged != null) {
                    this.onValueChanged.accept(finalValue);
                }
                cancel();
            } else {
                this.statusMessage = "Could not apply value: " + finalValue;
                this.statusError = true;
            }
        } catch (Throwable t) {
            this.statusMessage = "Error: " + t.getMessage();
            this.statusError = true;
        }
    }

    private void cancel() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parentScreen);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
            applyChanges();
            return true;
        }
        if (event.key() == InputConstants.KEY_ESCAPE) {
            cancel();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 1. Dark semi-transparent scrim overlay
        graphics.fill(0, 0, this.width, this.height, 0xAA000000);

        SearchTheme theme = SearchTheme.getCurrent();
        Font font = this.font;
        int cardW = 360;
        int cardH = 220;
        int cardX = (this.width - cardW) / 2;
        int cardY = (this.height - cardH) / 2;

        // 2. Main modal card
        GuiRenderUtils.drawRoundedCard(graphics, cardX, cardY, cardW, cardH, theme.getPanelBg(), theme.getPanelBorderTop(), 0x33FFFFFF);

        // 3. Card Header Bar
        int headerH = 26;
        graphics.fill(cardX + 2, cardY + 2, cardX + cardW - 2, cardY + headerH, 0x44000000);
        graphics.fill(cardX + 2, cardY + headerH, cardX + cardW - 2, cardY + headerH + 1, theme.getPanelBorder());

        String headerTitle = "⚙️  Setting Editor  [" + result.getModName() + "]";
        graphics.text(font, headerTitle, cardX + 12, cardY + 9, theme.getHeaderColor());

        // 4. Type Identification Pill Badge (Top Right)
        String typeLabel = settingType.getIcon() + "  " + settingType.getDisplayName();
        int badgeW = font.width(typeLabel) + 12;
        int badgeH = 14;
        int badgeX = cardX + cardW - badgeW - 12;
        int badgeY = cardY + 6;
        int badgeBg = (settingType.getAccentColor() & 0x00FFFFFF) | 0x2A000000;
        GuiRenderUtils.drawRoundedBox(graphics, badgeX, badgeY, badgeW, badgeH, badgeBg, settingType.getAccentColor());
        graphics.text(font, typeLabel, badgeX + 6, badgeY + 3, settingType.getAccentColor());

        // 5. Setting Title & Path
        String displayTitle = font.plainSubstrByWidth(result.getTitle(), cardW - 24);
        graphics.text(font, displayTitle, cardX + 12, cardY + 34, theme.getTitleColor());

        String displayPath = font.plainSubstrByWidth(result.getPath(), cardW - 24);
        graphics.text(font, displayPath, cardX + 12, cardY + 47, theme.getMutedTextColor());

        // 6. Containers and helper text based on setting type
        int contentY = cardY + 74;

        if (settingType == SettingType.BOOLEAN) {
            // Status feedback below buttons
            String statusText = currentBoolean ? "● Current State: ENABLED (true)" : "○ Current State: DISABLED (false)";
            int statusTextColor = currentBoolean ? 0xFF10B981 : 0xFFEF4444;
            graphics.centeredText(font, statusText, cardX + cardW / 2, contentY + 42, statusTextColor);

        } else if (settingType == SettingType.INTEGER || settingType == SettingType.DOUBLE) {
            // Rounded background box behind number input box
            int inputW = 74;
            int inputX = cardX + cardW - 24 - inputW;
            int rowH = 22;
            GuiRenderUtils.drawRoundedBox(graphics, inputX, contentY, inputW, rowH, theme.getSearchBg(), theme.getSearchBorderFocus());

        } else if (settingType == SettingType.STRING) {
            // Full width input box background
            int inputBgX = cardX + 20;
            int inputBgW = cardW - 40;
            int inputBgH = 24;
            GuiRenderUtils.drawRoundedBox(graphics, inputBgX, contentY + 2, inputBgW, inputBgH, theme.getSearchBg(), theme.getSearchBorderFocus());
        }

        // 7. Status / error message
        if (this.statusMessage != null) {
            int statusColor = statusError ? 0xFFEF4444 : 0xFF22C55E;
            graphics.centeredText(font, this.statusMessage, this.width / 2, cardY + cardH - 50, statusColor);
        }

        // 8. Render children widgets
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
