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
import java.util.List;
import java.util.Locale;

public class GlobalSearchScreen extends Screen {

    private final Screen parentScreen;
    private List<SearchResult> allIndexedResults = new ArrayList<>();
    private final List<SearchResult> filteredResults = new ArrayList<>();

    private EditBox searchBox;
    private SearchResultList resultList;
    private SearchResultType currentFilter = SearchResultType.ALL;
    private boolean searchByKeyValue = false;
    private String currentSearchQuery = "";

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

    public GlobalSearchScreen(Screen parentScreen) {
        super(Component.literal("Global Search"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        if (this.minecraft != null) {
            this.allIndexedResults = SearchIndexer.getOrIndex(this.minecraft);
        }

        int sideMargin = 12;
        int topMargin = 20;

        // 1. Top Search Bar & Rescan Button
        int rescanBtnWidth = 64;
        int searchBoxWidth = this.width - (sideMargin * 2) - rescanBtnWidth - 6;

        String prevText = this.currentSearchQuery;
        if (this.searchBox != null && !this.searchBox.getValue().isEmpty()) {
            prevText = this.searchBox.getValue();
        }
        this.currentSearchQuery = prevText;

        this.searchBox = new EditBox(this.font, sideMargin, topMargin, searchBoxWidth, 20, Component.literal("Search"));
        this.searchBox.setHint(Component.literal("Search keybinds, settings, items, mods...").withStyle(ChatFormatting.DARK_GRAY));
        if (!this.currentSearchQuery.isEmpty()) {
            this.searchBox.setValue(this.currentSearchQuery);
        }
        this.searchBox.setResponder(text -> {
            this.currentSearchQuery = text;
            updateFilter();
        });
        this.addRenderableWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);

        Button rescanBtn = Button.builder(Component.literal("↻ Rescan"), b -> triggerRescan())
                .bounds(sideMargin + searchBoxWidth + 6, topMargin, rescanBtnWidth, 20)
                .build();
        this.addRenderableWidget(rescanBtn);

        // 2. Left Navigator Panel Dimensions
        navX = sideMargin;
        navY = topMargin + 26;
        navWidth = 132;
        int bottomMargin = 20;
        navHeight = this.height - navY - bottomMargin;

        // 3. Right Side Result List
        int listX = navX + navWidth + 8;
        int listY = navY;
        int listWidth = this.width - listX - sideMargin;
        int listHeight = navHeight;

        // 4. Left Navigator Buttons (Vertical Stack)
        List<SearchResultType> filterTypes = SearchResultType.getAllCategories();

        int btnWidth = navWidth - 8;
        int totalItems = filterTypes.size() + (this.currentFilter.equals(SearchResultType.KEYBIND) ? 1 : 0);
        int availableH = navHeight - 20;
        int btnHeight = Math.max(16, Math.min(20, (availableH - (totalItems * 2)) / Math.max(1, totalItems)));
        int spacing = Math.max(1, Math.min(3, (availableH - (totalItems * btnHeight)) / Math.max(1, totalItems)));
        int btnY = navY + 18;

        for (SearchResultType type : filterTypes) {
            int currentY = btnY;
            Button btn = Button.builder(getFilterButtonText(type), b -> {
                this.currentFilter = type;
                this.rebuildWidgets();
            }).bounds(navX + 4, currentY, btnWidth, btnHeight).build();
            this.addRenderableWidget(btn);
            btnY += btnHeight + spacing;

            // If KEYBIND filter is active, add "By Key Value" Checkbox option underneath
            if (type.equals(SearchResultType.KEYBIND) && this.currentFilter.equals(SearchResultType.KEYBIND)) {
                Checkbox keyValCheckbox = Checkbox.builder(Component.literal("By Key Value"), this.font)
                        .pos(navX + 6, btnY)
                        .selected(this.searchByKeyValue)
                        .onValueChange((cb, val) -> {
                            this.searchByKeyValue = val;
                            updateFilter();
                        })
                        .build();
                this.addRenderableWidget(keyValCheckbox);
                btnY += btnHeight + spacing;
            }
        }

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
        String query = searchBox != null ? searchBox.getValue().trim().toLowerCase(Locale.ROOT) : "";
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

        if (resultList != null) {
            resultList.setResults(filteredResults);
        }
    }

    private void copyResultValue(SearchResult result) {
        if (this.minecraft != null && result != null) {
            String valueToCopy = result.getCopyValue();
            this.minecraft.keyboardHandler.setClipboard(valueToCopy);
            this.copiedNotification = valueToCopy;
            this.notificationExpiry = System.currentTimeMillis() + 3000;
        }
    }

    private void triggerRescan() {
        if (this.minecraft != null) {
            int count = SearchIndexer.rescan(this.minecraft);
            this.allIndexedResults = SearchIndexer.getOrIndex(this.minecraft);
            updateFilter();
            this.copiedNotification = "Rescanned & indexed " + count + " entries";
            this.notificationExpiry = System.currentTimeMillis() + 3000;
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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
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
        if (this.resultList != null && this.resultList.mouseDragged(event, dx, dy)) {
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.resultList != null && this.resultList.mouseReleased(event)) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Dim the background
        this.extractTransparentBackground(graphics);

        // Header Title
        graphics.centeredText(this.font, Component.literal("Global Search").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), this.width / 2, 7, 0xFFFFFFFF);

        // Left Navigation Card Background & Border
        graphics.fill(navX, navY, navX + navWidth, navY + navHeight, 0x44000000);
        graphics.fill(navX, navY, navX + navWidth, navY + 1, 0x33FFFFFF);
        graphics.fill(navX + navWidth - 1, navY, navX + navWidth, navY + navHeight, 0x22FFFFFF);

        // Navigator Header Label
        graphics.text(this.font, Component.literal("FILTERS").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD), navX + 8, navY + 6, 0xFFAAAAAA);

        // Render widgets (search box, rescan button, filter buttons, checkbox, result list)
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // Footer status / instructions
        int footerY = this.height - 14;
        if (listeningKeybindResult != null) {
            String listeningText = "▶ Press any key to bind to '" + listeningKeybindResult.getTitle() + "' • Esc to unbind • Click Edit again to cancel";
            graphics.centeredText(this.font, Component.literal(listeningText).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), this.width / 2, footerY, 0xFFFFFFFF);
        } else if (copiedNotification != null && System.currentTimeMillis() < notificationExpiry) {
            String notification = copiedNotification.startsWith("Rebound") || copiedNotification.startsWith("Updated") || copiedNotification.startsWith("Rescanned") || copiedNotification.startsWith("Keybind")
                    ? "✓ " + copiedNotification
                    : "✓ Copied to clipboard: " + copiedNotification;
            graphics.centeredText(this.font, Component.literal(notification).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), this.width / 2, footerY, 0xFFFFFFFF);
        } else {
            String stats = "Showing " + filteredResults.size() + " of " + allIndexedResults.size() + " results • Click [Edit] to modify • Esc to close";
            graphics.centeredText(this.font, Component.literal(stats).withStyle(ChatFormatting.GRAY), this.width / 2, footerY, 0xFFFFFFFF);
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
