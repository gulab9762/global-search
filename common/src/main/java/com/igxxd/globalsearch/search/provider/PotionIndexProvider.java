package com.igxxd.globalsearch.search.provider;

import com.igxxd.globalsearch.platform.Services;
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class PotionIndexProvider implements SearchIndexProvider {

    @Override
    public String getName() {
        return "Potions & Effects";
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        // 1. Potions
        for (Holder.Reference<Potion> holder : BuiltInRegistries.POTION.listElements().toList()) {
            try {
                Potion potion = holder.value();
                Identifier id = holder.key().identifier();
                String modId = id.getNamespace();
                String modName = Services.PLATFORM.getModName(modId);

                String transKey = "item.minecraft.potion.effect." + potion.name();
                String title = Component.translatable(transKey).getString();
                if (title.equals(transKey)) {
                    title = "Potion of " + ProviderUtils.formatCamelCase(potion.name().replace("strong_", "").replace("long_", ""));
                }

                StringBuilder valBuilder = new StringBuilder();
                List<MobEffectInstance> effects = potion.getEffects();
                if (!effects.isEmpty()) {
                    for (int i = 0; i < effects.size(); i++) {
                        MobEffectInstance inst = effects.get(i);
                        if (i > 0) valBuilder.append(", ");
                        valBuilder.append(inst.getEffect().value().getDisplayName().getString());
                        if (inst.getAmplifier() > 0) {
                            valBuilder.append(" ").append(inst.getAmplifier() + 1);
                        }
                    }
                } else {
                    valBuilder.append("No Effects");
                }
                String val = valBuilder.toString();
                String path = id.toString();
                String key = SearchResultType.POTION.name() + ":" + path.toLowerCase(Locale.ROOT);

                if (seenKeys.add(key)) {
                    String tags = holder.tags()
                            .map(t -> "#" + t.location().getPath())
                            .collect(Collectors.joining(" "));

                    String desc = "Potion: " + title + " • Effects: " + val;
                    if (!tags.isEmpty()) {
                        desc += " • Tags: " + tags;
                    }

                    ItemStack icon = PotionContents.createItemStack(Items.POTION, holder);

                    results.add(new SearchResult(
                            SearchResultType.POTION,
                            path,
                            title,
                            path,
                            val,
                            modId,
                            modName,
                            icon,
                            desc
                    ));
                }
            } catch (Throwable ignored) {}
        }

        // 2. Status Effects (MobEffect)
        for (Holder.Reference<MobEffect> holder : BuiltInRegistries.MOB_EFFECT.listElements().toList()) {
            try {
                MobEffect effect = holder.value();
                Identifier id = holder.key().identifier();
                String modId = id.getNamespace();
                String modName = Services.PLATFORM.getModName(modId);
                String title = effect.getDisplayName().getString();
                String category = effect.getCategory().name();
                String val = ProviderUtils.formatCamelCase(category);
                String path = id.toString();
                String key = SearchResultType.POTION.name() + ":effect:" + path.toLowerCase(Locale.ROOT);

                if (seenKeys.add(key)) {
                    String tags = holder.tags()
                            .map(t -> "#" + t.location().getPath())
                            .collect(Collectors.joining(" "));

                    String descKey = effect.getDescriptionId() + ".description";
                    Language lang = Language.getInstance();
                    String desc = lang.has(descKey) ? lang.getOrDefault(descKey) : "";
                    if (desc.isEmpty()) {
                        desc = val + " Status Effect (" + title + ")";
                    }
                    if (!tags.isEmpty()) {
                        desc += " • Tags: " + tags;
                    }

                    results.add(new SearchResult(
                            SearchResultType.POTION,
                            "effect:" + path,
                            title + " (Effect)",
                            path,
                            val,
                            modId,
                            modName,
                            new ItemStack(Items.SPLASH_POTION),
                            desc
                    ));
                }
            } catch (Throwable ignored) {}
        }
    }
}
