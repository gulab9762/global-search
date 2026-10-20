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

            // Background highlight on hover or focus
            if (isHovered || isFocused()) {
                graphics.fill(x, y, x + width, y + height, 0x33FFFFFF);
            } else {
                graphics.fill(x, y, x + width, y + height, 0x12000000);
            }

            // 1. Type Badge (e.g. [KEY], [SET], [ITEM], [MOD])
            SearchResultType type = result.getType();
            int badgeWidth = 46;
            graphics.fill(x + 2, y + 3, x + 2 + badgeWidth, y + height - 3, type.getBadgeBackgroundColor());
            graphics.centeredText(font, type.getShortLabel(), x + 2 + (badgeWidth / 2), y + 7, type.getTextColor());

            int curX = x + badgeWidth + 8;

            // 2. Icon (for items/blocks)
            if (result.hasIcon()) {
                graphics.item(result.getIcon(), curX, y + (height - 16) / 2);
                curX += 18;
            }

            // 3. Title
            String title = result.getTitle();
            int maxTitleWidth = 150;
            String trimmedTitle = font.plainSubstrByWidth(title, maxTitleWidth);
            graphics.text(font, trimmedTitle, curX, y + 7, 0xFFFFFFFF);
            curX += font.width(trimmedTitle) + 8;

            // 4. Value
            if (!result.getValue().isEmpty()) {
                String valStr = "[" + result.getValue() + "]";
                int maxValWidth = 140;
                String trimmedVal = font.plainSubstrByWidth(valStr, maxValWidth);
                graphics.text(font, trimmedVal, curX, y + 7, type.getValueColor());
                curX += font.width(trimmedVal) + 8;
            }

            // 5. Path / Category
            int modTagWidth = font.width(result.getModName()) + 10;
            int rightEdge = x + width - modTagWidth - 8;
            if (!result.getPath().isEmpty() && curX < rightEdge - 20) {
                int availablePathWidth = rightEdge - curX - 6;
                String trimmedPath = font.plainSubstrByWidth(result.getPath(), availablePathWidth);
                graphics.text(font, "• " + trimmedPath, curX, y + 7, 0xFFAAAAAA);
            }

            // 6. Mod Name Badge
            int modX = x + width - modTagWidth - 4;
            graphics.fill(modX, y + 4, modX + modTagWidth, y + height - 4, 0x334466FF);
            graphics.text(font, result.getModName(), modX + 5, y + 7, 0xFF99BBFF);

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
