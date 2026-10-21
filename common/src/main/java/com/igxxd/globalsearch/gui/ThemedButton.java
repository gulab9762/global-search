package com.igxxd.globalsearch.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Custom styled button component implementing Theme 2: Deep Netherite and Ancient Debris (RPG Aesthetic).
 * Supports active ember glows, metallic bronze borders, glowing dot indicators, and centered mode.
 */
public class ThemedButton extends AbstractWidget {

    private final Runnable onPress;
    private boolean activeThemeHighlight = false;
    private boolean hasDot = true;
    private boolean centered = false;
    private int badgeCount = -1;

    public ThemedButton(int x, int y, int width, int height, Component message, Runnable onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    public ThemedButton setActiveThemeHighlight(boolean highlight) {
        this.activeThemeHighlight = highlight;
        return this;
    }

    public ThemedButton setHasDot(boolean hasDot) {
        this.hasDot = hasDot;
        return this;
    }

    public ThemedButton setCentered(boolean centered) {
        this.centered = centered;
        return this;
    }

    public ThemedButton setBadgeCount(int count) {
        this.badgeCount = count;
        return this;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        playButtonClickSound(Minecraft.getInstance().getSoundManager());
        if (this.onPress != null) {
            this.onPress.run();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.active && this.visible && this.isFocused()) {
            if (event.key() == 257 || event.key() == 335 || event.key() == 32) { // ENTER, KP_ENTER, SPACE
                playButtonClickSound(Minecraft.getInstance().getSoundManager());
                if (this.onPress != null) {
                    this.onPress.run();
                }
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();
        Font font = Minecraft.getInstance().font;
        boolean hovered = this.isHovered();

        // Theme 2: Deep Netherite & Ancient Debris Button Styling
        if (activeThemeHighlight) {
            // Active Category / Pressed Button State: Rich Ancient Debris Ember Gradient
            graphics.fillGradient(x, y, x + w, y + h, 0xFF442A22, 0xFF2D1C17);
            // Outer Border: Rich Bronze / Amber
            graphics.fill(x, y, x + w, y + 1, 0xFFB45309);
            graphics.fill(x, y + h - 1, x + w, y + h, 0xFF78350F);
            graphics.fill(x, y, x + 1, y + h, 0xFFB45309);
            graphics.fill(x + w - 1, y, x + w, y + h, 0xFF78350F);
            // 1px inner specular top highlight
            graphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0x33FFFFFF);

            if (centered) {
                graphics.centeredText(font, this.getMessage(), x + (w / 2), y + (h - 8) / 2, 0xFFFBBF24);
            } else {
                if (hasDot) {
                    graphics.text(font, "●", x + 5, y + (h - 8) / 2, 0xFFFBBF24);
                    graphics.text(font, this.getMessage(), x + 16, y + (h - 8) / 2, 0xFFFBBF24);
                } else {
                    graphics.text(font, this.getMessage(), x + 6, y + (h - 8) / 2, 0xFFFBBF24);
                }
            }
        } else if (hovered) {
            // Hovered State: Netherite Slate with Ancient Debris Bronze Highlight
            graphics.fill(x, y, x + w, y + h, 0xFF362620);
            graphics.fill(x, y, x + w, y + 1, 0xFF8C5738);
            graphics.fill(x, y + h - 1, x + w, y + h, 0xFF4D362C);
            graphics.fill(x, y, x + 1, y + h, 0xFF8C5738);
            graphics.fill(x + w - 1, y, x + w, y + h, 0xFF4D362C);
            // 1px inner highlight
            graphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0x22FFFFFF);

            if (centered) {
                graphics.centeredText(font, this.getMessage(), x + (w / 2), y + (h - 8) / 2, 0xFFFFF0D0);
            } else {
                if (hasDot) {
                    graphics.text(font, "○", x + 5, y + (h - 8) / 2, 0xFF8C5738);
                    graphics.text(font, this.getMessage(), x + 16, y + (h - 8) / 2, 0xFFFFF0D0);
                } else {
                    graphics.text(font, this.getMessage(), x + 6, y + (h - 8) / 2, 0xFFFFF0D0);
                }
            }
        } else {
            // Normal Inactive State: Deep Charcoal with Subtle Border
            graphics.fill(x, y, x + w, y + h, 0xCC1E191D);
            graphics.fill(x, y, x + w, y + 1, 0xFF3D2C25);
            graphics.fill(x, y + h - 1, x + w, y + h, 0xFF261B16);
            graphics.fill(x, y, x + 1, y + h, 0xFF3D2C25);
            graphics.fill(x + w - 1, y, x + w, y + h, 0xFF261B16);

            if (centered) {
                graphics.centeredText(font, this.getMessage(), x + (w / 2), y + (h - 8) / 2, 0xFFA89F91);
            } else {
                if (hasDot) {
                    graphics.text(font, "○", x + 5, y + (h - 8) / 2, 0xFF5A3D31);
                    graphics.text(font, this.getMessage(), x + 16, y + (h - 8) / 2, 0xFFA89F91);
                } else {
                    graphics.text(font, this.getMessage(), x + 6, y + (h - 8) / 2, 0xFFA89F91);
                }
            }
        }

        // Optional badge count on right
        if (badgeCount >= 0) {
            String countStr = String.valueOf(badgeCount);
            int countW = font.width(countStr);
            int badgeX = x + w - countW - 7;
            int badgeY = y + (h - 8) / 2;
            int color = activeThemeHighlight ? 0xFFFBBF24 : (hovered ? 0xFFD97706 : 0xFF6B584E);
            graphics.text(font, countStr, badgeX, badgeY, color);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
