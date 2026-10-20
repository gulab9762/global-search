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

    public void refreshLayout() {
        this.refreshScrollAmount();
        this.setScrollAmount(this.scrollAmount());
    }

    @Override
    public int getRowWidth() {
        return Math.max(this.width - 16, 180);
    }

    public void setResults(Collection<SearchResult> results) {
        this.clearEntries();
        for (SearchResult result : results) {
            this.addEntry(new ResultEntry(this, result, this.minecraft.font, this.onSelected));
        }
        this.setScrollAmount(0);
    }

    public static class ResultEntry extends ObjectSelectionList.Entry<ResultEntry> {

        private final SearchResultList list;
        private final SearchResult result;
        private final Font font;
        private final Consumer<SearchResult> onSelected;
        private boolean expanded = false;

        public ResultEntry(SearchResultList list, SearchResult result, Font font, Consumer<SearchResult> onSelected) {
            this.list = list;
            this.result = result;
            this.font = font;
            this.onSelected = onSelected;
            this.setHeight(38);
        }

        public SearchResult getResult() {
            return result;
        }

        public boolean isExpanded() {
            return expanded;
        }

        public void toggleExpanded() {
            this.expanded = !this.expanded;
            if (this.expanded) {
                int contentWidth = Math.max(this.getWidth() - 70, 100);
                String desc = result.getDescription();
                var lines = font.split(Component.literal(desc), contentWidth);
                int lineCount = Math.max(1, lines.size());
                this.setHeight(52 + lineCount * 11 + 6);
            } else {
                this.setHeight(38);
            }
            this.list.refreshLayout();
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

            int expandBtnSize = 18;
            int expandBtnX = x + width - expandBtnSize - 6;
            int expandBtnY = y + 8;
            boolean isBtnHovered = mouseX >= expandBtnX && mouseX <= expandBtnX + expandBtnSize && mouseY >= expandBtnY && mouseY <= expandBtnY + expandBtnSize;

            // Background card with hover highlight
            if ((isHovered && !isBtnHovered) || isFocused()) {
                graphics.fill(x, y + 1, x + width, y + height - 1, 0x33FFFFFF);
            } else {
                graphics.fill(x, y + 1, x + width, y + height - 1, 0x14000000);
            }

            // 1. Left Type Badge (e.g. [KEY], [SETTING], [ITEM], [MOD])
            SearchResultType type = result.getType();
            int badgeWidth = 46;
            int badgeHeight = expanded ? 28 : height - 8;
            graphics.fill(x + 4, y + 4, x + 4 + badgeWidth, y + 4 + badgeHeight, type.getBadgeBackgroundColor());
            graphics.centeredText(font, type.getShortLabel(), x + 4 + (badgeWidth / 2), y + 4 + (badgeHeight - 8) / 2, type.getTextColor());

            int curX = x + badgeWidth + 10;

            // 2. Icon (for items/blocks)
            if (result.hasIcon()) {
                graphics.item(result.getIcon(), curX, y + 10);
                curX += 20;
            }

            int availableWidth = (expandBtnX - 6) - curX;

            // 3. Line 1: Title + Value
            int line1Y = y + 5;
            String title = result.getTitle();
            int maxTitleWidth = Math.max(availableWidth / 2, 140);
            String displayTitle = font.plainSubstrByWidth(title, maxTitleWidth);
            graphics.text(font, displayTitle, curX, line1Y, 0xFFFFFFFF);

            int titleRenderWidth = font.width(displayTitle);
            if (!result.getValue().isEmpty()) {
                String valStr = "[" + result.getValue() + "]";
                int maxValWidth = availableWidth - titleRenderWidth - 8;
                if (maxValWidth > 20) {
                    String displayVal = font.plainSubstrByWidth(valStr, maxValWidth);
                    graphics.text(font, displayVal, curX + titleRenderWidth + 6, line1Y, type.getValueColor());
                }
            }

            // 4. Line 2: Mod Name + Path / Category
            int line2Y = y + 19;
            String modText = "Mod: " + result.getModName();
            graphics.text(font, modText, curX, line2Y, 0xFF77A3FF);
            int modRenderWidth = font.width(modText);

            int pathStartX = curX + modRenderWidth;
            int pathAvailableWidth = (expandBtnX - 6) - pathStartX;

            if (!result.getPath().isEmpty() && pathAvailableWidth > 30) {
                String pathText = " • " + result.getPath();
                String displayPath = font.plainSubstrByWidth(pathText, pathAvailableWidth);
                graphics.text(font, displayPath, pathStartX, line2Y, 0xFFAAAAAA);
            }

            // 5. Expand / Collapse Button
            graphics.fill(expandBtnX, expandBtnY, expandBtnX + expandBtnSize, expandBtnY + expandBtnSize, isBtnHovered ? 0x44FFFFFF : 0x1AFFFFFF);
            String arrow = expanded ? "▲" : "▼";
            graphics.centeredText(font, arrow, expandBtnX + (expandBtnSize / 2), expandBtnY + (expandBtnSize - 9) / 2, isBtnHovered ? 0xFFFFFFFF : 0xFFAAAAAA);

            // 6. Expanded Description Area
            if (expanded) {
                int dividerY = y + 36;
                graphics.fill(curX, dividerY, expandBtnX + expandBtnSize, dividerY + 1, 0x33FFFFFF);

                int descLabelY = dividerY + 5;
                graphics.text(font, "Description:", curX, descLabelY, 0xFFFFAA00);

                int descWidth = Math.max((expandBtnX + expandBtnSize) - curX, 100);
                var lines = font.split(Component.literal(result.getDescription()), descWidth);
                int lineY = descLabelY + 12;
                for (var line : lines) {
                    graphics.text(font, line, curX, lineY, 0xFFE0E0E0);
                    lineY += 11;
                }
            }

            // Tooltips
            if (isBtnHovered) {
                graphics.setComponentTooltipForNextFrame(font, java.util.List.of(Component.literal(expanded ? "Collapse description" : "Expand description")), mouseX, mouseY);
            } else if (isHovered) {
                graphics.setComponentTooltipForNextFrame(font, result.getTooltip(), mouseX, mouseY);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            double mx = event.x();
            double my = event.y();

            int expandBtnSize = 18;
            int expandBtnX = this.getX() + this.getWidth() - expandBtnSize - 6;
            int expandBtnY = this.getY() + 8;

            if (mx >= expandBtnX && mx <= expandBtnX + expandBtnSize && my >= expandBtnY && my <= expandBtnY + expandBtnSize) {
                toggleExpanded();
                return true;
            }

            if (onSelected != null) {
                onSelected.accept(result);
                return true;
            }
            return false;
        }
    }
}
