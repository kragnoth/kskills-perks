package com.kragnoth.kskillsperks.hollow;

import com.kragnoth.kskillsperks.config.PerksConfig;
import com.kragnoth.kskillsperks.net.HollowSyncPayload;
import com.kragnoth.kskillsperks.perk.ModAttachments;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Hollow, server side (1.2.0). The mod owns the state ({@link HollowData}) and mirrors it to scoreboard tags that the pack's
 * quests, Mob Rules rules and skill-tree conditions read:
 *   gh_hollow_defeated - has put down their copy (FTB stage task)     gh_escaped - released by an Escapement
 *   gh_residue_1..<tier> - residue tier (quests may still add these tags; the highest one is taken in)
 * Holding / releasing also tells Wasteland Vision (/wastelandvision lock|release).
 */
public final class Hollow {
    private Hollow() { }

    public static final String DEFEATED = "gh_hollow_defeated", ESCAPED = "gh_escaped", TIER_TAG = "gh_residue_";

    public static HollowData get(Player p) {
        return p.getData(ModAttachments.HOLLOW);
    }

    static void put(ServerPlayer p, HollowData d) {
        p.setData(ModAttachments.HOLLOW, d);
        mirror(p, d);
    }

    public static int day(MinecraftServer server) {
        return (int) (server.overworld().getGameTime() / 24000L);
    }

    /** Takes in quest tier tags; on first sight also imports the 9-51/9-52 scoreboards and tags. */
    static void absorb(ServerPlayer p) {
        HollowData d = get(p);
        var tags = p.getTags();
        int tagTier = 0;
        for (int t = 5; t >= 1; t--) if (tags.contains(TIER_TAG + t)) { tagTier = t; break; }
        if (!d.migrated()) {
            d = d.withMigrated(true).withDefeated(d.defeated() || tags.contains(DEFEATED))
                    .withReleased(d.released() || tags.contains(ESCAPED));
            int held = score(p, "gh_held_day"), esc = score(p, "gh_escapements");
            if (d.heldDay() == 0 && held > 0 && !d.released()) d = d.withHeldDay(held);
            if (d.escapements() == 0 && esc > 0) d = d.withEscapements(esc);
        }
        if (tagTier > d.tier()) d = d.withTier(tagTier);
        if (!d.equals(get(p))) put(p, d);
        else mirror(p, d);
    }

    private static int score(ServerPlayer p, String objective) {
        var sb = p.getScoreboard();
        Objective o = sb.getObjective(objective);
        if (o == null) return 0;
        var info = sb.getPlayerScoreInfo(p, o);
        return info == null ? 0 : info.value();
    }

    private static void mirror(ServerPlayer p, HollowData d) {
        tag(p, DEFEATED, d.defeated());
        tag(p, ESCAPED, d.released());
        for (int t = 1; t <= 5; t++) tag(p, TIER_TAG + t, t <= d.tier());
    }

    private static void tag(ServerPlayer p, String tag, boolean on) {
        if (on) p.addTag(tag);
        else p.removeTag(tag);
    }

    // ------------------------------------------------------------------ actions (commands call these)

    public static boolean seesVision(Player p) {
        return p.getTags().contains(PerksConfig.HOLLOW_VISION_TAG.get());
    }

    /** Puts down their copy: tier 5, held. Returns true the first time. */
    public static boolean defeat(ServerPlayer p) {
        HollowData d = get(p);
        boolean first = !d.defeated();
        d = d.withDefeated(true).withTier(5);
        put(p, d);
        if (first && !d.released()) hold(p);
        return first;
    }

    /** Held by the Foundry (again): the held clock starts, Wasteland Vision locks them in. */
    public static void hold(ServerPlayer p) {
        put(p, get(p).withReleased(false).withHeldDay(day(p.server) + 1));
        run(p.server, "wastelandvision lock " + p.getGameProfile().getName() + " " + PerksConfig.HOLLOW_VISION.get());
        sync(p);
    }

    /** Released: the held clock stops, Wasteland Vision lets them go. Returns false if they already were. */
    public static boolean release(ServerPlayer p) {
        HollowData d = get(p);
        if (d.released()) return false;
        put(p, d.withReleased(true).withHeldDay(0));
        run(p.server, "wastelandvision release " + p.getGameProfile().getName());
        sync(p);
        return true;
    }

    public static void raiseTier(ServerPlayer p, int tier) {
        HollowData d = get(p);
        if (tier > d.tier()) put(p, d.withTier(Math.min(5, tier)));
    }

    public static int addEscapements(ServerPlayer p, int n) {
        HollowData d = get(p);
        int e = Math.max(0, d.escapements() + n);
        put(p, d.withEscapements(e));
        return e;
    }

    /** Chance (0..cap) that a friendly/neutral spawn near this player is taken. Released players weigh nothing. */
    public static double weight(Player p) {
        if (!PerksConfig.HOLLOW_ENABLED.get() || !(p instanceof ServerPlayer sp)) return 0;
        HollowData d = get(sp);
        if (d.released() || d.tier() <= 0) return 0;
        List<? extends Double> byTier = PerksConfig.TAKEN_BY_TIER.get();
        double w = byTier.isEmpty() ? 0 : byTier.get(Math.min(d.tier(), byTier.size()) - 1);
        if (d.heldDay() > 0) w += Math.max(0, day(sp.server) + 1 - d.heldDay()) * PerksConfig.TAKEN_PER_DAY.get();
        return Math.min(PerksConfig.TAKEN_CAP.get(), w);
    }

    public static void sync(ServerPlayer p) {
        HollowData d = get(p);
        if (p.connection != null) {
            PacketDistributor.sendToPlayer(p, new HollowSyncPayload((float) weight(p), d.defeated() && !d.released(), d.released()));
        }
    }

    static void run(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }
}
