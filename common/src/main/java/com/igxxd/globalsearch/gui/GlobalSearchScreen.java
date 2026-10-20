package com.igxxd.globalsearch.gui;

import com.igxxd.globalsearch.search.SearchIndexer;
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
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

    private String copiedNotification = null;
    private long notificationExpiry = 0;

    public GlobalSearchScreen(Screen parentScreen) {
        super(Component.literal("Global Search"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        if (this.minecraft != null) {
            this.allIndexedResults = SearchIndexer.getOrIndex(this.minecraft);
        }

        // Expanded panel length/width to fill almost the entire screen nicely
        int panelWidth = Math.max(280, this.width - 28);
        int panelLeft = (this.width - panelWidth) / 2;

        int rescanBtnWidth = 64;
        int searchBoxWidth = panelWidth - rescanBtnWidth - 6;

        // 1. Search Box
        int searchBoxY = 26;
        this.searchBox = new EditBox(this.font, panelLeft, searchBoxY, searchBoxWidth, 20, Component.literal("Search"));
        this.searchBox.setHint(Component.literal("Search keybinds, settings, items, mods...").withStyle(ChatFormatting.DARK_GRAY));
        this.searchBox.setResponder(text -> updateFilter());
        this.addRenderableWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);

        // 1b. Rescan Button
        Button rescanBtn = Button.builder(Component.literal("↻ Rescan"), b -> triggerRescan())
                .bounds(panelLeft + searchBoxWidth + 6, searchBoxY, rescanBtnWidth, 20)
                .build();
        this.addRenderableWidget(rescanBtn);

        // 2. Filter Category Buttons
        int buttonY = 50;
        int btnWidth = (panelWidth - 20) / 5;
        SearchResultType[] filterTypes = new SearchResultType[]{
                SearchResultType.ALL,
                SearchResultType.KEYBIND,
                SearchResultType.SETTING,
                SearchResultType.ITEM,
                SearchResultType.MOD
        };

        for (int i = 0; i < filterTypes.length; i++) {
            SearchResultType type = filterTypes[i];
            int btnX = panelLeft + i * (btnWidth + 5);
            Button btn = Button.builder(getFilterButtonText(type), b -> {
                this.currentFilter = type;
                this.rebuildWidgets();
            }).bounds(btnX, buttonY, btnWidth, 18).build();
            this.addRenderableWidget(btn);
        }

        // 3. Result List (multiline with itemHeight = 36)
        int listY = 72;
        int listHeight = this.height - listY - 26;
        this.resultList = new SearchResultList(
                this.minecraft,
                panelWidth,
                listHeight,
                listY,
                36,
                this::copyResultValue
        );
        this.resultList.setX(panelLeft);
        this.addRenderableWidget(this.resultList);

        // Populate initial filtered results
        updateFilter();
    }

    private Component getFilterButtonText(SearchResultType type) {
        boolean active = this.currentFilter == type;
        ChatFormatting color = active ? ChatFormatting.YELLOW : ChatFormatting.GRAY;
        String prefix = active ? "▶ " : "";
        return Component.literal(prefix + type.getLabel()).withStyle(color);
    }

    private void updateFilter() {
        filteredResults.clear();
        String query = searchBox != null ? searchBox.getValue().trim().toLowerCase(Locale.ROOT) : "";
        String[] tokens = query.isEmpty() ? new String[0] : query.split("\\s+");

        for (SearchResult result : allIndexedResults) {
            if (currentFilter != SearchResultType.ALL && result.getType() != currentFilter) {
                continue;
            }
            if (result.matches(tokens)) {
                filteredResults.add(result);
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
        if (event.key() == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (event.key() == InputConstants.KEY_RETURN) {
            if (resultList != null && resultList.getSelected() != null) {
                copyResultValue(resultList.getSelected().getResult());
                return true;
            }
        }

        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Dim the background
        this.extractTransparentBackground(graphics);

        // Header Title
        graphics.centeredText(this.font, Component.literal("Global Search").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), this.width / 2, 10, 0xFFFFFFFF);

        // Render widgets (search box, filter buttons, list)
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // Footer status / instructions
        int footerY = this.height - 16;
        if (copiedNotification != null && System.currentTimeMillis() < notificationExpiry) {
            String notification = "✓ Copied to clipboard: " + copiedNotification;
            graphics.centeredText(this.font, Component.literal(notification).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), this.width / 2, footerY, 0xFF55FF55);
        } else {
            String countText = String.format("Showing %d of %d results • Press Enter or Click to copy • Esc to close", filteredResults.size(), allIndexedResults.size());
            graphics.centeredText(this.font, Component.literal(countText).withStyle(ChatFormatting.DARK_GRAY), this.width / 2, footerY, 0xFFAAAAAA);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parentScreen);
        }
    }
}
