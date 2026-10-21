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
 * Custom styled rounded button component dynamically supporting multiple themes.
 * Supports active glows, metallic and neon borders, rounded corners, icons, and centered mode.
 */
public class ThemedButton extends AbstractWidget {

    private final Runnable onPress;
    private boolean activeThemeHighlight = false;
    private boolean hasDot = true;
    private boolean centered = false;
    private String iconPrefix = null;
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

    public ThemedButton setIconPrefix(String iconPrefix) {
        this.iconPrefix = iconPrefix;
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
        SearchTheme theme = SearchTheme.getCurrent();

        if (activeThemeHighlight) {
            // Active / Selected state with rounded corners & top gradient
            GuiRenderUtils.drawRoundedCard(graphics, x, y, w, h, theme.getActiveBtnGradTop(), theme.getActiveBtnBorder(), 0x33FFFFFF);

            if (centered) {
                graphics.centeredText(font, this.getMessage(), x + (w / 2), y + (h - 8) / 2, theme.getActiveBtnText());
            } else {
                int textStartX = x + 6;
                if (iconPrefix != null && !iconPrefix.isEmpty()) {
                    graphics.text(font, iconPrefix, textStartX, y + (h - 8) / 2, theme.getActiveBtnText());
                    textStartX += font.width(iconPrefix) + 4;
                } else if (hasDot) {
                    graphics.text(font, "●", textStartX, y + (h - 8) / 2, theme.getActiveBtnText());
                    textStartX += 10;
                }
                graphics.text(font, this.getMessage(), textStartX, y + (h - 8) / 2, theme.getActiveBtnText());
            }
        } else if (hovered) {
            // Hovered state
            GuiRenderUtils.drawRoundedCard(graphics, x, y, w, h, theme.getCardHoverBg(), theme.getCardBorderHover(), 0x22FFFFFF);

            if (centered) {
                graphics.centeredText(font, this.getMessage(), x + (w / 2), y + (h - 8) / 2, 0xFFFFFFFF);
            } else {
                int textStartX = x + 6;
                if (iconPrefix != null && !iconPrefix.isEmpty()) {
                    graphics.text(font, iconPrefix, textStartX, y + (h - 8) / 2, theme.getAccentColor());
                    textStartX += font.width(iconPrefix) + 4;
                } else if (hasDot) {
                    graphics.text(font, "○", textStartX, y + (h - 8) / 2, theme.getCardBorderHover());
                    textStartX += 10;
                }
                graphics.text(font, this.getMessage(), textStartX, y + (h - 8) / 2, 0xFFFFF0D0);
            }
        } else {
            // Normal inactive state
            GuiRenderUtils.drawRoundedBox(graphics, x, y, w, h, theme.getCardBg(), theme.getPanelBorder());

            if (centered) {
                graphics.centeredText(font, this.getMessage(), x + (w / 2), y + (h - 8) / 2, theme.getMutedTextColor());
            } else {
                int textStartX = x + 6;
                if (iconPrefix != null && !iconPrefix.isEmpty()) {
                    graphics.text(font, iconPrefix, textStartX, y + (h - 8) / 2, theme.getMutedTextColor());
                    textStartX += font.width(iconPrefix) + 4;
                } else if (hasDot) {
                    graphics.text(font, "○", textStartX, y + (h - 8) / 2, theme.getCardBorder());
                    textStartX += 10;
                }
                graphics.text(font, this.getMessage(), textStartX, y + (h - 8) / 2, theme.getMutedTextColor());
            }
        }

        // Optional badge count on right
        if (badgeCount >= 0) {
            String countStr = String.valueOf(badgeCount);
            int countW = font.width(countStr);
            int badgeX = x + w - countW - 6;
            int badgeY = y + (h - 8) / 2;
            int color = activeThemeHighlight ? theme.getActiveBtnText() : (hovered ? theme.getAccentColor() : theme.getMutedTextColor());
            graphics.text(font, countStr, badgeX, badgeY, color);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
