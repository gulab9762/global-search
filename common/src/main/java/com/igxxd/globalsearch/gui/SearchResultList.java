package com.igxxd.globalsearch.gui;

import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.List;
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
    public int getRowLeft() {
        return this.getX() + 4;
    }

    @Override
    public int getRowWidth() {
        return Math.max(this.width - 20, 180);
    }

    @Override
    protected int scrollBarX() {
        return this.getX() + this.width - 8;
    }

    @Override
    protected boolean isOverScrollbar(double mouseX, double mouseY) {
        return this.scrollable()
                && mouseX >= this.getX() + this.width - 14
                && mouseX <= this.getX() + this.width
                && mouseY >= this.getY()
                && mouseY <= this.getBottom();
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
                int contentWidth = Math.max(this.getWidth() - 56, 100);
                int h = 38 + 6; // header + card top gap

                // Card top padding
                h += 6;

                // Line 1: Type
                h += 12;

                // Line 2: Mod (on next line)
                h += 12;

                // Line 3: Full Path
                if (!result.getPath().isEmpty()) {
                    var pathLines = font.split(Component.literal("Path: " + result.getPath()), contentWidth);
                    h += Math.max(1, pathLines.size()) * 11;
                }

                // Line 4: ID (if distinct from path and non-empty)
                if (!result.getId().isEmpty() && !result.getId().equals(result.getPath())) {
                    h += 12;
                }

                // Line 5: Description / Info
                if (!result.getDescription().isEmpty()) {
                    var descLines = font.split(Component.literal(result.getDescription()), contentWidth);
                    h += 12 + Math.max(1, descLines.size()) * 11;
                }

                // Line 6: Tags (on next line)
                if (result.hasTags()) {
                    var tagLines = font.split(Component.literal(result.getTags()), contentWidth);
                    h += 12 + Math.max(1, tagLines.size()) * 11;
                }

                // Card bottom padding + row margin
                h += 10;

                this.setHeight(h);
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

            int iconBoxSize = 22;
            int iconBoxX = x + 5;
            int iconBoxY = y + 8;
            boolean isIconHovered = mouseX >= iconBoxX && mouseX <= iconBoxX + iconBoxSize && mouseY >= iconBoxY && mouseY <= iconBoxY + iconBoxSize;

            // Background card with hover highlight
            if ((isHovered && !isBtnHovered && !isIconHovered) || isFocused()) {
                graphics.fill(x, y + 1, x + width, y + height - 1, 0x33FFFFFF);
            } else {
                graphics.fill(x, y + 1, x + width, y + height - 1, 0x14000000);
            }

            // 1. Left Icon Slot (In-Game Sprite / Item / Mod Profile Icon)
            SearchResultType type = result.getType();
            graphics.fill(iconBoxX, iconBoxY, iconBoxX + iconBoxSize, iconBoxY + iconBoxSize, isIconHovered ? 0x44FFFFFF : 0x2E000000);
            graphics.fill(iconBoxX, iconBoxY, iconBoxX + iconBoxSize, iconBoxY + 1, 0x33FFFFFF);
            graphics.fill(iconBoxX, iconBoxY + iconBoxSize - 1, iconBoxX + iconBoxSize, iconBoxY + iconBoxSize, 0x22000000);
            graphics.fill(iconBoxX, iconBoxY, iconBoxX + 1, iconBoxY + iconBoxSize, 0x33FFFFFF);
            graphics.fill(iconBoxX + iconBoxSize - 1, iconBoxY, iconBoxX + iconBoxSize, iconBoxY + iconBoxSize, 0x22000000);

            net.minecraft.resources.Identifier modTexture = result.getTextureIcon();
            if (modTexture != null) {
                graphics.blit(modTexture, iconBoxX + 3, iconBoxY + 3, iconBoxX + 19, iconBoxY + 19, 0.0f, 1.0f, 0.0f, 1.0f);
            } else if (result.hasIcon()) {
                graphics.item(result.getIcon(), iconBoxX + 3, iconBoxY + 3);
            } else {
                graphics.item(type.getDefaultIcon(), iconBoxX + 3, iconBoxY + 3);
            }

            int curX = iconBoxX + iconBoxSize + 7;
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

            // 4. Line 2: Mod Name + Path / Category (collapsed preview)
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

            // 5. Expand / Collapse Button (+ / -)
            graphics.fill(expandBtnX, expandBtnY, expandBtnX + expandBtnSize, expandBtnY + expandBtnSize, isBtnHovered ? 0x44FFFFFF : 0x1AFFFFFF);
            String symbol = expanded ? "-" : "+";
            graphics.centeredText(font, symbol, expandBtnX + (expandBtnSize / 2), expandBtnY + (expandBtnSize - 9) / 2, isBtnHovered ? 0xFFFFFFFF : 0xFFAAAAAA);

            // 6. Expanded Full Details Area (Vanilla hover tooltip-style card)
            if (expanded) {
                int cardX = curX - 6;
                int cardY = y + 36;
                int cardW = (expandBtnX + expandBtnSize) - cardX + 2;
                int cardH = (y + height - 4) - cardY;
                int contentWidth = Math.max(cardW - 14, 100);

                // Authentic Vanilla Tooltip Box
                // Main dark background
                graphics.fill(cardX + 1, cardY, cardX + cardW - 1, cardY + cardH, 0xF0100010);
                graphics.fill(cardX, cardY + 1, cardX + cardW, cardY + cardH - 1, 0xF0100010);

                // Outer border (purple/blue gradient)
                graphics.fill(cardX + 1, cardY, cardX + cardW - 1, cardY + 1, 0x505000FF);
                graphics.fill(cardX + 1, cardY + cardH - 1, cardX + cardW - 1, cardY + cardH, 0x5028007F);
                graphics.fillGradient(cardX, cardY + 1, cardX + 1, cardY + cardH - 1, 0x505000FF, 0x5028007F);
                graphics.fillGradient(cardX + cardW - 1, cardY + 1, cardX + cardW, cardY + cardH - 1, 0x505000FF, 0x5028007F);

                // Inner border highlight (purple/blue gradient)
                graphics.fill(cardX + 1, cardY + 1, cardX + cardW - 1, cardY + 2, 0x505000FF);
                graphics.fill(cardX + 1, cardY + cardH - 2, cardX + cardW - 1, cardY + cardH - 1, 0x5028007F);
                graphics.fillGradient(cardX + 1, cardY + 2, cardX + 2, cardY + cardH - 2, 0x505000FF, 0x5028007F);
                graphics.fillGradient(cardX + cardW - 2, cardY + 2, cardX + cardW - 1, cardY + cardH - 2, 0x505000FF, 0x5028007F);

                int innerX = cardX + 7;
                int curY = cardY + 6;

                // Detail 1: Type (Different color: GOLD & YELLOW)
                Component typeComp = Component.literal("Type: ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(type.getLabel()).withStyle(ChatFormatting.YELLOW));
                graphics.text(font, typeComp, innerX, curY, 0xFFFFFFFF);
                curY += 12;

                // Detail 2: Mod (Moved to next line! Different color: BLUE & AQUA)
                Component modComp = Component.literal("Mod: ").withStyle(ChatFormatting.BLUE)
                        .append(Component.literal(result.getModName()).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" (" + result.getModId() + ")").withStyle(ChatFormatting.DARK_GRAY));
                graphics.text(font, modComp, innerX, curY, 0xFFFFFFFF);
                curY += 12;

                // Detail 3: Full Path
                if (!result.getPath().isEmpty()) {
                    Component pathComp = Component.literal("Path: ").withStyle(ChatFormatting.DARK_GRAY)
                            .append(Component.literal(result.getPath()).withStyle(ChatFormatting.GRAY));
                    var pathLines = font.split(pathComp, contentWidth);
                    for (var pline : pathLines) {
                        graphics.text(font, pline, innerX, curY, 0xFFCCCCCC);
                        curY += 11;
                    }
                }

                // Detail 4: ID (if distinct)
                if (!result.getId().isEmpty() && !result.getId().equals(result.getPath())) {
                    Component idComp = Component.literal("ID: ").withStyle(ChatFormatting.DARK_GRAY)
                            .append(Component.literal(result.getId()).withStyle(ChatFormatting.DARK_AQUA));
                    graphics.text(font, idComp, innerX, curY, 0xFF88DDAA);
                    curY += 12;
                }

                // Detail 5: Description
                if (!result.getDescription().isEmpty()) {
                    graphics.text(font, Component.literal("Description:").withStyle(ChatFormatting.GOLD), innerX, curY, 0xFFFFAA00);
                    curY += 11;
                    var descLines = font.split(Component.literal(result.getDescription()), contentWidth);
                    for (var dline : descLines) {
                        graphics.text(font, dline, innerX, curY, 0xFFE0E0E0);
                        curY += 11;
                    }
                }

                // Detail 6: Tags (Moved to next line!)
                if (result.hasTags()) {
                    graphics.text(font, Component.literal("Tags:").withStyle(ChatFormatting.DARK_AQUA), innerX, curY, 0xFF55FFFF);
                    curY += 11;
                    var tagLines = font.split(Component.literal(result.getTags()).withStyle(ChatFormatting.AQUA), contentWidth);
                    for (var tline : tagLines) {
                        graphics.text(font, tline, innerX, curY, 0xFF77DDAA);
                        curY += 11;
                    }
                }
            }

            // Minimal, clean on-hover tooltips (for expand button and icon box)
            if (isBtnHovered) {
                graphics.setComponentTooltipForNextFrame(font, List.of(Component.literal(expanded ? "Collapse details" : "Expand details")), mouseX, mouseY);
            } else if (isIconHovered) {
                graphics.setComponentTooltipForNextFrame(font, List.of(Component.literal(type.getLabel()).withStyle(ChatFormatting.GOLD)), mouseX, mouseY);
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
