package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.gui.GlobalSettingEditScreen;
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GameRuleIndexProvider implements SearchIndexProvider {

    @Override
    public String getName() {
        return "Game Rules";
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        if (minecraft == null || minecraft.level == null) return;

        try {
            GameRules serverRules = null;
            if (minecraft.getSingleplayerServer() != null) {
                serverRules = minecraft.getSingleplayerServer().getGameRules();
            }

            for (GameRule<?> rule : BuiltInRegistries.GAME_RULE) {
                try {
                    String id = rule.id();
                    String title = ProviderUtils.formatCamelCase(id);
                    String val = rule.defaultValue() != null ? rule.defaultValue().toString() : "";

                    if (serverRules != null) {
                        try {
                            String activeVal = serverRules.getAsString(rule);
                            if (activeVal != null && !activeVal.isEmpty()) {
                                val = activeVal;
                            }
                        } catch (Throwable ignored) {}
                    }

                    String path = id;
                    String searchKey = SearchResultType.GAMERULE.name() + ":" + id.toLowerCase(Locale.ROOT);

                    if (seenKeys.add(searchKey)) {
                        boolean isBoolean = (rule.valueClass() == Boolean.class);
                        GlobalSettingEditScreen.SettingType settingType = isBoolean
                                ? GlobalSettingEditScreen.SettingType.BOOLEAN
                                : GlobalSettingEditScreen.SettingType.INTEGER;

                        String categoryName = rule.category() != null ? rule.category().label().getString() : "World";
                        String desc = "Game rule for current world: " + id + " (Category: " + categoryName + ", Default: " + rule.defaultValue() + ")";

                        final String initialVal = val;
                        SearchResult result = new SearchResult(
                                SearchResultType.GAMERULE,
                                id,
                                title,
                                path,
                                initialVal,
                                "minecraft",
                                "Minecraft World",
                                new ItemStack(isBoolean ? Items.REPEATING_COMMAND_BLOCK : Items.COMMAND_BLOCK),
                                null,
                                desc,
                                true,
                                (mc, screen, onValueChanged) -> {
                                    mc.gui.setScreen(new GlobalSettingEditScreen(
                                            screen,
                                            new SearchResult(SearchResultType.GAMERULE, id, title, path, initialVal, "minecraft", "Minecraft World", ItemStack.EMPTY),
                                            settingType,
                                            List.of(),
                                            initialVal,
                                            newVal -> {
                                                if (mc.player != null && mc.player.connection != null) {
                                                    mc.player.connection.sendCommand("gamerule " + id + " " + newVal);
                                                    return true;
                                                }
                                                return false;
                                            },
                                            onValueChanged
                                    ));
                                    return true;
                                },
                                GameRuleIndexProvider.this
                        );
                        results.add(result);
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }
}
