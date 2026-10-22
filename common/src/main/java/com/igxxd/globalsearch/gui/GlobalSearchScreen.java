package com.igxxd.globalsearch.gui;

import com.igxxd.globalsearch.search.SearchIndexer;
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class GlobalSearchScreen extends Screen {

    private final Screen parentScreen;
    private List<SearchResult> allIndexedResults = new ArrayList<>();
    private final List<SearchResult> filteredResults = new ArrayList<>();

    private EditBox searchBox;
    private CategoryNavigationList categoryList;
    private SearchResultList resultList;
    private SearchResultType currentFilter = SearchResultType.ALL;
    private boolean searchByKeyValue = false;
    private boolean isIndexing = false;
    private static String savedSearchQuery = "";

    private String copiedNotification = null;
    private long notificationExpiry = 0;

    // Keybind inline listening state
    private SearchResult listeningKeybindResult = null;
    private net.minecraft.client.KeyMapping listeningKeyMapping = null;
    private java.util.function.Consumer<String> listeningValueChanged = null;
    private long listeningStartTime = 0;

    // Navigator panel layout bounds
    private int navX;
    private int navY;
    private int navWidth;
    private int navHeight;
    private int searchBoxX;
    private int searchBoxY;
    private int searchBoxW;
    private int searchBoxH;

    public GlobalSearchScreen(Screen parentScreen) {
        super(Component.literal("Global Search"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        if (this.minecraft != null) {
            if (this.allIndexedResults.isEmpty()) {
                if (SearchIndexer.hasCachedResults()) {
                    this.allIndexedResults = SearchIndexer.getCachedResults();
                } else {
                    this.isIndexing = true;
                    SearchIndexer.indexAllAsync(this.minecraft, results -> {
                        this.allIndexedResults = results;
                        this.isIndexing = false;
                        if (this.categoryList != null) {
                            this.categoryList.rebuildEntries();
                        }
                        updateFilter();
                    });
                }
            } else if (SearchIndexer.hasCachedResults()) {
                List<SearchResult> cached = SearchIndexer.getCachedResults();
                if (!cached.isEmpty() && cached.size() >= this.allIndexedResults.size()) {
                    this.allIndexedResults = cached;
                }
            }
        }

        int sideMargin = 12;
        int topMargin = 20;

        // 1. Top Search Bar, Theme Toggle & Rescan Button
        int themeBtnWidth = 104;
        int rescanBtnWidth = 64;
        int searchTotalWidth = this.width - (sideMargin * 2) - rescanBtnWidth - themeBtnWidth - 10;

        this.searchBoxX = sideMargin;
        this.searchBoxY = topMargin;
        this.searchBoxW = searchTotalWidth;
        this.searchBoxH = 20;

        int searchIconPadding = 18;
        this.searchBox = new EditBox(
                this.font,
                searchBoxX + searchIconPadding,
                searchBoxY + 4,
                searchBoxW - searchIconPadding - 22,
                12,
                Component.literal("Search")
        );
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(SearchTheme.getCurrent().getTitleColor());
        this.searchBox.setHint(Component.literal("Search keybinds, settings, items, gamerules, stats...").withStyle(ChatFormatting.DARK_GRAY));
        if (!savedSearchQuery.isEmpty()) {
            this.searchBox.setValue(savedSearchQuery);
        }
        this.searchBox.setResponder(text -> {
            savedSearchQuery = text;
            updateFilter();
        });
        this.addRenderableWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);

        ThemedButton themeBtn = new ThemedButton(
                sideMargin + searchBoxW + 6,
                topMargin,
                themeBtnWidth,
                20,
                Component.literal(SearchTheme.getCurrent().getIcon() + " " + SearchTheme.getCurrent().getDisplayName()),
                () -> {
                    SearchTheme.setCurrent(SearchTheme.getCurrent().next());
                    this.rebuildWidgets();
                }
        ).setCentered(true).setHasDot(false);
        this.addRenderableWidget(themeBtn);

        ThemedButton rescanBtn = new ThemedButton(
                sideMargin + searchBoxW + 6 + themeBtnWidth + 4,
                topMargin,
                rescanBtnWidth,
                20,
                Component.literal("↻ Rescan"),
                this::triggerRescan
        ).setCentered(true).setHasDot(false);
        this.addRenderableWidget(rescanBtn);

        // 2. Left Navigator Panel Dimensions
        navX = sideMargin;
        navY = topMargin + 26;
        navWidth = 138;
        int bottomMargin = 22;
        navHeight = this.height - navY - bottomMargin;

        // 3. Right Side Result List
        int listX = navX + navWidth + 8;
        int listY = navY;
        int listWidth = this.width - listX - sideMargin;
        int listHeight = navHeight;

        // 4. Left Scrollable Navigator Category List
        this.categoryList = new CategoryNavigationList(
                this.minecraft,
                navWidth - 4,
                navHeight - 24,
                navY + 19,
                21,
                this
        );
        this.categoryList.setX(navX + 2);
        this.addRenderableWidget(this.categoryList);

        // 5. Initialize Result List
        this.resultList = new SearchResultList(
                this.minecraft,
                listWidth,
                listHeight,
                listY,
                38,
                this::copyResultValue,
                this::handleResultEdit,
                res -> res != null && res == this.listeningKeybindResult
        );
        this.resultList.setX(listX);
        this.addRenderableWidget(this.resultList);

        // Populate initial filtered results
        updateFilter();
    }

    public void startListeningForKeybind(SearchResult result, net.minecraft.client.KeyMapping keyMapping, java.util.function.Consumer<String> onValueChanged) {
        this.listeningKeybindResult = result;
        this.listeningKeyMapping = keyMapping;
        this.listeningValueChanged = onValueChanged;
        this.listeningStartTime = System.currentTimeMillis();
        this.copiedNotification = "Press any key to bind, or Esc to unbind...";
        this.notificationExpiry = System.currentTimeMillis() + 10000;
        if (this.resultList != null) {
            this.resultList.refreshLayout();
        }
    }

    public void cancelKeybindListening() {
        this.listeningKeybindResult = null;
        this.listeningKeyMapping = null;
        this.listeningValueChanged = null;
        this.copiedNotification = "Keybind change cancelled";
        this.notificationExpiry = System.currentTimeMillis() + 2000;
        if (this.resultList != null) {
            this.resultList.refreshLayout();
        }
    }

    private void handleResultEdit(SearchResult result) {
        if (result == null || this.minecraft == null) return;

        if (result == this.listeningKeybindResult) {
            cancelKeybindListening();
            return;
        }

        boolean edited = com.igxxd.globalsearch.api.GlobalSearchApi.triggerEdit(result, this, newVal -> {
            this.copiedNotification = "Updated " + result.getTitle() + " to [" + newVal + "]";
            this.notificationExpiry = System.currentTimeMillis() + 3000;
            if (this.resultList != null) {
                this.resultList.refreshLayout();
            }
        });

        if (edited && this.listeningKeybindResult == null) {
            this.copiedNotification = "Updated " + result.getTitle() + " to [" + result.getValue() + "]";
            this.notificationExpiry = System.currentTimeMillis() + 3000;
            if (this.resultList != null) {
                this.resultList.refreshLayout();
            }
        }
    }

    private Component getFilterButtonText(SearchResultType type) {
        boolean active = this.currentFilter.equals(type);
        ChatFormatting color = active ? ChatFormatting.YELLOW : ChatFormatting.GRAY;
        String prefix = active ? "▶ " : "  ";
        return Component.literal(prefix + type.getLabel()).withStyle(color);
    }

    private void updateFilter() {
        filteredResults.clear();
        String query = searchBox != null ? searchBox.getValue().trim().toLowerCase(Locale.ROOT) : savedSearchQuery.trim().toLowerCase(Locale.ROOT);
        String[] tokens = query.isEmpty() ? new String[0] : query.split("\\s+");

        for (SearchResult result : allIndexedResults) {
            if (!currentFilter.equals(SearchResultType.ALL) && !result.getType().equals(currentFilter)) {
                continue;
            }

            // Keybind specific: search by key value if checkbox is enabled
            if (currentFilter.equals(SearchResultType.KEYBIND) && searchByKeyValue) {
                if (result.matchesKeyValue(tokens)) {
                    filteredResults.add(result);
                }
            } else {
                if (result.matches(tokens)) {
                    filteredResults.add(result);
                }
            }
        }

        // Prioritize: result name starting with query, or key value starting with query when enabled
        if (!query.isEmpty() && !filteredResults.isEmpty()) {
            boolean byKeyVal = currentFilter.equals(SearchResultType.KEYBIND) && searchByKeyValue;
            filteredResults.sort(Comparator.comparingInt(r -> getRelevanceScore(r, query, byKeyVal)));
        }

        if (resultList != null) {
            resultList.setResults(filteredResults);
        }
    }

    private int getRelevanceScore(SearchResult result, String query, boolean byKeyValue) {
        String title = result.getTitle().toLowerCase(Locale.ROOT);
        String val = result.getValue().toLowerCase(Locale.ROOT);

        if (byKeyValue) {
            // Check key value starting with, then containing, then remaining results
            if (val.startsWith(query)) return 0;
            if (val.contains(query)) return 1;
            if (title.startsWith(query)) return 2;
            if (title.contains(query)) return 3;
            return 4;
        } else {
            // Prioritize result name starting with query
            if (title.startsWith(query)) return 0;
            // Any word in the result name starting with query
            for (String part : title.split("[\\s_\\->\\[\\]()]+")) {
                if (part.startsWith(query)) return 1;
            }
            if (title.contains(query)) return 2;
            if (!val.isEmpty() && val.startsWith(query)) return 3;
            if (!val.isEmpty() && val.contains(query)) return 4;
            if (result.getPath().toLowerCase(Locale.ROOT).contains(query)) return 5;
            return 6;
        }
    }

    private void copyResultValue(SearchResult result) {
        if (this.minecraft != null && result != null) {
            String valueToCopy = result.getCopyValue();
            this.minecraft.keyboardHandler.setClipboard(valueToCopy);
            if (result.hasActionCommand()) {
                this.copiedNotification = "Copied " + result.getActionCommandLabel() + " command: " + valueToCopy;
            } else {
                this.copiedNotification = "Copied: " + valueToCopy;
            }
            this.notificationExpiry = System.currentTimeMillis() + 3000;
        }
    }

    private void triggerRescan() {
        if (this.minecraft != null && !this.isIndexing) {
            this.isIndexing = true;
            this.copiedNotification = "⚡ Rescanning in background...";
            this.notificationExpiry = System.currentTimeMillis() + 10000;
            SearchIndexer.rescanAsync(this.minecraft, results -> {
                this.allIndexedResults = results;
                this.isIndexing = false;
                this.copiedNotification = "Rescanned & indexed " + results.size() + " entries";
                this.notificationExpiry = System.currentTimeMillis() + 3000;
                if (this.categoryList != null) {
                    this.categoryList.rebuildEntries();
                }
                updateFilter();
            });
        }
    }

    public SearchResultType getCurrentFilter() {
        return this.currentFilter;
    }

    public boolean isSearchByKeyValue() {
        return this.searchByKeyValue;
    }

    public void toggleSearchByKeyValue() {
        this.searchByKeyValue = !this.searchByKeyValue;
        if (this.categoryList != null) {
            this.categoryList.rebuildEntries();
        }
        updateFilter();
    }

    public void onCategorySelected(SearchResultType type) {
        if (this.currentFilter != type) {
            this.currentFilter = type;
            if (this.categoryList != null) {
                this.categoryList.rebuildEntries();
            }
            updateFilter();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.listeningKeyMapping != null) {
            if (event.isEscape()) {
                this.listeningKeyMapping.setKey(InputConstants.UNKNOWN);
            } else {
                this.listeningKeyMapping.setKey(InputConstants.getKey(event));
            }
            net.minecraft.client.KeyMapping.resetMapping();
            if (this.minecraft != null && this.minecraft.options != null) {
                this.minecraft.options.save();
            }
            String newKey = this.listeningKeyMapping.getTranslatedKeyMessage().getString();
            if (this.listeningKeybindResult != null) {
                this.listeningKeybindResult.setValue(newKey);
            }
            if (this.listeningValueChanged != null) {
                this.listeningValueChanged.accept(newKey);
            }
            this.copiedNotification = "Rebound " + (this.listeningKeybindResult != null ? this.listeningKeybindResult.getTitle() : "Key") + " to [" + newKey + "]";
            this.notificationExpiry = System.currentTimeMillis() + 3000;

            this.listeningKeybindResult = null;
            this.listeningKeyMapping = null;
            this.listeningValueChanged = null;

            if (this.resultList != null) {
                this.resultList.refreshLayout();
            }
            return true;
        }

        if (event.key() == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (event.key() == InputConstants.KEY_F6 || (event.hasControlDown() && event.key() == InputConstants.KEY_T)) {
            SearchTheme.setCurrent(SearchTheme.getCurrent().next());
            this.rebuildWidgets();
            this.copiedNotification = "Switched to " + SearchTheme.getCurrent().getIcon() + " " + SearchTheme.getCurrent().getDisplayName();
            this.notificationExpiry = System.currentTimeMillis() + 2500;
            return true;
        }

        if (event.key() == InputConstants.KEY_RETURN) {
            if (resultList != null && resultList.getSelected() != null) {
                SearchResult sel = resultList.getSelected().getResult();
                if (sel != null && sel.isEditable()) {
                    handleResultEdit(sel);
                } else {
                    copyResultValue(sel);
                }
                return true;
            }
        }

        return super.keyPressed(event);
    }

    public int getCategoryResultCount(SearchResultType type) {
        if (type == null || type.equals(SearchResultType.ALL)) {
            return allIndexedResults.size();
        }
        int count = 0;
        for (SearchResult res : allIndexedResults) {
            if (res.getType().equals(type)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // 1. Cross icon to clear search inside right hand side of searchBox
        if (this.searchBox != null && !this.searchBox.getValue().isEmpty()) {
            int clearBtnSize = 14;
            int clearBtnX = this.searchBoxX + this.searchBoxW - clearBtnSize - 4;
            int clearBtnY = this.searchBoxY + (this.searchBoxH - clearBtnSize) / 2;
            if (event.x() >= clearBtnX && event.x() <= clearBtnX + clearBtnSize &&
                event.y() >= clearBtnY && event.y() <= clearBtnY + clearBtnSize) {
                this.searchBox.setValue("");
                savedSearchQuery = "";
                updateFilter();
                return true;
            }
        }

        if (this.listeningKeyMapping != null && System.currentTimeMillis() - this.listeningStartTime > 150) {
            this.listeningKeyMapping.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
            net.minecraft.client.KeyMapping.resetMapping();
            if (this.minecraft != null && this.minecraft.options != null) {
                this.minecraft.options.save();
            }
            String newKey = this.listeningKeyMapping.getTranslatedKeyMessage().getString();
            if (this.listeningKeybindResult != null) {
                this.listeningKeybindResult.setValue(newKey);
            }
            if (this.listeningValueChanged != null) {
                this.listeningValueChanged.accept(newKey);
            }
            this.copiedNotification = "Rebound " + (this.listeningKeybindResult != null ? this.listeningKeybindResult.getTitle() : "Key") + " to [" + newKey + "]";
            this.notificationExpiry = System.currentTimeMillis() + 3000;

            this.listeningKeybindResult = null;
            this.listeningKeyMapping = null;
            this.listeningValueChanged = null;

            if (this.resultList != null) {
                this.resultList.refreshLayout();
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.categoryList != null && this.categoryList.mouseDragged(event, dx, dy)) {
            return true;
        }
        if (this.resultList != null && this.resultList.mouseDragged(event, dx, dy)) {
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.categoryList != null && this.categoryList.mouseReleased(event)) {
            return true;
        }
        if (this.resultList != null && this.resultList.mouseReleased(event)) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        SearchTheme theme = SearchTheme.getCurrent();

        // Screen atmosphere
        this.extractTransparentBackground(graphics);
        graphics.fill(0, 0, this.width, this.height, theme.getScreenBg());
        graphics.fillGradient(0, 0, this.width, 36, theme.getActiveBtnGradTop() & 0x66FFFFFF, 0x00000000);

        // Header Title: ✦ GLOBAL SEARCH ✦
        graphics.centeredText(this.font, Component.literal("✦ GLOBAL SEARCH ✦").withStyle(ChatFormatting.BOLD), this.width / 2, 7, theme.getHeaderColor());

        // Search Bar Outer Frame with rounded box
        boolean searchFocused = this.searchBox != null && this.searchBox.isFocused();
        int borderColor = searchFocused ? theme.getSearchBorderFocus() : theme.getSearchBorder();
        GuiRenderUtils.drawRoundedCard(graphics, searchBoxX, searchBoxY, searchBoxW, searchBoxH, theme.getSearchBg(), borderColor, 0x22FFFFFF);

        // Search icon 🔍
        graphics.text(this.font, "🔍", searchBoxX + 5, searchBoxY + 6, searchFocused ? theme.getSearchBorderFocus() : theme.getMutedTextColor());

        // Left Navigation Card Background & Border (Rounded Panel)
        GuiRenderUtils.drawRoundedCard(graphics, navX, navY, navWidth, navHeight, theme.getPanelBg(), theme.getPanelBorder(), 0x11FFFFFF);
        graphics.fill(navX + 1, navY, navX + navWidth - 1, navY + 1, theme.getPanelBorderTop());

        // Navigator Header Label
        graphics.text(this.font, Component.literal("⚔ CATEGORIES").withStyle(ChatFormatting.BOLD), navX + 8, navY + 6, theme.getAccentColor());

        // Render widgets (search box, theme button, rescan button, filter buttons, checkbox, result list)
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // Clear cross icon on right-hand side inside search input box
        if (this.searchBox != null && !this.searchBox.getValue().isEmpty()) {
            int clearBtnSize = 14;
            int clearBtnX = searchBoxX + searchBoxW - clearBtnSize - 4;
            int clearBtnY = searchBoxY + (searchBoxH - clearBtnSize) / 2;
            boolean isClearHovered = mouseX >= clearBtnX && mouseX <= clearBtnX + clearBtnSize &&
                                     mouseY >= clearBtnY && mouseY <= clearBtnY + clearBtnSize;
            if (isClearHovered) {
                graphics.fill(clearBtnX, clearBtnY, clearBtnX + clearBtnSize, clearBtnY + clearBtnSize, 0x44D97706);
            }
            graphics.centeredText(this.font, "✕", clearBtnX + (clearBtnSize / 2), clearBtnY + (clearBtnSize - 8) / 2, isClearHovered ? 0xFFFFF0D0 : theme.getMutedTextColor());
        }

        // Footer Bar (Theme Slate with Accent Border)
        int footerH = 18;
        int footerY = this.height - footerH;
        graphics.fill(0, footerY, this.width, this.height, theme.getPanelBg());
        graphics.fill(0, footerY, this.width, footerY + 1, theme.getPanelBorder());

        int textY = footerY + (footerH - 8) / 2;
        if (listeningKeybindResult != null) {
            float pulse = (float) (0.6 + 0.4 * Math.sin(System.currentTimeMillis() * 0.008));
            int alpha = (int) (pulse * 255);
            int pulseColor = (alpha << 24) | (theme.getAccentColor() & 0x00FFFFFF);

            Component listeningComp = Component.literal("▶ Press any key to bind to ").withStyle(style -> style.withColor(theme.getTitleColor()))
                    .append(Component.literal("'" + listeningKeybindResult.getTitle() + "'").withStyle(style -> style.withColor(theme.getAccentColor()).withBold(true)))
                    .append(Component.literal("  •  [Esc] ").withStyle(style -> style.withColor(theme.getAccentColor())))
                    .append(Component.literal("unbind  •  ").withStyle(style -> style.withColor(theme.getMutedTextColor())))
                    .append(Component.literal("[Click Edit] ").withStyle(style -> style.withColor(theme.getAccentColor())))
                    .append(Component.literal("cancel").withStyle(style -> style.withColor(theme.getMutedTextColor())));

            int textW = this.font.width(listeningComp);
            int pillX = (this.width - textW) / 2 - 8;
            int pillW = textW + 16;
            int pillH = 14;
            int pillY = footerY + (footerH - pillH) / 2;
            int pillBg = ((alpha / 5) << 24) | (theme.getActiveBtnGradTop() & 0x00FFFFFF);
            int pillBorder = (alpha << 24) | (theme.getAccentColor() & 0x00FFFFFF);
            GuiRenderUtils.drawRoundedBox(graphics, pillX, pillY, pillW, pillH, pillBg, pillBorder);

            graphics.centeredText(this.font, listeningComp, this.width / 2, textY, pulseColor);
        } else if (isIndexing) {
            Component indexingComp = Component.literal("⚡ Indexing entries in background...").withStyle(style -> style.withColor(theme.getAccentColor()).withItalic(true));
            graphics.centeredText(this.font, indexingComp, this.width / 2, textY, theme.getAccentColor());
        } else if (copiedNotification != null && System.currentTimeMillis() < notificationExpiry) {
            String notification = copiedNotification.startsWith("Rebound") || copiedNotification.startsWith("Updated") || copiedNotification.startsWith("Rescanned") || copiedNotification.startsWith("Keybind")
                    ? "✓ " + copiedNotification
                    : "✓ Copied to clipboard: " + copiedNotification;
            Component notifComp = Component.literal(notification).withStyle(style -> style.withColor(theme.getAccentColor()).withBold(true));
            graphics.centeredText(this.font, notifComp, this.width / 2, textY, theme.getAccentColor());
        } else {
            String stats = "Showing " + filteredResults.size() + " of " + allIndexedResults.size() + " results";
            graphics.text(this.font, stats, 14, textY, theme.getMutedTextColor());

            Component hints = Component.literal("[F6] ").withStyle(style -> style.withColor(theme.getAccentColor()))
                    .append(Component.literal("Theme   ").withStyle(style -> style.withColor(theme.getMutedTextColor())))
                    .append(Component.literal("[Enter] ").withStyle(style -> style.withColor(theme.getAccentColor())))
                    .append(Component.literal("Select/Edit   ").withStyle(style -> style.withColor(theme.getMutedTextColor())))
                    .append(Component.literal("[↑ ↓] ").withStyle(style -> style.withColor(theme.getAccentColor())))
                    .append(Component.literal("Nav   ").withStyle(style -> style.withColor(theme.getMutedTextColor())))
                    .append(Component.literal("[Esc] ").withStyle(style -> style.withColor(theme.getAccentColor())))
                    .append(Component.literal("Close").withStyle(style -> style.withColor(theme.getMutedTextColor())));
            int hintsW = this.font.width(hints);
            graphics.text(this.font, hints, this.width - hintsW - 14, textY, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parentScreen);
        }
    }
}
