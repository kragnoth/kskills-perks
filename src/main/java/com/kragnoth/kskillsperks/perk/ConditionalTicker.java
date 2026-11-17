package com.kragnoth.kskillsperks.perk;

import com.kragnoth.kskillsperks.KSkillsPerks;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Once a second, re-checks conditional perks and attributes for players that have any (1.1.0). */
@EventBusSubscriber(modid = KSkillsPerks.MOD_ID)
public final class ConditionalTicker {
    private ConditionalTicker() { }

    private static final Set<UUID> WATCHED = ConcurrentHashMap.newKeySet();

    static void watch(UUID player) {
        WATCHED.add(player);
    }

    static void forget(UUID player) {
        WATCHED.remove(player);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0 || !WATCHED.contains(player.getUUID())) return;
        PerkManager.recheck(player);
        ConditionalAttributeReward.recheck(player);
    }
}
