package com.igxxd.globalsearch.search;

import com.igxxd.globalsearch.search.provider.SearchIndexProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class SearchResult {

    @FunctionalInterface
    public interface ResultEditHandler {
        boolean handleEdit(Minecraft minecraft, Screen screen, Consumer<String> onValueChanged);
    }

    private final SearchResultType type;
    private final String id;
    private final String title;
    private final String path;
    private String value;
    private final String modId;
    private final String modName;
    private final ItemStack initialIcon;
    private final net.minecraft.world.item.Item item;
    private ItemStack icon;
    private String description;
    private final String tags;
    private final String searchTarget;
    private final boolean editable;
    private final ResultEditHandler editHandler;
    private final SearchIndexProvider provider;

    public SearchResult(SearchResultType type, String id, String title, String path, String value, String modId, String modName, ItemStack icon) {
        this(type, id, title, path, value, modId, modName, icon, null, "");
    }

    public SearchResult(SearchResultType type, String id, String title, String path, String value, String modId, String modName, net.minecraft.world.item.Item item, String description) {
        this(type, id, title, path, value, modId, modName, ItemStack.EMPTY, item, description);
    }

    public SearchResult(SearchResultType type, String id, String title, String path, String value, String modId, String modName, ItemStack icon, String description) {
        this(type, id, title, path, value, modId, modName, icon, null, description);
    }

    public SearchResult(SearchResultType type, String id, String title, String path, String value, String modId, String modName, ItemStack icon, net.minecraft.world.item.Item item, String description) {
        this(type, id, title, path, value, modId, modName, icon, item, description, false, null, null);
    }

    public SearchResult(SearchResultType type, String id, String title, String path, String value, String modId, String modName, ItemStack icon, net.minecraft.world.item.Item item, String description, boolean editable, ResultEditHandler editHandler, SearchIndexProvider provider) {
        this.type = type;
        this.id = id != null ? id : "";
        this.title = title != null ? title : "";
        this.path = path != null ? path : "";
        this.value = value != null ? value : "";
        this.modId = modId != null ? modId : "minecraft";
        this.modName = modName != null ? modName : "Minecraft";
        this.initialIcon = icon != null ? icon : ItemStack.EMPTY;
        this.icon = this.initialIcon;
        this.item = item;
        this.editable = editable;
        this.editHandler = editHandler;
        this.provider = provider;
        
        String desc = description != null ? description.trim() : "";
        if (!desc.isEmpty()) {
            net.minecraft.locale.Language lang = net.minecraft.locale.Language.getInstance();
            if (lang.has(desc)) {
                desc = lang.getOrDefault(desc);
            } else if (desc.contains(".")) {
                String translated = Component.translatable(desc).getString();
                if (!translated.equals(desc) && !translated.isEmpty()) {
                    desc = translated;
                }
            }
        }

        String extractedTags = "";
        int tagIdx = desc.indexOf(" • Tags: ");
        if (tagIdx != -1) {
            extractedTags = desc.substring(tagIdx + 9).trim();
            desc = desc.substring(0, tagIdx).trim();
        } else {
            int tagIdx2 = desc.indexOf("Tags: ");
            if (tagIdx2 != -1) {
                extractedTags = desc.substring(tagIdx2 + 6).trim();
                desc = desc.substring(0, tagIdx2).trim();
            }
        }
        this.tags = extractedTags;

        if (desc.isEmpty()) {
            if (!this.path.isEmpty()) {
                desc = this.title + " (" + this.path + ")";
            } else {
                desc = this.title + " (" + this.id + ")";
            }
        }
        this.description = desc;

        // Pre-compute lower-case search target for fast substring and multi-word matching
        this.searchTarget = (this.title + " " + this.path + " " + this.value + " " + this.modName + " " + this.modId + " " + this.id + " " + this.description.replace('\n', ' ') + " " + this.tags + " " + type.name())
                .toLowerCase(Locale.ROOT);
    }

    public SearchResultType getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getPath() {
        return path;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value != null ? value : "";
    }

    public boolean isEditable() {
        return editable || editHandler != null;
    }

    public ResultEditHandler getEditHandler() {
        return editHandler;
    }

    public SearchIndexProvider getProvider() {
        return provider;
    }

    public String getModId() {
        return modId;
    }

    public String getModName() {
        return modName;
    }

    public ItemStack getIcon() {
        if (icon.isEmpty() && item != null) {
            try {
                this.icon = new ItemStack(item);
            } catch (Throwable ignored) {
            }
        }
        if (icon.isEmpty() && this.type == SearchResultType.MOD) {
            if ("minecraft".equalsIgnoreCase(this.modId)) {
                return new ItemStack(net.minecraft.world.item.Items.GRASS_BLOCK);
            }
            return new ItemStack(net.minecraft.world.item.Items.KNOWLEDGE_BOOK);
        }
        return icon;
    }

    public net.minecraft.resources.Identifier getTextureIcon() {
        if (this.type == SearchResultType.MOD) {
            return com.igxxd.globalsearch.gui.ModIconManager.getModIcon(this.modId);
        }
        return null;
    }

    public boolean hasIcon() {
        return !getIcon().isEmpty();
    }

    public boolean matches(String[] tokens) {
        if (tokens.length == 0) return true;
        for (String token : tokens) {
            if (!searchTarget.contains(token)) {
                return false;
            }
        }
        return true;
    }

    public boolean matchesKeyValue(String[] tokens) {
        if (tokens.length == 0) return true;
        String valLower = this.value.toLowerCase(Locale.ROOT);
        for (String token : tokens) {
            if (!valLower.contains(token)) {
                return false;
            }
        }
        return true;
    }

    public String getCopyValue() {
        if (!value.isEmpty()) {
            return value;
        }
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getTags() {
        return tags;
    }

    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }

    public List<Component> getTooltip() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(title).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Type: ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(type.getLabel()).withStyle(ChatFormatting.YELLOW)));

        if (!value.isEmpty()) {
            tooltip.add(Component.literal("Value: ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(value).withStyle(ChatFormatting.AQUA)));
        }

        if (!path.isEmpty()) {
            tooltip.add(Component.literal("Path: ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(path).withStyle(ChatFormatting.GRAY)));
        }

        tooltip.add(Component.literal("Mod: ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(modName).withStyle(ChatFormatting.BLUE))
                .append(Component.literal(" (" + modId + ")").withStyle(ChatFormatting.DARK_GRAY)));

        if (!description.isEmpty()) {
            tooltip.add(Component.literal("Info: ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(description).withStyle(ChatFormatting.GOLD)));
        }

        if (!id.isEmpty() && !id.equals(path)) {
            tooltip.add(Component.literal("ID: ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(id).withStyle(ChatFormatting.DARK_AQUA)));
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Click to copy value to clipboard").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
        return tooltip;
    }

    public static Builder builder(SearchResultType type, String id, String title) {
        return new Builder(type, id, title);
    }

    public static class Builder {
        private final SearchResultType type;
        private final String id;
        private final String title;
        private String path = "";
        private String value = "";
        private String modId = "minecraft";
        private String modName = "Minecraft";
        private ItemStack icon = ItemStack.EMPTY;
        private net.minecraft.world.item.Item item = null;
        private String description = "";
        private boolean editable = false;
        private ResultEditHandler editHandler = null;
        private SearchIndexProvider provider = null;

        public Builder(SearchResultType type, String id, String title) {
            this.type = type;
            this.id = id;
            this.title = title;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder modId(String modId) {
            this.modId = modId;
            return this;
        }

        public Builder modName(String modName) {
            this.modName = modName;
            return this;
        }

        public Builder icon(ItemStack icon) {
            this.icon = icon;
            return this;
        }

        public Builder item(net.minecraft.world.item.Item item) {
            this.item = item;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder editable(boolean editable) {
            this.editable = editable;
            return this;
        }

        public Builder onEdit(ResultEditHandler editHandler) {
            this.editHandler = editHandler;
            this.editable = true;
            return this;
        }

        public Builder provider(SearchIndexProvider provider) {
            this.provider = provider;
            return this;
        }

        public SearchResult build() {
            return new SearchResult(type, id, title, path, value, modId, modName, icon, item, description, editable, editHandler, provider);
        }
    }
}
