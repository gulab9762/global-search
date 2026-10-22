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
import java.util.function.Predicate;

public class SearchResultList extends ObjectSelectionList<SearchResultList.ResultEntry> {

    private final Consumer<SearchResult> onSelected;
    private final Consumer<SearchResult> onEdit;
    private final Predicate<SearchResult> isListening;

    public SearchResultList(Minecraft minecraft, int width, int height, int y, int itemHeight,
                            Consumer<SearchResult> onSelected,
                            Consumer<SearchResult> onEdit,
                            Predicate<SearchResult> isListening) {
        super(minecraft, width, height, y, itemHeight);
        this.onSelected = onSelected;
        this.onEdit = onEdit;
        this.isListening = isListening;
    }

    public SearchResultList(Minecraft minecraft, int width, int height, int y, int itemHeight, Consumer<SearchResult> onSelected) {
        this(minecraft, width, height, y, itemHeight, onSelected, null, null);
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
    protected void extractListBackground(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        // Suppress Minecraft's default opaque dirt/dark background so world is visible behind
    }

    @Override
    protected void extractListSeparators(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        // Suppress default opaque list separators
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
            this.addEntry(new ResultEntry(this, result, this.minecraft.font, this.onSelected, this.onEdit, this.isListening));
        }
        this.setScrollAmount(0);
    }

    public static class ResultEntry extends ObjectSelectionList.Entry<ResultEntry> {

        private final SearchResultList list;
        private final SearchResult result;
        private final Font font;
        private final Consumer<SearchResult> onSelected;
        private final Consumer<SearchResult> onEdit;
        private final Predicate<SearchResult> isListening;
        private boolean expanded = false;

