package com.kragnoth.kskillsperks.client;

import com.kragnoth.kskillsperks.net.PerkSyncPayload;
import com.kragnoth.kskillsperks.net.ProspectPayload;
import com.kragnoth.kskillsperks.perk.Perk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side copy of synced perk data. Deliberately plain data only (no client classes),
 * so referencing it from common code is safe on a dedicated server.
 */
public final class ClientAccess {
    private ClientAccess() { }

    private static Map<String, Integer> levels = new HashMap<>();
    public static int dashCooldown = 80;
    public static double dashSpeed = 1.4;
    public static double doubleJumpStrength = 0.55;
    public static double wallSlideSpeed = 0.08;

    public static volatile List<Long> prospectPositions = List.of();
    /** Set by the renderer when it sees a new list; outlines last 8 s from then. */
    public static volatile boolean prospectFresh = false;

    public static int level(Perk perk) {
        return levels.getOrDefault(perk.id, 0);
    }

    public static boolean has(Perk perk) {
        return level(perk) > 0;
    }

    public static void onSync(PerkSyncPayload payload) {
        levels = new HashMap<>(payload.levels());
        dashCooldown = payload.dashCooldown();
        dashSpeed = payload.dashSpeed();
        doubleJumpStrength = payload.doubleJumpStrength();
        wallSlideSpeed = payload.wallSlideSpeed();
    }

    public static void onProspect(ProspectPayload payload) {
        prospectPositions = List.copyOf(payload.positions());
        prospectFresh = true;
    }

    public static void reset() {
        levels = new HashMap<>();
        prospectPositions = List.of();
    }
}
