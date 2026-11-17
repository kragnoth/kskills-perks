package com.kragnoth.kskillsperks.perk;

import com.kragnoth.kskillsperks.KSkillsPerks;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = KSkillsPerks.MOD_ID)
public final class LifecycleEvents {
    private LifecycleEvents() { }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // Pufferfish's Skills re-applies rewards on join; this makes sure the client gets config values too.
        if (event.getEntity() instanceof ServerPlayer sp) PerkManager.sync(sp);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) PerkManager.sync(sp);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        var id = event.getEntity().getUUID();
        PerkManager.forget(id);
        CombatEvents.forget(id);
        ServerActions.forget(id);
    }
}
