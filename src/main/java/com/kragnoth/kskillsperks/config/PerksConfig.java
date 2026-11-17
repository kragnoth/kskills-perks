package com.kragnoth.kskillsperks.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Common config: config/kskills_perks-common.toml (one file for every world).
 * Per-world overrides: <world>/serverconfig/kskills_perks-overrides.toml (see ConfigOverrides).
 */
public final class PerksConfig {
    private PerksConfig() { }

    public static final ModConfigSpec SPEC;

    // Hollow (1.2.0, Gearwright's Hollow endgame)
    public static final Val<Boolean> HOLLOW_ENABLED;
    public static final Val<String> HOLLOW_VISION;
    public static final Val<String> HOLLOW_VISION_TAG;
    public static final Val<List<? extends Double>> TAKEN_BY_TIER;
    public static final Val<Double> TAKEN_PER_DAY;
    public static final Val<Double> TAKEN_CAP;
    public static final Val<Integer> TAKEN_RADIUS;
    public static final Val<String> RESIDUE_ITEM;
    public static final Val<String> DISTILLED_ITEM;
    public static final Val<List<? extends String>> RESIDUE_BLOCKS;
    public static final Val<List<? extends Double>> RESIDUE_BLOCK_CHANCE;
    public static final Val<List<? extends Double>> RESIDUE_UNDEAD_CHANCE;
    public static final Val<String> REVENANT;
    public static final Val<Double> DISTILLED_UNDEAD;
    public static final Val<Double> DISTILLED_REVENANT;
    public static final Val<Double> DISTILLED_BLOCK;
    public static final Val<Double> LOCKED_UNIQUE_CHANCE;
    public static final Val<List<? extends String>> LOCKED_UNIQUES;
    public static final Val<String> HOLLOWED_SET;

    // Bulwark
    public static final Val<Integer> LAST_STAND_COOLDOWN;
    public static final Val<Double> SHIELD_WALL_REDUCTION;
    public static final Val<Double> SHIELD_WALL_RADIUS;
    public static final Val<Double> IRONCLAD_DURABILITY_SAVE;
    // Warpath
    public static final Val<Integer> BLOODRUSH_MAX_STACKS;
    public static final Val<Double> BLOODRUSH_PER_STACK;
    public static final Val<Integer> BLOODRUSH_DURATION;
    public static final Val<Double> CLEAVE_FRACTION;
    public static final Val<Double> CLEAVE_RADIUS;
    public static final Val<Double> EXECUTE_BONUS_BELOW;
    public static final Val<Double> EXECUTE_KILL_BELOW;
    // Deadeye
    public static final Val<Double> RICOCHET_RANGE;
    public static final Val<Double> MARK_BONUS;
    public static final Val<Integer> MARK_DURATION;
    public static final Val<Double> LONG_SHOT_MAX_BONUS;
    public static final Val<Double> LONG_SHOT_MAX_DISTANCE;
    // Pathfinder (sent to clients on sync)
    public static final Val<Integer> DASH_COOLDOWN;
    public static final Val<Double> DASH_SPEED;
    public static final Val<Double> DOUBLE_JUMP_STRENGTH;
    public static final Val<Double> WALL_SLIDE_SPEED;
    // Foreman
    public static final Val<Integer> PROSPECT_RADIUS;
    public static final Val<Integer> PROSPECT_COOLDOWN;
    public static final Val<Integer> PROSPECT_MAX_BLOCKS;
    public static final Val<Double> AUTO_SMELT_CHANCE;
    public static final Val<Double> TREASURE_CHANCE;
    // Occult
    public static final Val<Double> SOUL_SIPHON_HEAL_FRACTION;
    public static final Val<Double> SOUL_SIPHON_HEAL_MAX;
    public static final Val<Integer> SOUL_SIPHON_LP_PER_HEALTH;
    public static final Val<Integer> ARCANE_WARD_BASE;
    public static final Val<Integer> ARCANE_WARD_MAX;
    public static final Val<Integer> ARCANE_WARD_OUT_OF_COMBAT;
    public static final Val<Double> HEX_CHANCE;
    // Industry
    public static final Val<List<? extends Integer>> CROP_RADIUS;
    public static final Val<List<? extends Double>> CROP_CHANCE;
    public static final Val<Integer> RANCHER_RADIUS;
    public static final Val<Integer> RANCHER_SECONDS;
    public static final Val<List<? extends Integer>> OVERCLOCK_RADIUS;
    public static final Val<List<? extends Integer>> OVERCLOCK_EXTRA_TICKS;
    public static final Val<Integer> OVERCLOCK_MAX_PER_PLAYER;
    public static final Val<List<? extends String>> OVERCLOCK_BLACKLIST_MODS;
    public static final Val<List<? extends Integer>> MAGNET_RADIUS;
    public static final Val<Double> MAGNET_SPEED;
    public static final Val<Integer> HEARTH_RADIUS;
    public static final Val<Integer> STOKER_RADIUS;
    public static final Val<Double> STOKER_BONUS;
    public static final Val<Double> CAREFUL_HANDS_SAVE;

