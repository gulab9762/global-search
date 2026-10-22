package com.igxxd.globalsearch.gui;

/**
 * Visual themes for Global Search UI matching the modern design templates.
 */
public enum SearchTheme {

    OBSIDIAN_GLASS(
            "Obsidian Glass",
            "💎",
            0x550C0F18, // screenBg (more translucent dark glass overlay)
            0x77121622, // panelBg (frosted translucent panel)
            0xAA1E2638, // panelBorder
            0xDD2563EB, // panelBorderTop
            0x66181D2C, // cardBg (translucent glass card)
            0x9921283C, // cardHoverBg
            0xBB263248, // cardBorder
            0xFF3B82F6, // cardBorderHover
            0x77141926, // searchBg
            0xAA263248, // searchBorder
            0xFF3B82F6, // searchBorderFocus
            0xFF60A5FA, // headerColor
            0xFF38BDF8, // accentColor
            0xFFF8FAFC, // titleColor
            0xFF94A3B8, // mutedTextColor
            0xCC1D4ED8, // activeBtnGradTop
            0xCC1E3A8A, // activeBtnGradBottom
            0xFF3B82F6, // activeBtnBorder
            0xFF93C5FD  // activeBtnText
    ),

    NETHERITE_RPG(
            "Netherite RPG",
            "⚔️",
            0x55141215, // screenBg
            0x77181518, // panelBg
            0xAA3D2C25, // panelBorder
            0xDD5A3D31, // panelBorderTop
            0x661E191D, // cardBg
            0x99272025, // cardHoverBg
            0xBB3A2B25, // cardBorder
            0xFF8C5738, // cardBorderHover
            0x771A1619, // searchBg
            0xAA5A3D31, // searchBorder
            0xFFD97706, // searchBorderFocus
            0xFFFFAA00, // headerColor
            0xFFFBBF24, // accentColor
            0xFFFFF8F0, // titleColor
            0xFFA89F91, // mutedTextColor
            0xCC442A22, // activeBtnGradTop
            0xCC2D1C17, // activeBtnGradBottom
            0xFFB45309, // activeBtnBorder
            0xFFFBBF24  // activeBtnText
    ),

    RAYCAST_CLEAN(
            "Raycast Clean",
            "⚡",
            0x550F1115, // screenBg
            0x7712151B, // panelBg
            0xAA1E232D, // panelBorder
            0xDD10B981, // panelBorderTop
            0x66141820, // cardBg
            0x991A202C, // cardHoverBg
            0xBB1F2532, // cardBorder
            0xFF2E384D, // cardBorderHover
            0x77161922, // searchBg
            0xAA282F3D, // searchBorder
            0xFF10B981, // searchBorderFocus
            0xFF34D399, // headerColor
            0xFF10B981, // accentColor
            0xFFF1F5F9, // titleColor
            0xFF8A99AD, // mutedTextColor
            0xCC1B3D33, // activeBtnGradTop
            0xCC132B24, // activeBtnGradBottom
            0xFF10B981, // activeBtnBorder
            0xFF6EE7B7  // activeBtnText
    ),

    ENDER_TWILIGHT(
            "Ender Twilight",
            "🔮",
            0x550D0A17, // screenBg
            0x77120E1D, // panelBg
            0xAA2E1C4D, // panelBorder
            0xDDA855F7, // panelBorderTop
            0x661C1430, // cardBg
            0x99261B42, // cardHoverBg
            0xBB341F59, // cardBorder
            0xFFA855F7, // cardBorderHover
            0x771A132C, // searchBg
            0xAA3A2463, // searchBorder
            0xFFA855F7, // searchBorderFocus
            0xFFC084FC, // headerColor
            0xFFE879F9, // accentColor
            0xFFFAF5FF, // titleColor
            0xFFA79BBB, // mutedTextColor
            0xCC4A1D96, // activeBtnGradTop
            0xCC2E1065, // activeBtnGradBottom
            0xFFA855F7, // activeBtnBorder
            0xFFE9D5FF  // activeBtnText
    );

    private static SearchTheme currentTheme = OBSIDIAN_GLASS;

    private final String displayName;
    private final String icon;
    private final int screenBg;
    private final int panelBg;
    private final int panelBorder;
    private final int panelBorderTop;
    private final int cardBg;
    private final int cardHoverBg;
    private final int cardBorder;
    private final int cardBorderHover;
    private final int searchBg;
    private final int searchBorder;
    private final int searchBorderFocus;
    private final int headerColor;
    private final int accentColor;
    private final int titleColor;
    private final int mutedTextColor;
    private final int activeBtnGradTop;
    private final int activeBtnGradBottom;
    private final int activeBtnBorder;
    private final int activeBtnText;

    SearchTheme(String displayName, String icon, int screenBg, int panelBg, int panelBorder, int panelBorderTop,
                int cardBg, int cardHoverBg, int cardBorder, int cardBorderHover,
                int searchBg, int searchBorder, int searchBorderFocus,
                int headerColor, int accentColor, int titleColor, int mutedTextColor,
                int activeBtnGradTop, int activeBtnGradBottom, int activeBtnBorder, int activeBtnText) {
        this.displayName = displayName;
        this.icon = icon;
        this.screenBg = screenBg;
        this.panelBg = panelBg;
        this.panelBorder = panelBorder;
        this.panelBorderTop = panelBorderTop;
        this.cardBg = cardBg;
        this.cardHoverBg = cardHoverBg;
        this.cardBorder = cardBorder;
        this.cardBorderHover = cardBorderHover;
        this.searchBg = searchBg;
        this.searchBorder = searchBorder;
        this.searchBorderFocus = searchBorderFocus;
        this.headerColor = headerColor;
        this.accentColor = accentColor;
        this.titleColor = titleColor;
        this.mutedTextColor = mutedTextColor;
        this.activeBtnGradTop = activeBtnGradTop;
        this.activeBtnGradBottom = activeBtnGradBottom;
        this.activeBtnBorder = activeBtnBorder;
        this.activeBtnText = activeBtnText;
    }

    public static SearchTheme getCurrent() {
        return currentTheme;
    }

    public static void setCurrent(SearchTheme theme) {
        if (theme != null) {
            currentTheme = theme;
        }
    }

    public SearchTheme next() {
        SearchTheme[] values = values();
        int nextIdx = (this.ordinal() + 1) % values.length;
        return values[nextIdx];
    }

    public String getDisplayName() { return displayName; }
    public String getIcon() { return icon; }
    public int getScreenBg() { return screenBg; }
    public int getPanelBg() { return panelBg; }
    public int getPanelBorder() { return panelBorder; }
    public int getPanelBorderTop() { return panelBorderTop; }
    public int getCardBg() { return cardBg; }
    public int getCardHoverBg() { return cardHoverBg; }
    public int getCardBorder() { return cardBorder; }
    public int getCardBorderHover() { return cardBorderHover; }
    public int getSearchBg() { return searchBg; }
    public int getSearchBorder() { return searchBorder; }
    public int getSearchBorderFocus() { return searchBorderFocus; }
    public int getHeaderColor() { return headerColor; }
    public int getAccentColor() { return accentColor; }
    public int getTitleColor() { return titleColor; }
    public int getMutedTextColor() { return mutedTextColor; }
    public int getActiveBtnGradTop() { return activeBtnGradTop; }
    public int getActiveBtnGradBottom() { return activeBtnGradBottom; }
    public int getActiveBtnBorder() { return activeBtnBorder; }
    public int getActiveBtnText() { return activeBtnText; }
}
