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

            int iconBoxSize = 22;
            int iconBoxX = x + 5;
            int iconBoxY = y + 8;
            boolean isIconHovered = mouseX >= iconBoxX && mouseX <= iconBoxX + iconBoxSize && mouseY >= iconBoxY && mouseY <= iconBoxY + iconBoxSize;

            // Background card with Theme 2: Deep Netherite & Ancient Debris
            if ((isHovered && !isBtnHovered && !isEditHovered && !isIconHovered) || isFocused()) {
                // Hovered / Focused: Warmer Slate with highlighted bronze border
                graphics.fill(x, y + 1, x + width, y + height - 1, 0xEE272025);
                graphics.fill(x, y + 1, x + width, y + 2, 0xFF8C5738);
                graphics.fill(x, y + height - 2, x + width, y + height - 1, 0xFF5A3D31);
                graphics.fill(x, y + 1, x + 1, y + height - 1, 0xFF8C5738);
                graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, 0xFF5A3D31);
                // 1px inner specular top line
                graphics.fill(x + 1, y + 2, x + width - 1, y + 3, 0x22FFFFFF);
            } else {
                // Normal: Deep Netherite Slate with Ancient Debris Border
                graphics.fill(x, y + 1, x + width, y + height - 1, 0xEE1E191D);
                graphics.fill(x, y + 1, x + width, y + 2, 0xFF3A2B25);
                graphics.fill(x, y + height - 2, x + width, y + height - 1, 0xFF241A17);
                graphics.fill(x, y + 1, x + 1, y + height - 1, 0xFF3A2B25);
                graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, 0xFF241A17);
            }

            // 1. Left Icon Slot (Inset Ancient Debris Beveled Frame)
            SearchResultType type = result.getType();
            graphics.fill(iconBoxX, iconBoxY, iconBoxX + iconBoxSize, iconBoxY + iconBoxSize, isIconHovered ? 0xFF1C1619 : 0xFF120E10);
            graphics.fill(iconBoxX, iconBoxY, iconBoxX + iconBoxSize, iconBoxY + 1, 0xFF4D362C);
            graphics.fill(iconBoxX, iconBoxY + iconBoxSize - 1, iconBoxX + iconBoxSize, iconBoxY + iconBoxSize, 0xFF36241D);
            graphics.fill(iconBoxX, iconBoxY, iconBoxX + 1, iconBoxY + iconBoxSize, 0xFF4D362C);
            graphics.fill(iconBoxX + iconBoxSize - 1, iconBoxY, iconBoxX + iconBoxSize, iconBoxY + iconBoxSize, 0xFF36241D);
            graphics.fill(iconBoxX + 1, iconBoxY + 1, iconBoxX + iconBoxSize - 1, iconBoxY + 2, 0x44000000);
            graphics.fill(iconBoxX + 1, iconBoxY + 1, iconBoxX + 2, iconBoxY + iconBoxSize - 1, 0x44000000);

            net.minecraft.resources.Identifier modTexture = result.getTextureIcon();
            if (modTexture != null) {
                graphics.blit(modTexture, iconBoxX + 3, iconBoxY + 3, iconBoxX + 19, iconBoxY + 19, 0.0f, 1.0f, 0.0f, 1.0f);
            } else if (result.hasIcon()) {
                graphics.item(result.getIcon(), iconBoxX + 3, iconBoxY + 3);
            } else {
                graphics.item(type.getDefaultIcon(), iconBoxX + 3, iconBoxY + 3);
            }

            int curX = iconBoxX + iconBoxSize + 7;
            int rightBoundaryX = hasEdit ? editBtnX - 4 : expandBtnX - 4;
            int availableWidth = rightBoundaryX - curX;

            // 3. Line 1: Title + Value
            int line1Y = y + 5;
            String title = result.getTitle();
            int maxTitleWidth = Math.max(availableWidth / 2, 130);
            String displayTitle = font.plainSubstrByWidth(title, maxTitleWidth);
            graphics.text(font, displayTitle, curX, line1Y, 0xFFFFF8F0);

            int titleRenderWidth = font.width(displayTitle);
            int valStartX = curX + titleRenderWidth + 6;
            int maxValWidth = availableWidth - titleRenderWidth - 8;

            if (isListeningNow) {
                float pulse = (float) (0.6 + 0.4 * Math.sin(System.currentTimeMillis() * 0.008));
                int valAlpha = (int) (pulse * 255);
                int pulseColor = (valAlpha << 24) | 0x00FFCC00;
                int pillW = font.width("[Press Any Key...]") + 6;
                graphics.fill(valStartX - 2, line1Y - 1, valStartX + pillW - 4, line1Y + 10, 0x55B45309);
                graphics.fill(valStartX - 2, line1Y - 1, valStartX + pillW - 4, line1Y, 0xFFD97706);
                graphics.text(font, "[Press Any Key...]", valStartX, line1Y, pulseColor);
            } else if (!result.getValue().isEmpty()) {
                String valStr = "[" + result.getValue() + "]";
                if (maxValWidth > 20) {
                    String displayVal = font.plainSubstrByWidth(valStr, maxValWidth);
                    int valW = font.width(displayVal);
                    graphics.fill(valStartX - 2, line1Y - 1, valStartX + valW + 2, line1Y + 10, 0x33B45309);
                    graphics.fill(valStartX - 2, line1Y - 1, valStartX + valW + 2, line1Y, 0x66D97706);
                    int valColor = isEditHovered ? 0xFFFFF0D0 : 0xFFFBBF24;
                    graphics.text(font, displayVal, valStartX, line1Y, valColor);
                }
            }

            // 4. Line 2: Path / Category
            int line2Y = y + 19;
            if (!result.getPath().isEmpty() && availableWidth > 20) {
                String displayPath = font.plainSubstrByWidth(result.getPath(), availableWidth);
                graphics.text(font, displayPath, curX, line2Y, 0xFFA89F91);
            }

            // 5. Edit Button (Theme 2: Netherite Action Button)
            if (hasEdit) {
                if (isListeningNow) {
                    graphics.fill(editBtnX, editBtnY, editBtnX + editBtnW, editBtnY + editBtnH, 0xFF7F1D1D);
                    graphics.fill(editBtnX, editBtnY, editBtnX + editBtnW, editBtnY + 1, 0xFFEF4444);
                    graphics.centeredText(font, "> ? <", editBtnX + (editBtnW / 2), editBtnY + (editBtnH - 8) / 2, 0xFFFFF0D0);
                } else if (isEditHovered) {
                    graphics.fill(editBtnX, editBtnY, editBtnX + editBtnW, editBtnY + editBtnH, 0xFFB45309);
                    graphics.fill(editBtnX, editBtnY, editBtnX + editBtnW, editBtnY + 1, 0xFFF59E0B);
                    graphics.fill(editBtnX, editBtnY + editBtnH - 1, editBtnX + editBtnW, editBtnY + editBtnH, 0xFF78350F);
                    graphics.fill(editBtnX, editBtnY, editBtnX + 1, editBtnY + editBtnH, 0xFFF59E0B);
                    graphics.fill(editBtnX + editBtnW - 1, editBtnY, editBtnX + editBtnW, editBtnY + editBtnH, 0xFF78350F);
                    graphics.centeredText(font, "Edit", editBtnX + (editBtnW / 2), editBtnY + (editBtnH - 8) / 2, 0xFFFFFFFF);
                } else {
                    graphics.fill(editBtnX, editBtnY, editBtnX + editBtnW, editBtnY + editBtnH, 0xFF362620);
                    graphics.fill(editBtnX, editBtnY, editBtnX + editBtnW, editBtnY + 1, 0xFF634537);
                    graphics.fill(editBtnX, editBtnY + editBtnH - 1, editBtnX + editBtnW, editBtnY + editBtnH, 0xFF241A17);
                    graphics.fill(editBtnX, editBtnY, editBtnX + 1, editBtnY + editBtnH, 0xFF634537);
                    graphics.fill(editBtnX + editBtnW - 1, editBtnY, editBtnX + editBtnW, editBtnY + editBtnH, 0xFF241A17);
                    graphics.centeredText(font, "Edit", editBtnX + (editBtnW / 2), editBtnY + (editBtnH - 8) / 2, 0xFFFED7AA);
                }
            }

            // 6. Expand / Collapse Button (+ / -) (Theme 2: Netherite Button)
            if (isBtnHovered) {
                graphics.fill(expandBtnX, expandBtnY, expandBtnX + expandBtnSize, expandBtnY + expandBtnSize, 0xFFB45309);
                graphics.fill(expandBtnX, expandBtnY, expandBtnX + expandBtnSize, expandBtnY + 1, 0xFFF59E0B);
                graphics.fill(expandBtnX, expandBtnY + expandBtnSize - 1, expandBtnX + expandBtnSize, expandBtnY + expandBtnSize, 0xFF78350F);
                graphics.fill(expandBtnX, expandBtnY, expandBtnX + 1, expandBtnY + expandBtnSize, 0xFFF59E0B);
                graphics.fill(expandBtnX + expandBtnSize - 1, expandBtnY, expandBtnX + expandBtnSize, expandBtnY + expandBtnSize, 0xFF78350F);
                String symbol = expanded ? "-" : "+";
                graphics.centeredText(font, symbol, expandBtnX + (expandBtnSize / 2), expandBtnY + (expandBtnSize - 8) / 2, 0xFFFFFFFF);
            } else {
                graphics.fill(expandBtnX, expandBtnY, expandBtnX + expandBtnSize, expandBtnY + expandBtnSize, 0xFF362620);
                graphics.fill(expandBtnX, expandBtnY, expandBtnX + expandBtnSize, expandBtnY + 1, 0xFF634537);
                graphics.fill(expandBtnX, expandBtnY + expandBtnSize - 1, expandBtnX + expandBtnSize, expandBtnY + expandBtnSize, 0xFF241A17);
                graphics.fill(expandBtnX, expandBtnY, expandBtnX + 1, expandBtnY + expandBtnSize, 0xFF634537);
                graphics.fill(expandBtnX + expandBtnSize - 1, expandBtnY, expandBtnX + expandBtnSize, expandBtnY + expandBtnSize, 0xFF241A17);
                String symbol = expanded ? "-" : "+";
                graphics.centeredText(font, symbol, expandBtnX + (expandBtnSize / 2), expandBtnY + (expandBtnSize - 8) / 2, 0xFFFED7AA);
            }

            // 7. Expanded Full Details Area (Theme 2: Netherite Slate Details Card)
            if (expanded) {
                int cardX = curX - 6;
                int cardY = y + 36;
                int cardW = (expandBtnX + expandBtnSize) - cardX + 2;
                int cardH = (y + height - 4) - cardY;
                int contentWidth = Math.max(cardW - 14, 100);

                // Main Netherite background
                graphics.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0xF0181518);
                // Ancient debris bronze borders
                graphics.fill(cardX, cardY, cardX + cardW, cardY + 1, 0xFF5A3D31);
                graphics.fill(cardX, cardY + cardH - 1, cardX + cardW, cardY + cardH, 0xFF3D2C25);
                graphics.fill(cardX, cardY, cardX + 1, cardY + cardH, 0xFF5A3D31);
                graphics.fill(cardX + cardW - 1, cardY, cardX + cardW, cardY + cardH, 0xFF3D2C25);
                // Top inner specular line
                graphics.fill(cardX + 1, cardY + 1, cardX + cardW - 1, cardY + 2, 0x44B45309);

                int innerX = cardX + 7;
                int curY = cardY + 6;

                // Detail 1: Type (GOLD & YELLOW)
                Component typeComp = Component.literal("Type: ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(type.getLabel()).withStyle(ChatFormatting.YELLOW));
                graphics.text(font, typeComp, innerX, curY, 0xFFFFFFFF);
                curY += 12;

                // Detail 2: Mod (BLUE & AQUA)
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

                // Detail 6: Tags
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

            // 2. On click [Edit] only, do edit!
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

            // 3. Clicking result item row does nothing
            return false;
        }
    }
}
