package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public class AdvancementIndexProvider implements SearchIndexProvider {

    @Override
    public String getName() {
        return "Advancements";
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        if (minecraft == null || minecraft.player == null || minecraft.player.connection == null) return;
        ClientAdvancements advManager = minecraft.player.connection.getAdvancements();
        if (advManager == null) return;

        Map<AdvancementHolder, AdvancementProgress> progressMap = advManager.progress();
        if (progressMap == null || progressMap.isEmpty()) return;

        for (Map.Entry<AdvancementHolder, AdvancementProgress> entry : progressMap.entrySet()) {
            try {
                AdvancementHolder holder = entry.getKey();
                AdvancementProgress progress = entry.getValue();
                if (holder == null || progress == null) continue;

                Advancement adv = holder.value();
                Identifier id = holder.id();
                String path = id.toString();
                String key = SearchResultType.ADVANCEMENT.name() + ":" + path.toLowerCase(Locale.ROOT);

                if (!seenKeys.add(key)) continue;

                Optional<DisplayInfo> displayOpt = adv.display();
                String title;
                String description = "";
                ItemStack icon;
                String advType = "Task";

                if (displayOpt.isPresent()) {
                    DisplayInfo display = displayOpt.get();
                    title = display.title().getString();
                    description = display.description().getString();
                    try {
                        icon = display.icon().create();
                    } catch (Throwable t) {
                        icon = new ItemStack(Items.TARGET);
                    }
                    if (icon == null || icon.isEmpty()) {
                        icon = new ItemStack(Items.TARGET);
                    }
                    if (display.type() != null) {
                        advType = ProviderUtils.formatCamelCase(display.type().name());
                    }
                } else {
                    title = ProviderUtils.formatCamelCase(id.getPath());
                    icon = new ItemStack(Items.KNOWLEDGE_BOOK);
                }

                // Progress calculation
                boolean isDone = progress.isDone();
                float percent = progress.getPercent() * 100.0f;

                int completedCount = 0;
                List<String> completedList = new ArrayList<>();
                for (String c : progress.getCompletedCriteria()) {
                    completedCount++;
                    completedList.add(c);
                }

                int remainingCount = 0;
                List<String> remainingList = new ArrayList<>();
                for (String r : progress.getRemainingCriteria()) {
                    remainingCount++;
                    remainingList.add(r);
                }
                int totalCount = completedCount + remainingCount;

                String val;
                if (isDone) {
                    val = "✓ Completed";
                } else if (progress.hasProgress()) {
                    val = String.format(Locale.ROOT, "○ In Progress (%.0f%%)", percent);
                } else {
                    val = "✕ Not Started";
                }

                // Detailed description formatted for card and expanded view
                StringBuilder descBuilder = new StringBuilder();
                if (!description.isEmpty()) {
                    descBuilder.append(description).append(" • ");
                }
                descBuilder.append("Type: ").append(advType)
                        .append(" • Progress: ").append(completedCount).append("/").append(totalCount)
                        .append(String.format(Locale.ROOT, " (%.1f%%)", percent));

                if (!completedList.isEmpty()) {
                    descBuilder.append(" • Completed: ").append(String.join(", ", completedList));
                }
                if (!remainingList.isEmpty()) {
                    descBuilder.append(" • Remaining: ").append(String.join(", ", remainingList));
                }

                String modId = id.getNamespace();
                String modName = modId.equals("minecraft") ? "Minecraft" : ProviderUtils.formatCamelCase(modId);

                results.add(new SearchResult(
                        SearchResultType.ADVANCEMENT,
                        path,
                        title,
                        path,
                        val,
                        modId,
                        modName,
                        icon,
                        descBuilder.toString()
                ));
            } catch (Throwable ignored) {}
        }
    }
}
