package com.kragnoth.kskillsperks.compat;

import com.kragnoth.kskillsperks.KSkillsPerks;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/**
 * Optional NeoVitae support without a compile dependency. Mirrors what NeoVitae's own Blood Siphon does:
 * NeoVitaeAPI.getInstance().getAnima(uuid).add(AnimaTicket.create(amount), Integer.MAX_VALUE)
 */
public final class NeoVitaeCompat {
    private NeoVitaeCompat() { }

    private static boolean resolved = false;
    private static Method getInstance, getAnima, add, createTicket;

    private static boolean resolve() {
        if (resolved) return getInstance != null;
        resolved = true;
        if (!ModList.get().isLoaded("neovitae")) return false;
        try {
            Class<?> api = Class.forName("com.breakinblocks.neovitae.api.NeoVitaeAPI");
            Class<?> ticket = Class.forName("com.breakinblocks.neovitae.api.soul.AnimaTicket");
            getInstance = api.getMethod("getInstance");
            Object inst = getInstance.invoke(null);
            getAnima = inst.getClass().getMethod("getAnima", java.util.UUID.class);
            createTicket = ticket.getMethod("create", int.class);
            Class<?> anima = Class.forName("com.breakinblocks.neovitae.api.soul.IAnima");
            add = anima.getMethod("add", ticket, int.class);
            return true;
        } catch (Throwable t) {
            KSkillsPerks.LOGGER.warn("NeoVitae found but its API didn't match; Soul Siphon LP disabled", t);
            getInstance = null;
            return false;
        }
    }

    public static void addLP(ServerPlayer player, int amount) {
        if (amount <= 0 || !resolve()) return;
        try {
            Object anima = getAnima.invoke(getInstance.invoke(null), player.getUUID());
            if (anima != null) add.invoke(anima, createTicket.invoke(null, amount), Integer.MAX_VALUE);
        } catch (Throwable t) {
            KSkillsPerks.LOGGER.debug("Soul Siphon LP failed", t);
        }
    }
}
