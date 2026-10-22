package com.igxxd.globalsearch.gui;

import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Scrollable list for category navigation in Global Search.
 * Automatically handles overflow, scrolling, themes, active indicators, and badge counts.
 */
public class CategoryNavigationList extends ObjectSelectionList<CategoryNavigationList.CategoryNavEntry> {

    private final GlobalSearchScreen screen;

    public CategoryNavigationList(Minecraft minecraft, int width, int height, int y, int itemHeight, GlobalSearchScreen screen) {
        super(minecraft, width, height, y, itemHeight);
        this.screen = screen;
        rebuildEntries();
    }

    public void rebuildEntries() {
        double currentScroll = this.scrollAmount();
        this.clearEntries();

        List<SearchResultType> categories = SearchResultType.getAllCategories();
        for (SearchResultType type : categories) {
            this.addEntry(new CategoryItemEntry(this, type));
            // Add "By Key Value" Checkbox under KEYBIND when active
            if (type.equals(SearchResultType.KEYBIND) && screen.getCurrentFilter().equals(SearchResultType.KEYBIND)) {
                this.addEntry(new KeyValueCheckboxEntry(this));
            }
        }

        this.setScrollAmount(currentScroll);
    }

    public void refreshCounts() {
        // Entries read counts dynamically during render
    }

    @Override
    public int getRowLeft() {
        return this.getX() + 2;
    }

    @Override
    public int getRowWidth() {
        return Math.max(this.width - 8, 80);
    }

    @Override
    protected int scrollBarX() {
        return this.getX() + this.width - 5;
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
                && mouseX >= this.getX() + this.width - 10
                && mouseX <= this.getX() + this.width
                && mouseY >= this.getY()
                && mouseY <= this.getBottom();
    }

    public abstract static class CategoryNavEntry extends ObjectSelectionList.Entry<CategoryNavEntry> {
        protected final CategoryNavigationList list;

        public CategoryNavEntry(CategoryNavigationList list) {
            this.list = list;
        }

        @Override
        public Component getNarration() {
            return Component.empty();
        }
    }

    /**
     * Entry representing a single SearchResultType category.
     */
    public static class CategoryItemEntry extends CategoryNavEntry {
        private final SearchResultType type;

        public CategoryItemEntry(CategoryNavigationList list, SearchResultType type) {
            super(list);
            this.type = type;
            this.setHeight(19);
        }

        public SearchResultType getType() {
            return type;
        }

        @Override
        public Component getNarration() {
            return Component.literal(type.getLabel());
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();

            Minecraft mc = Minecraft.getInstance();
            Font font = mc.font;
            SearchTheme theme = SearchTheme.getCurrent();
            boolean active = list.screen.getCurrentFilter().equals(type);
            int count = list.screen.getCategoryResultCount(type);

            if (active) {
                // Active Category Card
                int bg = theme.getActiveBtnGradTop();
                int border = theme.getActiveBtnBorder();
                GuiRenderUtils.drawRoundedCard(graphics, x, y, w, h, bg, border, 0x33FFFFFF);

                int textStartX = x + 5;
                String icon = type.getCategoryIcon();
                if (icon != null && !icon.isEmpty()) {
                    graphics.text(font, icon, textStartX, y + (h - 8) / 2, theme.getActiveBtnText());
                    textStartX += font.width(icon) + 3;
                } else {
                    graphics.text(font, "●", textStartX, y + (h - 8) / 2, theme.getActiveBtnText());
                    textStartX += 8;
                }
                graphics.text(font, type.getLabel(), textStartX, y + (h - 8) / 2, theme.getActiveBtnText());
            } else if (isHovered) {
                // Hovered Category Card
                GuiRenderUtils.drawRoundedCard(graphics, x, y, w, h, theme.getCardHoverBg(), theme.getCardBorderHover(), 0x22FFFFFF);

                int textStartX = x + 5;
                String icon = type.getCategoryIcon();
                if (icon != null && !icon.isEmpty()) {
                    graphics.text(font, icon, textStartX, y + (h - 8) / 2, theme.getAccentColor());
                    textStartX += font.width(icon) + 3;
                } else {
                    graphics.text(font, "○", textStartX, y + (h - 8) / 2, theme.getCardBorderHover());
                    textStartX += 8;
                }
                graphics.text(font, type.getLabel(), textStartX, y + (h - 8) / 2, 0xFFFFF0D0);
            } else {
                // Normal Inactive Category Card
                GuiRenderUtils.drawRoundedBox(graphics, x, y, w, h, theme.getCardBg(), theme.getPanelBorder());

                int textStartX = x + 5;
                String icon = type.getCategoryIcon();
                if (icon != null && !icon.isEmpty()) {
                    graphics.text(font, icon, textStartX, y + (h - 8) / 2, theme.getMutedTextColor());
                    textStartX += font.width(icon) + 3;
                } else {
                    graphics.text(font, "○", textStartX, y + (h - 8) / 2, theme.getCardBorder());
                    textStartX += 8;
                }
                graphics.text(font, type.getLabel(), textStartX, y + (h - 8) / 2, theme.getMutedTextColor());
            }

            // Right-aligned Badge count
            if (count >= 0) {
                String countStr = String.valueOf(count);
                int countW = font.width(countStr);
                int badgeX = x + w - countW - 5;
                int badgeY = y + (h - 8) / 2;
                int color = active ? theme.getActiveBtnText() : (isHovered ? theme.getAccentColor() : theme.getMutedTextColor());
                graphics.text(font, countStr, badgeX, badgeY, color);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F
            ));
            list.screen.onCategorySelected(this.type);
            return true;
        }
    }

    /**
     * Entry representing the "By Key Value" Checkbox under Keybinds.
     */
    public static class KeyValueCheckboxEntry extends CategoryNavEntry {

        public KeyValueCheckboxEntry(CategoryNavigationList list) {
            super(list);
            this.setHeight(17);
        }

        @Override
        public Component getNarration() {
            return Component.literal("Search by key value");
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            int x = this.getX() + 6;
            int y = this.getY();
            int h = this.getHeight();
            Font font = Minecraft.getInstance().font;
            SearchTheme theme = SearchTheme.getCurrent();
            boolean checked = list.screen.isSearchByKeyValue();

            int boxSize = 10;
            int boxY = y + (h - boxSize) / 2;
            int boxBg = checked ? theme.getActiveBtnGradTop() : theme.getCardBg();
            int boxBorder = isHovered ? theme.getAccentColor() : (checked ? theme.getActiveBtnBorder() : theme.getCardBorder());

            GuiRenderUtils.drawRoundedBox(graphics, x, boxY, boxSize, boxSize, boxBg, boxBorder);
            if (checked) {
                graphics.centeredText(font, "✓", x + (boxSize / 2), boxY + 1, theme.getActiveBtnText());
            }

            int textColor = isHovered ? theme.getAccentColor() : (checked ? 0xFFFFF0D0 : theme.getMutedTextColor());
            graphics.text(font, "By Key Value", x + boxSize + 5, y + (h - 8) / 2, textColor);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F
            ));
            list.screen.toggleSearchByKeyValue();
            return true;
        }
    }
}
