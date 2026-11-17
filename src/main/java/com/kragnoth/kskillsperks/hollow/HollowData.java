package com.kragnoth.kskillsperks.hollow;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A player's Hollow state, saved on the player (survives death and relog).
 * tier: residue tier 0-5 (1-4 from quests, 5 = Hollowed); heldDay: overworld day held + 1 (0 = not held since the Hollow);
 * escapements: Escapements made after release; defeated: has put down their copy; released: let go by an Escapement.
 */
public record HollowData(int tier, int heldDay, int escapements, boolean defeated, boolean released, boolean migrated) {
    public static final HollowData EMPTY = new HollowData(0, 0, 0, false, false, false);
    public static final Codec<HollowData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("tier", 0).forGetter(HollowData::tier),
            Codec.INT.optionalFieldOf("held_day", 0).forGetter(HollowData::heldDay),
            Codec.INT.optionalFieldOf("escapements", 0).forGetter(HollowData::escapements),
            Codec.BOOL.optionalFieldOf("defeated", false).forGetter(HollowData::defeated),
            Codec.BOOL.optionalFieldOf("released", false).forGetter(HollowData::released),
            Codec.BOOL.optionalFieldOf("migrated", false).forGetter(HollowData::migrated)
    ).apply(i, HollowData::new));

    public HollowData withTier(int t) { return new HollowData(t, heldDay, escapements, defeated, released, migrated); }
    public HollowData withHeldDay(int d) { return new HollowData(tier, d, escapements, defeated, released, migrated); }
    public HollowData withEscapements(int e) { return new HollowData(tier, heldDay, e, defeated, released, migrated); }
    public HollowData withDefeated(boolean b) { return new HollowData(tier, heldDay, escapements, b, released, migrated); }
    public HollowData withReleased(boolean b) { return new HollowData(tier, heldDay, escapements, defeated, b, migrated); }
    public HollowData withMigrated(boolean b) { return new HollowData(tier, heldDay, escapements, defeated, released, b); }
}
