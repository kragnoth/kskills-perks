package com.kragnoth.kskillsperks.perk;

import com.kragnoth.kskillsperks.client.ClientAccess;
import com.kragnoth.kskillsperks.config.PerksConfig;
import com.kragnoth.kskillsperks.net.PerkSyncPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which perks each player has, and at what level.
 * Several skill nodes can grant the same perk (e.g. crop growth I, II, III); the highest unlocked level wins.
 */
public final class PerkManager {
    private PerkManager() { }

    /** player -> perk -> (granting reward -> level) */
    private static final Map<UUID, EnumMap<Perk, Map<Object, Integer>>> SOURCES = new ConcurrentHashMap<>();

    /** Perk level for a player (0 = not unlocked). Works on both sides: clients read the synced copy. */
    public static int level(Player player, Perk perk) {
        if (player.level().isClientSide()) {
            return ClientAccess.level(perk);
        }
        var perks = SOURCES.get(player.getUUID());
        if (perks == null) return 0;
        var sources = perks.get(perk);
        if (sources == null || sources.isEmpty()) return 0;
        int max = 0;
        for (int v : sources.values()) max = Math.max(max, v);
        return max;
    }

    public static boolean has(Player player, Perk perk) {
        return level(player, perk) > 0;
    }

    static void set(ServerPlayer player, Perk perk, Object source, int level, boolean notify) {
        var perks = SOURCES.computeIfAbsent(player.getUUID(), u -> new EnumMap<>(Perk.class));
        var sources = perks.computeIfAbsent(perk, p -> new HashMap<>());
        int before = level(player, perk);
        if (level > 0) sources.put(source, level);
        else sources.remove(source);
        int after = level(player, perk);
        if (before != after) {
            PerkEffects.onLevelChanged(player, perk, before, after);
            sync(player);
        }
    }

    static void removeSource(MinecraftServer server, Object source) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            var perks = SOURCES.get(player.getUUID());
            if (perks == null) continue;
            for (Perk perk : Perk.values()) {
                var sources = perks.get(perk);
                if (sources != null && sources.containsKey(source)) {
                    set(player, perk, source, 0, false);
                }
            }
        }
    }

    public static void forget(UUID player) {
        SOURCES.remove(player);
    }

    public static Map<String, Integer> snapshot(ServerPlayer player) {
        Map<String, Integer> out = new HashMap<>();
        for (Perk perk : Perk.values()) {
            int l = level(player, perk);
            if (l > 0) out.put(perk.id, l);
        }
        return out;
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new PerkSyncPayload(snapshot(player),
                PerksConfig.DASH_COOLDOWN.get(), PerksConfig.DASH_SPEED.get(),
                PerksConfig.DOUBLE_JUMP_STRENGTH.get(), PerksConfig.WALL_SLIDE_SPEED.get()));
    }
}
