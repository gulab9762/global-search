package com.igxxd.globalsearch.gui;

import com.igxxd.globalsearch.search.SearchResult;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Universal modal editor for settings across all mods and subsystems.
 * Supports booleans, integers, decimals, enum/choice lists, and strings.
 */
public class GlobalSettingEditScreen extends Screen {

    public enum SettingType {
        BOOLEAN,
        INTEGER,
        DOUBLE,
        CHOICE,
        STRING
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

    // UI state
    private boolean currentBoolean;
    private int currentChoiceIndex = 0;
    private EditBox valueInput;
    private ThemedButton boolToggleButton;
    private ThemedButton choiceButton;
    private String statusMessage = null;
    private boolean statusError = false;

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
                || rawVal.equalsIgnoreCase("on") || rawVal.equalsIgnoreCase("off")) {
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
            this.currentBoolean = rawVal.equalsIgnoreCase("true") || rawVal.equalsIgnoreCase("on");
        } else if (this.settingType == SettingType.CHOICE && !this.choiceOptions.isEmpty()) {
            int foundIdx = this.choiceOptions.indexOf(rawVal);
            this.currentChoiceIndex = foundIdx >= 0 ? foundIdx : 0;
        }
    }

    private static boolean isInteger(String s) {
        try {
            Long.parseLong(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void init() {
        int cardW = 340;
        int cardH = 210;
        int cardX = (this.width - cardW) / 2;
        int cardY = (this.height - cardH) / 2;

        SearchTheme theme = SearchTheme.getCurrent();
        String initialVal = result.getValue() != null ? result.getValue() : "";

        int contentY = cardY + 70;

        if (this.settingType == SettingType.BOOLEAN) {
            this.boolToggleButton = new ThemedButton(
                    cardX + 24, contentY + 10, cardW - 48, 28,
                    getBoolLabel(),
                    () -> {
                        this.currentBoolean = !this.currentBoolean;
                        if (this.boolToggleButton != null) {
                            this.boolToggleButton.setMessage(getBoolLabel());
                            this.boolToggleButton.setActiveThemeHighlight(this.currentBoolean);
                        }
                    }
            ).setCentered(true).setHasDot(false).setActiveThemeHighlight(this.currentBoolean);
            this.addRenderableWidget(this.boolToggleButton);
        } else if (this.settingType == SettingType.CHOICE && !this.choiceOptions.isEmpty()) {
            this.choiceButton = new ThemedButton(
                    cardX + 24, contentY + 10, cardW - 48, 28,
                    getChoiceLabel(),
                    () -> {
                        this.currentChoiceIndex = (this.currentChoiceIndex + 1) % this.choiceOptions.size();
                        if (this.choiceButton != null) {
                            this.choiceButton.setMessage(getChoiceLabel());
                        }
                    }
            ).setCentered(true).setHasDot(false).setActiveThemeHighlight(true);
            this.addRenderableWidget(this.choiceButton);
        } else {
            // Text or Numeric Input
            int inputH = 18;
            this.valueInput = new EditBox(this.font, cardX + 26, contentY + 4, cardW - 52, inputH, Component.literal("Value"));
            this.valueInput.setBordered(false);
            this.valueInput.setTextColor(theme.getTitleColor());
            this.valueInput.setValue(initialVal);
            this.addRenderableWidget(this.valueInput);
            this.setInitialFocus(this.valueInput);

            // Step adjustment buttons for numeric types
            if (this.settingType == SettingType.INTEGER) {
                int btnW = 38;
                int btnH = 18;
                int stepY = contentY + 28;
                int startX = cardX + (cardW - (btnW * 4 + 18)) / 2;

                addStepButton(startX, stepY, btnW, btnH, "-10", () -> adjustInteger(-10));
                addStepButton(startX + btnW + 6, stepY, btnW, btnH, "-1", () -> adjustInteger(-1));
                addStepButton(startX + (btnW + 6) * 2, stepY, btnW, btnH, "+1", () -> adjustInteger(1));
                addStepButton(startX + (btnW + 6) * 3, stepY, btnW, btnH, "+10", () -> adjustInteger(10));
            } else if (this.settingType == SettingType.DOUBLE) {
                int btnW = 44;
                int btnH = 18;
                int stepY = contentY + 28;
                int startX = cardX + (cardW - (btnW * 4 + 18)) / 2;

                addStepButton(startX, stepY, btnW, btnH, "-1.0", () -> adjustDouble(-1.0));
                addStepButton(startX + btnW + 6, stepY, btnW, btnH, "-0.1", () -> adjustDouble(-0.1));
                addStepButton(startX + (btnW + 6) * 2, stepY, btnW, btnH, "+0.1", () -> adjustDouble(0.1));
                addStepButton(startX + (btnW + 6) * 3, stepY, btnW, btnH, "+1.0", () -> adjustDouble(1.0));
            }
        }

        // Bottom action buttons: Apply, Reset (if available), Cancel
        int btnY = cardY + cardH - 36;
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

    private Component getBoolLabel() {
        return this.currentBoolean
                ? Component.literal("●  ENABLED  (true)").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                : Component.literal("○  DISABLED (false)").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
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
            valueInput.setValue(String.valueOf(current + delta));
        } catch (Exception ignored) {
            valueInput.setValue(String.valueOf(delta));
        }
    }

    private void adjustDouble(double delta) {
        if (valueInput == null) return;
        try {
            double current = Double.parseDouble(valueInput.getValue().trim());
            double next = current + delta;
            // Round to 3 decimal places to prevent floating point drift
            double rounded = Math.round(next * 1000.0) / 1000.0;
            valueInput.setValue(String.valueOf(rounded));
        } catch (Exception ignored) {
            valueInput.setValue(String.valueOf(delta));
        }
    }

    private void resetToDefault() {
        if (defaultValue == null) return;
        if (settingType == SettingType.BOOLEAN) {
            this.currentBoolean = defaultValue.equalsIgnoreCase("true") || defaultValue.equalsIgnoreCase("on");
            if (this.boolToggleButton != null) {
                this.boolToggleButton.setMessage(getBoolLabel());
                this.boolToggleButton.setActiveThemeHighlight(this.currentBoolean);
            }
        } else if (settingType == SettingType.CHOICE && !this.choiceOptions.isEmpty()) {
            int idx = this.choiceOptions.indexOf(defaultValue);
            this.currentChoiceIndex = idx >= 0 ? idx : 0;
            if (this.choiceButton != null) {
                this.choiceButton.setMessage(getChoiceLabel());
            }
        } else if (valueInput != null) {
            valueInput.setValue(defaultValue);
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
        int cardW = 340;
        int cardH = 210;
        int cardX = (this.width - cardW) / 2;
        int cardY = (this.height - cardH) / 2;

        // 2. Main modal card
        GuiRenderUtils.drawRoundedCard(graphics, cardX, cardY, cardW, cardH, theme.getPanelBg(), theme.getPanelBorderTop(), 0x33FFFFFF);

        // 3. Card Header Bar
        int headerH = 26;
        graphics.fill(cardX + 2, cardY + 2, cardX + cardW - 2, cardY + headerH, 0x44000000);
        graphics.fill(cardX + 2, cardY + headerH, cardX + cardW - 2, cardY + headerH + 1, theme.getPanelBorder());

        String headerTitle = "⚙️  Setting Editor  [" + result.getModName() + "]";
        graphics.text(this.font, headerTitle, cardX + 12, cardY + 9, theme.getHeaderColor());

        // 4. Setting Title & Path
        String displayTitle = this.font.plainSubstrByWidth(result.getTitle(), cardW - 24);
        graphics.text(this.font, displayTitle, cardX + 12, cardY + 32, theme.getTitleColor());

        String displayPath = this.font.plainSubstrByWidth(result.getPath(), cardW - 24);
        graphics.text(this.font, displayPath, cardX + 12, cardY + 45, theme.getMutedTextColor());

        // 5. Input Box Background for string/numeric
        if (settingType != SettingType.BOOLEAN && (settingType != SettingType.CHOICE || choiceOptions.isEmpty())) {
            int inputBgX = cardX + 20;
            int inputBgY = cardY + 70;
            int inputBgW = cardW - 40;
            int inputBgH = 24;
            GuiRenderUtils.drawRoundedBox(graphics, inputBgX, inputBgY, inputBgW, inputBgH, theme.getSearchBg(), theme.getSearchBorderFocus());
        }

        // 6. Status message
        if (this.statusMessage != null) {
            int statusColor = statusError ? 0xFFEF4444 : 0xFF22C55E;
            graphics.centeredText(this.font, this.statusMessage, this.width / 2, cardY + cardH - 52, statusColor);
        }

        // 7. Render children widgets
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
