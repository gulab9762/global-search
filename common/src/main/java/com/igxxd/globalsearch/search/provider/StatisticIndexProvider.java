package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class StatisticIndexProvider implements SearchIndexProvider {

    @Override
    public String getName() {
        return "Statistics";
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        if (minecraft == null || minecraft.player == null) return;

        // Request live stats from server (updates client cache over network if on multiplayer)
        if (minecraft.getConnection() != null) {
            try {
                minecraft.getConnection().send(new net.minecraft.network.protocol.game.ServerboundClientCommandPacket(
                        net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action.REQUEST_STATS
                ));
            } catch (Throwable ignored) {}
        }

        // Live stats in Singleplayer directly from ServerPlayer
        StatsCounter stats = null;
        if (minecraft.getSingleplayerServer() != null) {
            try {
                var sPlayer = minecraft.getSingleplayerServer().getPlayerList().getPlayer(minecraft.player.getUUID());
                if (sPlayer != null) {
                    stats = sPlayer.getStats();
                }
            } catch (Throwable ignored) {}
        }
        if (stats == null) {
            stats = minecraft.player.getStats();
        }
        if (stats == null) return;

        // 1. Custom Player Statistics
        Identifier[] customStats = new Identifier[]{
                Stats.PLAY_TIME,
                Stats.TOTAL_WORLD_TIME,
                Stats.TIME_SINCE_DEATH,
                Stats.DEATHS,
                Stats.MOB_KILLS,
                Stats.PLAYER_KILLS,
                Stats.DAMAGE_DEALT,
                Stats.DAMAGE_TAKEN,
                Stats.JUMP,
                Stats.WALK_ONE_CM,
                Stats.SPRINT_ONE_CM,
                Stats.FLY_ONE_CM,
                Stats.SWIM_ONE_CM,
                Stats.AVIATE_ONE_CM,
                Stats.FALL_ONE_CM,
                Stats.CLIMB_ONE_CM,
                Stats.FISH_CAUGHT,
                Stats.ANIMALS_BRED,
                Stats.RAID_WIN,
                Stats.RAID_TRIGGER,
                Stats.ENCHANT_ITEM,
                Stats.OPEN_CHEST,
                Stats.OPEN_SHULKER_BOX,
                Stats.OPEN_BARREL,
                Stats.SLEEP_IN_BED,
                Stats.TRADED_WITH_VILLAGER,
                Stats.TALKED_TO_VILLAGER,
                Stats.TARGET_HIT,
                Stats.BELL_RING,
                Stats.LEAVE_GAME
        };

        for (Identifier statId : customStats) {
            try {
                int val = stats.getValue(Stats.CUSTOM, statId);
                String path = statId.toString();
                String key = SearchResultType.STATISTIC.name() + ":" + path.toLowerCase(Locale.ROOT);

                if (seenKeys.add(key)) {
                    String transKey = "stat." + statId.getNamespace() + "." + statId.getPath();
                    String title = Component.translatable(transKey).getString();
                    if (title.equals(transKey)) {
                        title = ProviderUtils.formatCamelCase(statId.getPath());
                    }

                    String formattedVal = formatCustomStat(statId.getPath(), val);
                    ItemStack icon = pickStatIcon(statId.getPath());

                    results.add(new SearchResult(
                            SearchResultType.STATISTIC,
                            path,
                            title,
                            path,
                            formattedVal,
                            statId.getNamespace(),
                            "Minecraft Stats",
                            icon,
                            "Player statistic for current world (Raw count: " + val + ")"
                    ));
                }
            } catch (Throwable ignored) {}
        }

        // 2. Notable Blocks Mined (where mined > 0)
        try {
            for (Block block : BuiltInRegistries.BLOCK) {
                int mined = stats.getValue(Stats.BLOCK_MINED, block);
                if (mined > 0) {
                    Identifier id = BuiltInRegistries.BLOCK.getKey(block);
                    if (id != null) {
                        String path = "mined." + id;
                        String key = SearchResultType.STATISTIC.name() + ":" + path.toLowerCase(Locale.ROOT);
                        if (seenKeys.add(key)) {
                            String blockName = block.getName().getString();
                            String title = blockName + " Mined";
                            String formattedVal = String.format(Locale.ROOT, "%,d", mined);
                            results.add(new SearchResult(
                                    SearchResultType.STATISTIC,
                                    path,
                                    title,
                                    path,
                                    formattedVal,
                                    id.getNamespace(),
                                    "Mining Stats",
                                    new ItemStack(block.asItem()),
                                    "Total blocks of " + blockName + " mined in this world: " + mined
                            ));
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 3. Notable Items Crafted (where crafted > 0)
        try {
            for (Item item : BuiltInRegistries.ITEM) {
                int crafted = stats.getValue(Stats.ITEM_CRAFTED, item);
                if (crafted > 0) {
                    Identifier id = BuiltInRegistries.ITEM.getKey(item);
                    if (id != null) {
                        String path = "crafted." + id;
                        String key = SearchResultType.STATISTIC.name() + ":" + path.toLowerCase(Locale.ROOT);
                        if (seenKeys.add(key)) {
                            String itemName = Component.translatable(item.getDescriptionId()).getString();
                            String title = itemName + " Crafted";
                            String formattedVal = String.format(Locale.ROOT, "%,d", crafted);
                            results.add(new SearchResult(
                                    SearchResultType.STATISTIC,
                                    path,
                                    title,
                                    path,
                                    formattedVal,
                                    id.getNamespace(),
                                    "Crafting Stats",
                                    new ItemStack(item),
                                    "Total " + itemName + " crafted in this world: " + crafted
                            ));
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private String formatCustomStat(String path, int val) {
        if (path.endsWith("_one_cm")) {
            if (val >= 100000) {
                return String.format(Locale.ROOT, "%.2f km", val / 100000.0);
            } else if (val >= 100) {
                return String.format(Locale.ROOT, "%.1f m", val / 100.0);
            } else {
                return val + " cm";
            }
        }

        if (path.contains("time") || path.contains("play_time")) {
            long totalSeconds = val / 20;
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;
            if (hours > 0) {
                return String.format(Locale.ROOT, "%dh %dm", hours, minutes);
            } else if (minutes > 0) {
                return String.format(Locale.ROOT, "%dm %ds", minutes, seconds);
            } else {
                return seconds + "s";
            }
        }

        if (path.contains("damage")) {
            return String.format(Locale.ROOT, "%,d (%.1f ❤)", val, val / 2.0);
        }

        return String.format(Locale.ROOT, "%,d", val);
    }

    private ItemStack pickStatIcon(String path) {
        if (path.contains("time")) return new ItemStack(Items.CLOCK);
        if (path.contains("death") || path.contains("kill")) return new ItemStack(Items.SKELETON_SKULL);
        if (path.contains("damage")) return new ItemStack(Items.DIAMOND_SWORD);
        if (path.contains("jump") || path.contains("walk") || path.contains("sprint")) return new ItemStack(Items.LEATHER_BOOTS);
        if (path.contains("fly") || path.contains("aviate")) return new ItemStack(Items.ELYTRA);
        if (path.contains("swim")) return new ItemStack(Items.TURTLE_HELMET);
        if (path.contains("fish")) return new ItemStack(Items.FISHING_ROD);
        if (path.contains("chest") || path.contains("barrel") || path.contains("shulker")) return new ItemStack(Items.CHEST);
        if (path.contains("bed")) return new ItemStack(Items.FEATHER);
        if (path.contains("villager")) return new ItemStack(Items.EMERALD);
        if (path.contains("enchant")) return new ItemStack(Items.ENCHANTED_BOOK);
        if (path.contains("raid")) return new ItemStack(Items.OMINOUS_BOTTLE);
        return new ItemStack(Items.WRITABLE_BOOK);
    }
}