        public ResultEntry(SearchResultList list, SearchResult result, Font font,
                           Consumer<SearchResult> onSelected,
                           Consumer<SearchResult> onEdit,
                           Predicate<SearchResult> isListening) {
            this.list = list;
            this.result = result;
            this.font = font;
            this.onSelected = onSelected;
            this.onEdit = onEdit;
            this.isListening = isListening;
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

            int editBtnW = 30;
            int editBtnH = 18;
            int editBtnX = expandBtnX - editBtnW - 4;
            int editBtnY = y + 8;
            boolean hasEdit = result.isEditable();
            boolean isListeningNow = isListening != null && isListening.test(result);
            boolean isEditHovered = hasEdit && mouseX >= editBtnX && mouseX <= editBtnX + editBtnW && mouseY >= editBtnY && mouseY <= editBtnY + editBtnH;

            boolean hasAction = result.hasActionCommand();
            String actionLabel = result.getActionCommandLabel();
            int actionBtnW = Math.max(font.width(actionLabel) + 10, 32);
            int actionBtnH = 18;
            int actionBtnX = (hasEdit ? editBtnX : expandBtnX) - actionBtnW - 4;
            int actionBtnY = y + 8;
            boolean isActionHovered = hasAction && mouseX >= actionBtnX && mouseX <= actionBtnX + actionBtnW && mouseY >= actionBtnY && mouseY <= actionBtnY + actionBtnH;

            int iconBoxSize = 22;
            int iconBoxX = x + 5;
            int iconBoxY = y + 8;
            boolean isIconHovered = mouseX >= iconBoxX && mouseX <= iconBoxX + iconBoxSize && mouseY >= iconBoxY && mouseY <= iconBoxY + iconBoxSize;

            SearchTheme theme = SearchTheme.getCurrent();

            // Background card with rounded corners and theme colors
            if ((isHovered && !isBtnHovered && !isEditHovered && !isActionHovered && !isIconHovered) || isFocused()) {
                GuiRenderUtils.drawRoundedCard(graphics, x, y + 1, width, height - 2, theme.getCardHoverBg(), theme.getCardBorderHover(), 0x22FFFFFF);
            } else {
                GuiRenderUtils.drawRoundedBox(graphics, x, y + 1, width, height - 2, theme.getCardBg(), theme.getCardBorder());
            }

            // 1. Left Icon Slot with rounded border and theme styling
            SearchResultType type = result.getType();
            int iconBg = isIconHovered ? 0x88000000 : 0x55000000;
            GuiRenderUtils.drawRoundedBox(graphics, iconBoxX, iconBoxY, iconBoxSize, iconBoxSize, iconBg, isIconHovered ? theme.getCardBorderHover() : theme.getCardBorder());

            net.minecraft.resources.Identifier modTexture = result.getTextureIcon();
            if (modTexture != null) {
                graphics.blit(modTexture, iconBoxX + 3, iconBoxY + 3, iconBoxX + 19, iconBoxY + 19, 0.0f, 1.0f, 0.0f, 1.0f);
            } else if (result.hasIcon()) {
                graphics.item(result.getIcon(), iconBoxX + 3, iconBoxY + 3);
            } else {
                graphics.item(type.getDefaultIcon(), iconBoxX + 3, iconBoxY + 3);
            }

            int curX = iconBoxX + iconBoxSize + 7;
            int rightBoundaryX = hasAction ? actionBtnX - 4 : (hasEdit ? editBtnX - 4 : expandBtnX - 4);
            int availableWidth = rightBoundaryX - curX;

            // 3. Line 1: Title + Value Pill
            int line1Y = y + 5;
            String title = result.getTitle();
            int maxTitleWidth = Math.max(availableWidth / 2, 120);
            String displayTitle = font.plainSubstrByWidth(title, maxTitleWidth);
            graphics.text(font, displayTitle, curX, line1Y, theme.getTitleColor());

            int titleRenderWidth = font.width(displayTitle);
            int valStartX = curX + titleRenderWidth + 6;
            int maxValWidth = availableWidth - titleRenderWidth - 8;

            if (isListeningNow) {
                float pulse = (float) (0.6 + 0.4 * Math.sin(System.currentTimeMillis() * 0.008));
                int valAlpha = (int) (pulse * 255);
                int pulseColor = (valAlpha << 24) | (theme.getAccentColor() & 0x00FFFFFF);
                String listenText = "[Press Any Key...]";
                int pillW = font.width(listenText) + 6;
                int pillBg = ((valAlpha / 4) << 24) | (theme.getActiveBtnGradTop() & 0x00FFFFFF);
                GuiRenderUtils.drawRoundedBox(graphics, valStartX - 2, line1Y - 1, pillW, 11, pillBg, theme.getAccentColor());
                graphics.text(font, listenText, valStartX + 1, line1Y, pulseColor);
            } else if (!result.getValue().isEmpty()) {
                String valStr = "[" + result.getValue() + "]";
                if (maxValWidth > 20) {
                    String displayVal = font.plainSubstrByWidth(valStr, maxValWidth);
                    int valW = font.width(displayVal);
                    int pillBg = isEditHovered ? (0x55000000 | (theme.getAccentColor() & 0x00FFFFFF)) : 0x44000000;
                    GuiRenderUtils.drawRoundedBox(graphics, valStartX - 2, line1Y - 1, valW + 4, 11, pillBg, theme.getActiveBtnBorder());
                    int valColor = isEditHovered ? 0xFFFFFFFF : theme.getAccentColor();
                    graphics.text(font, displayVal, valStartX, line1Y, valColor);
                }
            }

            // 4. Line 2: Category Tag Pill + Path
            int line2Y = y + 19;
            String tagText = type.getCategoryIcon() + " " + type.getLabel().toUpperCase();
            int tagW = font.width(tagText) + 6;
            int tagBg = 0x33000000 | (theme.getHeaderColor() & 0x00FFFFFF);
            int tagBorder = 0x55000000 | (theme.getHeaderColor() & 0x00FFFFFF);
            GuiRenderUtils.drawRoundedBox(graphics, curX, line2Y - 1, tagW, 10, tagBg, tagBorder);
            graphics.text(font, tagText, curX + 3, line2Y, theme.getHeaderColor());

            int pathStartX = curX + tagW + 5;
            int maxPathW = availableWidth - tagW - 5;
            if (!result.getPath().isEmpty() && maxPathW > 20) {
                String displayPath = font.plainSubstrByWidth(result.getPath(), maxPathW);
                graphics.text(font, displayPath, pathStartX, line2Y, theme.getMutedTextColor());
            }

            // 5a. Action / Command Copy Button ([Locate], [Give], etc.)
            if (hasAction) {
                if (isActionHovered) {
                    GuiRenderUtils.drawRoundedBox(graphics, actionBtnX, actionBtnY, actionBtnW, actionBtnH, theme.getActiveBtnGradTop(), theme.getActiveBtnBorder());
                    graphics.centeredText(font, actionLabel, actionBtnX + (actionBtnW / 2), actionBtnY + (actionBtnH - 8) / 2, 0xFFFFFFFF);
                } else {
                    GuiRenderUtils.drawRoundedBox(graphics, actionBtnX, actionBtnY, actionBtnW, actionBtnH, theme.getCardBg(), theme.getCardBorder());
                    graphics.centeredText(font, actionLabel, actionBtnX + (actionBtnW / 2), actionBtnY + (actionBtnH - 8) / 2, theme.getTitleColor());
                }
            }

            // 5b. Edit Button (Themed Rounded Action Button)
            if (hasEdit) {
                if (isListeningNow) {
                    GuiRenderUtils.drawRoundedBox(graphics, editBtnX, editBtnY, editBtnW, editBtnH, theme.getActiveBtnGradTop(), theme.getAccentColor());
                    graphics.centeredText(font, "> ? <", editBtnX + (editBtnW / 2), editBtnY + (editBtnH - 8) / 2, theme.getActiveBtnText());
                } else if (isEditHovered) {
                    GuiRenderUtils.drawRoundedBox(graphics, editBtnX, editBtnY, editBtnW, editBtnH, theme.getActiveBtnGradTop(), theme.getActiveBtnBorder());
                    graphics.centeredText(font, "Edit", editBtnX + (editBtnW / 2), editBtnY + (editBtnH - 8) / 2, 0xFFFFFFFF);
                } else {
                    GuiRenderUtils.drawRoundedBox(graphics, editBtnX, editBtnY, editBtnW, editBtnH, theme.getCardBg(), theme.getCardBorder());
                    graphics.centeredText(font, "Edit", editBtnX + (editBtnW / 2), editBtnY + (editBtnH - 8) / 2, theme.getTitleColor());
                }
            }

            // 6. Expand / Collapse Button (+ / -)
            String symbol = expanded ? "-" : "+";
            if (isBtnHovered) {
                GuiRenderUtils.drawRoundedBox(graphics, expandBtnX, expandBtnY, expandBtnSize, expandBtnSize, theme.getActiveBtnGradTop(), theme.getActiveBtnBorder());
                graphics.centeredText(font, symbol, expandBtnX + (expandBtnSize / 2), expandBtnY + (expandBtnSize - 8) / 2, 0xFFFFFFFF);
            } else {
                GuiRenderUtils.drawRoundedBox(graphics, expandBtnX, expandBtnY, expandBtnSize, expandBtnSize, theme.getCardBg(), theme.getCardBorder());
                graphics.centeredText(font, symbol, expandBtnX + (expandBtnSize / 2), expandBtnY + (expandBtnSize - 8) / 2, theme.getMutedTextColor());
            }

            // 7. Expanded Full Details Area
            if (expanded) {
                int cardX = curX - 6;
                int cardY = y + 36;
                int cardW = (expandBtnX + expandBtnSize) - cardX + 2;
                int cardH = (y + height - 4) - cardY;
                int contentWidth = Math.max(cardW - 14, 100);

                GuiRenderUtils.drawRoundedCard(graphics, cardX, cardY, cardW, cardH, theme.getPanelBg(), theme.getPanelBorderTop(), 0x22FFFFFF);

                int innerX = cardX + 7;
                int curY = cardY + 6;

                // Detail 1: Type
                Component typeComp = Component.literal("Type: ").withStyle(style -> style.withColor(theme.getAccentColor()))
                        .append(Component.literal(type.getLabel()).withStyle(style -> style.withColor(theme.getTitleColor())));
                graphics.text(font, typeComp, innerX, curY, 0xFFFFFFFF);
                curY += 12;

                // Detail 2: Mod
                Component modComp = Component.literal("Mod: ").withStyle(style -> style.withColor(theme.getAccentColor()))
                        .append(Component.literal(result.getModName()).withStyle(style -> style.withColor(theme.getTitleColor())))
                        .append(Component.literal(" (" + result.getModId() + ")").withStyle(style -> style.withColor(theme.getMutedTextColor())));
                graphics.text(font, modComp, innerX, curY, 0xFFFFFFFF);
                curY += 12;

                // Detail 3: Full Path
                if (!result.getPath().isEmpty()) {
                    Component pathComp = Component.literal("Path: ").withStyle(style -> style.withColor(theme.getMutedTextColor()))
                            .append(Component.literal(result.getPath()).withStyle(style -> style.withColor(theme.getTitleColor())));
                    var pathLines = font.split(pathComp, contentWidth);
                    for (var pline : pathLines) {
                        graphics.text(font, pline, innerX, curY, theme.getMutedTextColor());
                        curY += 11;
                    }
                }

                // Detail 4: ID (if distinct)
                if (!result.getId().isEmpty() && !result.getId().equals(result.getPath())) {
                    Component idComp = Component.literal("ID: ").withStyle(style -> style.withColor(theme.getMutedTextColor()))
                            .append(Component.literal(result.getId()).withStyle(style -> style.withColor(theme.getAccentColor())));
                    graphics.text(font, idComp, innerX, curY, theme.getAccentColor());
                    curY += 12;
                }

                // Detail 4b: Command (if action command available)
                if (result.hasActionCommand()) {
                    Component cmdComp = Component.literal("Command: ").withStyle(style -> style.withColor(theme.getAccentColor()))
                            .append(Component.literal(result.getCopyValue()).withStyle(style -> style.withColor(theme.getTitleColor())));
                    var cmdLines = font.split(cmdComp, contentWidth);
                    for (var cline : cmdLines) {
                        graphics.text(font, cline, innerX, curY, theme.getTitleColor());
                        curY += 11;
                    }
                }

                // Detail 5: Description
                if (!result.getDescription().isEmpty()) {
                    graphics.text(font, Component.literal("Description:").withStyle(style -> style.withColor(theme.getAccentColor())), innerX, curY, theme.getAccentColor());
                    curY += 11;
                    var descLines = font.split(Component.literal(result.getDescription()), contentWidth);
                    for (var dline : descLines) {
                        graphics.text(font, dline, innerX, curY, theme.getTitleColor());
                        curY += 11;
                    }
                }

                // Detail 6: Tags
                if (result.hasTags()) {
                    graphics.text(font, Component.literal("Tags:").withStyle(style -> style.withColor(theme.getAccentColor())), innerX, curY, theme.getAccentColor());
                    curY += 11;
                    var tagLines = font.split(Component.literal(result.getTags()).withStyle(style -> style.withColor(theme.getMutedTextColor())), contentWidth);
                    for (var tline : tagLines) {
                        graphics.text(font, tline, innerX, curY, theme.getMutedTextColor());
                        curY += 11;
                    }
                }
            }

            // On-hover tooltips (only for expand button if hovered)
            if (isBtnHovered) {
                graphics.setComponentTooltipForNextFrame(font, List.of(Component.literal(expanded ? "Collapse details" : "Expand details")), mouseX, mouseY);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            double mx = event.x();
            double my = event.y();

            int expandBtnSize = 18;
            int expandBtnX = this.getX() + this.getWidth() - expandBtnSize - 6;
            int expandBtnY = this.getY() + 8;

            // 1. Expand / collapse details (+ / -)
            if (mx >= expandBtnX && mx <= expandBtnX + expandBtnSize && my >= expandBtnY && my <= expandBtnY + expandBtnSize) {
                toggleExpanded();
                return true;
            }

            // 2. On click [Action] button, copy action command
            boolean hasAction = result.hasActionCommand();
            if (hasAction) {
                String actionLabel = result.getActionCommandLabel();
                int actionBtnW = Math.max(Minecraft.getInstance().font.width(actionLabel) + 10, 32);
                int actionBtnH = 18;
                int actionBtnX = (result.isEditable() ? (expandBtnX - 34) : expandBtnX) - actionBtnW - 4;
                int actionBtnY = this.getY() + 8;

                if (mx >= actionBtnX && mx <= actionBtnX + actionBtnW && my >= actionBtnY && my <= actionBtnY + actionBtnH) {
                    if (onSelected != null) {
                        onSelected.accept(result);
                        return true;
                    }
                }
            }

            // 3. On click [Edit] only, do edit!
            if (result.isEditable()) {
                int editBtnW = 30;
                int editBtnH = 18;
                int editBtnX = expandBtnX - editBtnW - 4;
                int editBtnY = this.getY() + 8;

                if (mx >= editBtnX && mx <= editBtnX + editBtnW && my >= editBtnY && my <= editBtnY + editBtnH) {
                    if (onEdit != null) {
                        onEdit.accept(result);
                        return true;
                    }
                }
            }

            // 4. Clicking result item row copies command / value
            if (mx >= this.getX() && mx <= this.getX() + this.getWidth() && my >= this.getY() && my <= this.getY() + this.getHeight()) {
                if (onSelected != null) {
                    onSelected.accept(result);
                    return true;
                }
            }
            return false;
        }
    }
}
