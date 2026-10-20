package com.igxxd.globalsearch.gui;

import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.function.Consumer;

public class SearchResultList extends ObjectSelectionList<SearchResultList.ResultEntry> {

    private final Consumer<SearchResult> onSelected;

    public SearchResultList(Minecraft minecraft, int width, int height, int y, int itemHeight, Consumer<SearchResult> onSelected) {
        super(minecraft, width, height, y, itemHeight);
        this.onSelected = onSelected;
    }

    @Override
    public int getRowWidth() {
        return Math.max(this.width - 16, 180);
    }

    public void setResults(Collection<SearchResult> results) {
        this.clearEntries();
        for (SearchResult result : results) {
            this.addEntry(new ResultEntry(result, this.minecraft.font, this.onSelected));
        }
        this.setScrollAmount(0);
    }

    public static class ResultEntry extends ObjectSelectionList.Entry<ResultEntry> {

        private final SearchResult result;
        private final Font font;
        private final Consumer<SearchResult> onSelected;

        public ResultEntry(SearchResult result, Font font, Consumer<SearchResult> onSelected) {
            this.result = result;
            this.font = font;
            this.onSelected = onSelected;
        }

        public SearchResult getResult() {
            return result;
        }

        @Override
        public Component getNarration() {
            return Component.literal(result.getTitle() + ", " + result.getType().getLabel());
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            int width = this.getWidth();
            int height = this.getHeight();

            // Background card with hover highlight
            if (isHovered || isFocused()) {
                graphics.fill(x, y + 1, x + width, y + height - 1, 0x33FFFFFF);
            } else {
                graphics.fill(x, y + 1, x + width, y + height - 1, 0x14000000);
            }

            // 1. Left Type Badge (e.g. [KEY], [SETTING], [ITEM], [MOD])
            SearchResultType type = result.getType();
            int badgeWidth = 46;
            int badgeHeight = height - 8;
            graphics.fill(x + 4, y + 4, x + 4 + badgeWidth, y + 4 + badgeHeight, type.getBadgeBackgroundColor());
            graphics.centeredText(font, type.getShortLabel(), x + 4 + (badgeWidth / 2), y + (height - 8) / 2, type.getTextColor());

            int curX = x + badgeWidth + 10;

            // 2. Icon (for items/blocks)
            if (result.hasIcon()) {
                graphics.item(result.getIcon(), curX, y + (height - 16) / 2);
                curX += 20;
            }

            int availableWidth = (x + width) - curX - 10;

            // 3. Line 1: Title + Value
            int line1Y = y + 5;
            String title = result.getTitle();
            int maxTitleWidth = Math.max(availableWidth / 2, 160);
            String displayTitle = font.plainSubstrByWidth(title, maxTitleWidth);
            graphics.text(font, displayTitle, curX, line1Y, 0xFFFFFFFF);

            int titleRenderWidth = font.width(displayTitle);
            if (!result.getValue().isEmpty()) {
                String valStr = "[" + result.getValue() + "]";
                int maxValWidth = availableWidth - titleRenderWidth - 12;
                if (maxValWidth > 20) {
                    String displayVal = font.plainSubstrByWidth(valStr, maxValWidth);
                    graphics.text(font, displayVal, curX + titleRenderWidth + 8, line1Y, type.getValueColor());
                }
            }

            // 4. Line 2: Mod Name + Path / Category
            int line2Y = y + 19;
            String modText = "Mod: " + result.getModName();
            graphics.text(font, modText, curX, line2Y, 0xFF77A3FF);
            int modRenderWidth = font.width(modText);

            int pathStartX = curX + modRenderWidth;
            int pathAvailableWidth = (x + width) - pathStartX - 10;

            if (!result.getPath().isEmpty() && pathAvailableWidth > 30) {
                String pathText = " • " + result.getPath();
                String displayPath = font.plainSubstrByWidth(pathText, pathAvailableWidth);
                graphics.text(font, displayPath, pathStartX, line2Y, 0xFFAAAAAA);
            }

            // Tooltip when hovered
            if (isHovered) {
                graphics.setComponentTooltipForNextFrame(font, result.getTooltip(), mouseX, mouseY);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (onSelected != null) {
                onSelected.accept(result);
                return true;
            }
            return false;
        }
    }
}
