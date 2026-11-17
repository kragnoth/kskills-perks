package com.kragnoth.kskillsperks.perk;

import java.util.Locale;

/** Every perk a skill node can grant. The id is what goes in the tree JSON: {"perk": "last_stand"}. */
public enum Perk {
    // Bulwark
    LAST_STAND, SHIELD_WALL, IRONCLAD_UPKEEP,
    // Warpath
    BLOODRUSH, CLEAVE, EXECUTIONER,
    // Deadeye
    RICOCHET, HUNTERS_MARK, LONG_SHOT,
    // Pathfinder
    DOUBLE_JUMP, DASH, WALL_CLING,
    // Foreman
    PROSPECTOR, AUTO_SMELT, TREASURE_HUNTER,
    // Occult
    SOUL_SIPHON, ARCANE_WARD, HEX,
    // Industry (levelled perks: CROP_GROWTH 1-3, OVERCLOCK 1-3, MAGNET 1-2, HEARTH 1-2)
    CROP_GROWTH, RANCHER, OVERCLOCK, MAGNET, HEARTH, AUTO_REPLANT, STOKER, CAREFUL_HANDS;

    public final String id = name().toLowerCase(Locale.ROOT);

    public static Perk byId(String id) {
        for (Perk p : values()) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }
}
