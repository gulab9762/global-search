package com.igxxd.globalsearch.search.provider;

import java.util.HashMap;
import java.util.Map;

public final class VanillaEnchantmentDescriptions {

    private static final Map<String, String> DESCRIPTIONS = new HashMap<>();

    static {
        // Armor
        DESCRIPTIONS.put("protection", "Reduces damage from most general sources (physical attacks, environmental hazards, and mobs).");
        DESCRIPTIONS.put("fire_protection", "Reduces fire, lava, and blaze attack damage, and substantially decreases burning duration.");
        DESCRIPTIONS.put("feather_falling", "Reduces fall damage and damage from ender pearl teleportation.");
        DESCRIPTIONS.put("blast_protection", "Reduces damage from explosions and fireworks, and reduces explosion knockback distance.");
        DESCRIPTIONS.put("projectile_protection", "Reduces damage taken from arrows, tridents, shulker bullets, and other projectiles.");
        DESCRIPTIONS.put("respiration", "Extends underwater breathing time and reduces suffocation damage from drowning.");
        DESCRIPTIONS.put("aqua_affinity", "Increases underwater block mining speed to normal surface mining rate.");
        DESCRIPTIONS.put("thorns", "Damages attackers when the wearer is struck, at the cost of slight armor durability.");
        DESCRIPTIONS.put("depth_strider", "Increases underwater movement, walking, and swimming speed.");
        DESCRIPTIONS.put("frost_walker", "Freezes water blocks into frosted ice beneath feet and prevents magma block damage.");
        DESCRIPTIONS.put("soul_speed", "Increases movement speed when walking across soul sand and soul soil.");
        DESCRIPTIONS.put("swift_sneak", "Increases movement speed while sneaking or crouching.");

        // Melee Weapons (Sword & Axe)
        DESCRIPTIONS.put("sharpness", "Increases general melee damage dealt to all enemies.");
        DESCRIPTIONS.put("smite", "Increases melee damage dealt specifically to undead mobs (zombies, skeletons, withers, phantoms, etc.).");
        DESCRIPTIONS.put("bane_of_arthropods", "Increases melee damage and applies slowness to arthropods (spiders, cave spiders, silverfish, bees, endermites).");
        DESCRIPTIONS.put("knockback", "Knocks back struck targets away from the attacker.");
        DESCRIPTIONS.put("fire_aspect", "Sets struck targets on fire and causes killed livestock to drop cooked meat.");
        DESCRIPTIONS.put("looting", "Increases mob item drops and chances of rare loot drops upon death.");
        DESCRIPTIONS.put("sweeping_edge", "Increases damage dealt to multiple mobs by sword sweep attacks.");

        // Tools
        DESCRIPTIONS.put("efficiency", "Increases mining, digging, and chopping speed for the appropriate tool.");
        DESCRIPTIONS.put("silk_touch", "Causes mined blocks to drop themselves rather than their usual secondary items (e.g. glass, ore blocks).");
        DESCRIPTIONS.put("unbreaking", "Gives equipment a chance to avoid durability loss when used.");
        DESCRIPTIONS.put("fortune", "Increases the amount of item drops when mining specific blocks (coal, diamonds, redstone, crops, etc.).");

        // Bow & Crossbow
        DESCRIPTIONS.put("power", "Increases damage dealt by bow arrows.");
        DESCRIPTIONS.put("punch", "Increases the knockback distance applied to targets hit by bow arrows.");
        DESCRIPTIONS.put("flame", "Shoots flaming arrows that ignite targets and detonate TNT blocks.");
        DESCRIPTIONS.put("infinity", "Allows shooting regular arrows without consuming ammunition (requires at least 1 arrow in inventory).");
        DESCRIPTIONS.put("multishot", "Fires three arrows or firework rockets simultaneously at the cost of only one ammunition.");
        DESCRIPTIONS.put("quick_charge", "Decreases crossbow reloading and charging time.");
        DESCRIPTIONS.put("piercing", "Allows crossbow arrows to pass through multiple entities and be picked up after shooting.");

        // Fishing Rod & Trident
        DESCRIPTIONS.put("luck_of_the_sea", "Increases the chance of catching valuable treasure while fishing and decreases junk.");
        DESCRIPTIONS.put("lure", "Decreases the wait time before a fish bites the fishing hook.");
        DESCRIPTIONS.put("loyalty", "Causes thrown tridents to automatically return to the player upon impact.");
        DESCRIPTIONS.put("impaling", "Deals additional damage to aquatic mobs and targets in water or rain.");
        DESCRIPTIONS.put("riptide", "Launches the player forward through the air or water when throwing a trident in water or rain.");
        DESCRIPTIONS.put("channeling", "Strikes struck targets with a lightning bolt when thrown during thunderstorms.");

        // Mace (Minecraft 1.21 / 26.3)
        DESCRIPTIONS.put("density", "Increases smash damage dealt by the mace proportional to fall distance.");
        DESCRIPTIONS.put("breach", "Reduces the effective armor protection of targets struck by the mace.");
        DESCRIPTIONS.put("wind_burst", "Emits an upward wind burst upon landing a successful mace smash attack, launching the attacker into the air.");

        // General & Curses
        DESCRIPTIONS.put("mending", "Repairs item durability using collected experience orbs.");
        DESCRIPTIONS.put("curse_of_binding", "Prevents the cursed armor item from being removed from an armor slot until death.");
        DESCRIPTIONS.put("curse_of_vanishing", "Causes the item to disappear permanently upon player death instead of dropping.");
    }

    private VanillaEnchantmentDescriptions() {}

    public static String getDescription(String path) {
        if (path == null) return null;
        String key = path.toLowerCase();
        if (key.startsWith("minecraft:")) {
            key = key.substring(10);
        }
        return DESCRIPTIONS.get(key);
    }
}
