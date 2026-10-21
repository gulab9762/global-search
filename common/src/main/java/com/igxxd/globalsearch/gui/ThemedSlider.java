package com.igxxd.globalsearch.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Custom draggable slider bar matching the active SearchTheme.
 * Provides smooth dragging, live value updates, thematic styling, and range indicators.
 */
public class ThemedSlider extends AbstractWidget {

    private double min;
    private double max;
    private final double step;
    private final boolean isInteger;
    private double currentValue;
    private final Consumer<Double> onValueChange;
    private boolean dragging = false;

    public ThemedSlider(int x, int y, int width, int height,
                        double min, double max, double initialValue, double step, boolean isInteger,
                        Consumer<Double> onValueChange) {
        super(x, y, width, height, Component.empty());
        this.min = min;
        this.max = Math.max(min + 0.0001, max);
        this.step = step > 0 ? step : (isInteger ? 1.0 : 0.1);
        this.isInteger = isInteger;
        this.currentValue = Mth.clamp(initialValue, min, max);
        this.onValueChange = onValueChange;
    }

    public double getValue() {
        return this.currentValue;
    }

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }

    public void setRange(double min, double max) {
        this.min = min;
        this.max = Math.max(min + 0.0001, max);
    }

    public void ensureRange(double val) {
        if (val < this.min) {
            this.min = Math.floor(val);
        }
        if (val > this.max) {
            this.max = Math.ceil(val);
        }
    }

    public void setValue(double value, boolean notify) {
        ensureRange(value);
        double clamped = Mth.clamp(value, min, max);
        if (this.isInteger) {
            clamped = Math.round(clamped);
        } else {
            clamped = Math.round(clamped / step) * step;
            clamped = Math.round(clamped * 1000.0) / 1000.0;
        }

        if (Double.compare(this.currentValue, clamped) != 0) {
            this.currentValue = clamped;
            if (notify && this.onValueChange != null) {
                this.onValueChange.accept(this.currentValue);
            }
        }
    }

    private void updateFromMouse(double mouseX) {
        int thumbW = 12;
        int trackX = this.getX() + thumbW / 2;
        int trackW = this.getWidth() - thumbW;
        if (trackW <= 0) return;

        double ratio = (mouseX - trackX) / (double) trackW;
        ratio = Mth.clamp(ratio, 0.0, 1.0);

        double rawVal = min + ratio * (max - min);
        setValue(rawVal, true);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        playButtonClickSound(Minecraft.getInstance().getSoundManager());
        this.dragging = true;
        updateFromMouse(event.x());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.active && this.visible && this.isHovered()) {
            this.onClick(event, doubleClick);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.active && this.visible && this.dragging) {
            updateFromMouse(event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        SearchTheme theme = SearchTheme.getCurrent();
        Font font = Minecraft.getInstance().font;

        int thumbW = 12;
        int thumbH = h;
        int trackH = 6;
        int trackY = y + (h - trackH) / 2;
        int trackX = x + thumbW / 2;
        int trackW = w - thumbW;

        double ratio = (max > min) ? Mth.clamp((currentValue - min) / (max - min), 0.0, 1.0) : 0.0;
        int thumbX = (int) (trackX + ratio * trackW) - (thumbW / 2);

        // 1. Inactive track background
        GuiRenderUtils.drawRoundedBox(graphics, trackX - 2, trackY, trackW + 4, trackH, theme.getSearchBg(), theme.getPanelBorder());

        // 2. Filled track portion (left of thumb)
        int fillW = (int) (ratio * trackW);
        if (fillW > 0) {
            GuiRenderUtils.drawRoundedBox(graphics, trackX - 2, trackY, fillW + 4, trackH, theme.getActiveBtnGradTop(), theme.getActiveBtnBorder());
        }

        // 3. Thumb Handle
        boolean thumbHovered = (mouseX >= thumbX && mouseX <= thumbX + thumbW && mouseY >= y && mouseY <= y + thumbH) || dragging;
        int thumbBg = thumbHovered ? theme.getCardHoverBg() : theme.getCardBg();
        int thumbBorder = (thumbHovered || dragging) ? theme.getAccentColor() : theme.getActiveBtnBorder();

        // Draw thumb handle with rounded corners and specular top highlight
        GuiRenderUtils.drawRoundedCard(graphics, thumbX, y, thumbW, thumbH, thumbBg, thumbBorder, 0x44FFFFFF);

        // Thumb center vertical grip mark
        int gripColor = thumbHovered ? theme.getAccentColor() : theme.getMutedTextColor();
        graphics.fill(thumbX + thumbW / 2, y + 4, thumbX + thumbW / 2 + 1, y + thumbH - 4, gripColor);

        // 4. Min / Max indicators below track
        String minStr = isInteger ? String.valueOf((long) min) : String.format(Locale.ROOT, "%.1f", min);
        String maxStr = isInteger ? String.valueOf((long) max) : String.format(Locale.ROOT, "%.1f", max);
        graphics.text(font, minStr, trackX - 2, y + h + 2, theme.getMutedTextColor());
        int maxW = font.width(maxStr);
        graphics.text(font, maxStr, trackX + trackW + 2 - maxW, y + h + 2, theme.getMutedTextColor());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