    static {
        var b = new ModConfigSpec.Builder();

        b.push("bulwark");
        LAST_STAND_COOLDOWN = new Val<>(b.comment("Last Stand cooldown in seconds").defineInRange("last_stand_cooldown", 300, 10, 36000));
        SHIELD_WALL_REDUCTION = new Val<>(b.comment("Damage reduction for allies near a blocking Shield Wall player").defineInRange("shield_wall_reduction", 0.25, 0.0, 0.9));
        SHIELD_WALL_RADIUS = new Val<>(b.defineInRange("shield_wall_radius", 4.0, 1.0, 16.0));
        IRONCLAD_DURABILITY_SAVE = new Val<>(b.comment("Chance per durability point that armor takes no wear").defineInRange("ironclad_durability_save", 0.5, 0.0, 1.0));
        b.pop();

        b.push("warpath");
        BLOODRUSH_MAX_STACKS = new Val<>(b.defineInRange("bloodrush_max_stacks", 5, 1, 20));
        BLOODRUSH_PER_STACK = new Val<>(b.comment("Attack damage and attack speed bonus per stack").defineInRange("bloodrush_per_stack", 0.05, 0.0, 1.0));
        BLOODRUSH_DURATION = new Val<>(b.comment("Seconds a stack lasts (refreshed on each kill)").defineInRange("bloodrush_duration", 8, 1, 120));
        CLEAVE_FRACTION = new Val<>(b.comment("Fraction of a melee hit dealt to enemies around the target").defineInRange("cleave_fraction", 0.3, 0.0, 2.0));
        CLEAVE_RADIUS = new Val<>(b.defineInRange("cleave_radius", 2.5, 0.5, 8.0));
        EXECUTE_BONUS_BELOW = new Val<>(b.comment("Double damage against targets below this health fraction").defineInRange("execute_bonus_below", 0.2, 0.0, 1.0));
        EXECUTE_KILL_BELOW = new Val<>(b.comment("Non-boss targets below this health fraction die outright").defineInRange("execute_kill_below", 0.1, 0.0, 1.0));
        b.pop();

        b.push("deadeye");
        RICOCHET_RANGE = new Val<>(b.defineInRange("ricochet_range", 6.0, 1.0, 32.0));
        MARK_BONUS = new Val<>(b.comment("Extra damage marked targets take from everyone").defineInRange("hunters_mark_bonus", 0.2, 0.0, 2.0));
        MARK_DURATION = new Val<>(b.comment("Seconds a mark lasts").defineInRange("hunters_mark_duration", 10, 1, 120));
        LONG_SHOT_MAX_BONUS = new Val<>(b.defineInRange("long_shot_max_bonus", 0.5, 0.0, 5.0));
        LONG_SHOT_MAX_DISTANCE = new Val<>(b.comment("Distance in blocks at which Long Shot reaches its full bonus").defineInRange("long_shot_max_distance", 40.0, 4.0, 256.0));
        b.pop();

        b.push("pathfinder");
        DASH_COOLDOWN = new Val<>(b.comment("Dash cooldown in ticks").defineInRange("dash_cooldown", 80, 5, 1200));
        DASH_SPEED = new Val<>(b.defineInRange("dash_speed", 1.4, 0.2, 4.0));
        DOUBLE_JUMP_STRENGTH = new Val<>(b.defineInRange("double_jump_strength", 0.55, 0.1, 2.0));
        WALL_SLIDE_SPEED = new Val<>(b.comment("Max fall speed while clinging to a wall").defineInRange("wall_slide_speed", 0.08, 0.0, 1.0));
        b.pop();

        b.push("foreman");
        PROSPECT_RADIUS = new Val<>(b.defineInRange("prospector_radius", 12, 2, 32));
        PROSPECT_COOLDOWN = new Val<>(b.comment("Prospector's Sense cooldown in seconds").defineInRange("prospector_cooldown", 10, 0, 600));
        PROSPECT_MAX_BLOCKS = new Val<>(b.defineInRange("prospector_max_blocks", 128, 1, 2048));
        AUTO_SMELT_CHANCE = new Val<>(b.defineInRange("auto_smelt_chance", 0.5, 0.0, 1.0));
        TREASURE_CHANCE = new Val<>(b.comment("Chance per stone block mined to roll kskills_perks:treasure/stone").defineInRange("treasure_chance", 0.01, 0.0, 1.0));
        b.pop();

        b.push("occult");
        SOUL_SIPHON_HEAL_FRACTION = new Val<>(b.comment("Heal this fraction of the victim's max health on kill").defineInRange("soul_siphon_heal_fraction", 0.1, 0.0, 1.0));
        SOUL_SIPHON_HEAL_MAX = new Val<>(b.defineInRange("soul_siphon_heal_max", 4.0, 0.0, 100.0));
        SOUL_SIPHON_LP_PER_HEALTH = new Val<>(b.comment("NeoVitae LP added to your soul network per point of victim max health (0 = off)").defineInRange("soul_siphon_lp_per_health", 10, 0, 10000));
        ARCANE_WARD_BASE = new Val<>(b.comment("Arcane Ward shield size (absorption points) before magic proficiency").defineInRange("arcane_ward_base", 4, 0, 100));
        ARCANE_WARD_MAX = new Val<>(b.defineInRange("arcane_ward_max", 12, 0, 100));
        ARCANE_WARD_OUT_OF_COMBAT = new Val<>(b.comment("Seconds without taking damage before the ward refills").defineInRange("arcane_ward_out_of_combat", 8, 0, 600));
        HEX_CHANCE = new Val<>(b.defineInRange("hex_chance", 0.25, 0.0, 1.0));
        b.pop();

        b.push("industry");
        CROP_RADIUS = new Val<>(b.comment("Crop growth radius for levels 1/2/3").defineList("crop_radius", List.of(5, 7, 9), () -> 5, o -> o instanceof Integer));
        CROP_CHANCE = new Val<>(b.comment("Extra random-tick chance per growable block every 2 seconds, levels 1/2/3")
                .defineList("crop_chance", List.of(0.025, 0.05, 0.1), () -> 0.025, o -> o instanceof Double));
        RANCHER_RADIUS = new Val<>(b.defineInRange("rancher_radius", 8, 1, 32));
        RANCHER_SECONDS = new Val<>(b.comment("Seconds of growth added to nearby babies every 2 seconds").defineInRange("rancher_seconds", 4, 1, 60));
        OVERCLOCK_RADIUS = new Val<>(b.comment("Overclock radius for levels 1/2/3").defineList("overclock_radius", List.of(6, 8, 12), () -> 6, o -> o instanceof Integer));
        OVERCLOCK_EXTRA_TICKS = new Val<>(b.comment("Extra ticks per game tick for levels 1/2/3 (1 = 2x speed)")
                .defineList("overclock_extra_ticks", List.of(1, 2, 4), () -> 1, o -> o instanceof Integer));
        OVERCLOCK_MAX_PER_PLAYER = new Val<>(b.defineInRange("overclock_max_per_player", 48, 1, 1024));
        OVERCLOCK_BLACKLIST_MODS = new Val<>(b.comment("Mod ids whose block entities are never accelerated (Create and its addons are always excluded)")
                .defineListAllowEmpty("overclock_blacklist_mods",
                        List.of("ae2", "refinedstorage", "sophisticatedstorage", "sophisticatedbackpacks", "storagedrawers",
                                "functionalstorage", "toms_storage", "tarbound", "storagehub"),
                        () -> "", o -> o instanceof String));
        MAGNET_RADIUS = new Val<>(b.comment("Magnet radius for levels 1/2").defineList("magnet_radius", List.of(5, 10), () -> 5, o -> o instanceof Integer));
        MAGNET_SPEED = new Val<>(b.defineInRange("magnet_speed", 0.45, 0.05, 2.0));
        HEARTH_RADIUS = new Val<>(b.defineInRange("hearth_radius", 24, 1, 128));
        STOKER_RADIUS = new Val<>(b.defineInRange("stoker_radius", 8, 1, 32));
        STOKER_BONUS = new Val<>(b.comment("Fuel lasts this much longer in furnaces near you").defineInRange("stoker_bonus", 0.5, 0.0, 4.0));
        CAREFUL_HANDS_SAVE = new Val<>(b.comment("Chance per durability point that tools take no wear").defineInRange("careful_hands_save", 0.4, 0.0, 1.0));
        b.pop();

        b.comment("The Hollow (Gearwright's Hollow endgame). Residue tier 1-4 comes from quests (tags gh_residue_<n>), 5 = Hollowed.")
                .push("hollow");
        HOLLOW_ENABLED = new Val<>(b.define("enabled", true));
        HOLLOW_VISION = new Val<>(b.comment("Wasteland Vision vision a Hollowed player is held in").define("vision", "occult_foundry"));
        HOLLOW_VISION_TAG = new Val<>(b.comment("Tag Wasteland Vision gives players who see that vision").define("vision_tag", "wv_occult_foundry"));
        TAKEN_BY_TIER = new Val<>(b.comment("Chance a friendly/neutral spawn near the player is 'taken' (turns hostile), residue tiers 1-5")
                .defineList("taken_by_tier", List.of(0.02, 0.05, 0.08, 0.12, 0.15), () -> 0.0, o -> o instanceof Double));
        TAKEN_PER_DAY = new Val<>(b.comment("Added per in-game day held after the Hollow").defineInRange("taken_per_day_held", 0.03, 0.0, 1.0));
        TAKEN_CAP = new Val<>(b.defineInRange("taken_cap", 0.5, 0.0, 1.0));
        TAKEN_RADIUS = new Val<>(b.defineInRange("taken_radius", 48, 8, 256));
        RESIDUE_ITEM = new Val<>(b.define("residue_item", "gearwrights:foundry_residue"));
        DISTILLED_ITEM = new Val<>(b.define("distilled_item", "gearwrights:distilled_residue"));
        RESIDUE_BLOCKS = new Val<>(b.defineListAllowEmpty("residue_blocks", List.of("minecraft:deepslate", "minecraft:cobbled_deepslate"),
                () -> "", o -> o instanceof String));
        RESIDUE_BLOCK_CHANCE = new Val<>(b.comment("Residue chance per residue block mined, tiers 1-5 (only while seeing the vision)")
                .defineList("residue_block_chance", List.of(0.005, 0.01, 0.02, 0.03, 0.03), () -> 0.0, o -> o instanceof Double));
        RESIDUE_UNDEAD_CHANCE = new Val<>(b.comment("Residue chance per undead killed, tiers 1-5")
                .defineList("residue_undead_chance", List.of(0.02, 0.04, 0.06, 0.08, 0.08), () -> 0.0, o -> o instanceof Double));
        REVENANT = new Val<>(b.comment("Foundry Revenant mob (overworld): 1-2 residue at tier 5").define("revenant", "incision:skeletoncultist"));
        DISTILLED_UNDEAD = new Val<>(b.comment("Released players only").defineInRange("distilled_undead", 0.02, 0.0, 1.0));
        DISTILLED_REVENANT = new Val<>(b.defineInRange("distilled_revenant", 0.25, 0.0, 1.0));
        DISTILLED_BLOCK = new Val<>(b.defineInRange("distilled_block", 0.004, 0.0, 1.0));
        LOCKED_UNIQUE_CHANCE = new Val<>(b.comment("Held players killing a revenant: chance of one of locked_uniques (Hallmarked)")
                .defineInRange("locked_unique_chance", 0.02, 0.0, 1.0));
        LOCKED_UNIQUES = new Val<>(b.defineListAllowEmpty("locked_uniques", List.of("revenants_shroud", "kiln_hook", "hollow_bell"),
                () -> "", o -> o instanceof String));
        HOLLOWED_SET = new Val<>(b.comment("Hallmarked set whose pieces (and the locked uniques) go cold on released players")
                .define("hollowed_set", "hallmarked.set.hollowed"));
        b.pop();

        SPEC = b.build();
    }

    public static int intAt(List<? extends Integer> list, int level) {
        return list.get(Math.max(0, Math.min(level, list.size()) - 1));
    }

    public static double doubleAt(List<? extends Double> list, int level) {
        return list.get(Math.max(0, Math.min(level, list.size()) - 1));
    }
}
