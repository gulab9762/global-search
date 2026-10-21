package com.igxxd.globalsearch.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Utility for drawing pixel-perfect rounded boxes, cards, and pills in Minecraft GuiGraphics.
 */
public class GuiRenderUtils {

    /**
     * Draws a beveled / rounded rectangle with a 1px border.
     * Cuts off the 4 corner pixels to create a smooth, modern rounded corner in Minecraft GUI.
     */
    public static void drawRoundedBox(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int fillColor, int borderColor) {
        if (w <= 2 || h <= 2) {
            graphics.fill(x, y, x + w, y + h, fillColor);
            return;
        }

        // Inner fill
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, fillColor);

        // Borders without the 4 corner pixels
        graphics.fill(x + 1, y, x + w - 1, y + 1, borderColor); // Top
        graphics.fill(x + 1, y + h - 1, x + w - 1, y + h, borderColor); // Bottom
        graphics.fill(x, y + 1, x + 1, y + h - 1, borderColor); // Left
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, borderColor); // Right
    }

    /**
     * Draws a rounded card with an inner specular line along the top edge for a sleek glass/metallic effect.
     */
    public static void drawRoundedCard(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int fillColor, int borderColor, int specularColor) {
        drawRoundedBox(graphics, x, y, w, h, fillColor, borderColor);
        if (w > 4 && h > 4 && specularColor != 0) {
            graphics.fill(x + 2, y + 1, x + w - 2, y + 2, specularColor);
        }
    }
}
