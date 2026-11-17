package com.kragnoth.kskillsperks.perk;

import com.kragnoth.kskillsperks.config.PerksConfig;
import com.kragnoth.kskillsperks.net.PerkActionPayload;
import com.kragnoth.kskillsperks.net.ProspectPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Handles key presses and client-side movement perks reported by clients. */
public final class ServerActions {
    private ServerActions() { }

    private static final Map<UUID, Long> DASH_READY = new HashMap<>();
    private static final Map<UUID, Long> PROSPECT_READY = new HashMap<>();

    public static void handle(ServerPlayer player, int action) {
        long now = player.level().getGameTime();
        switch (action) {
            case PerkActionPayload.MAGNET_TOGGLE -> {
                if (!PerkManager.has(player, Perk.MAGNET)) {
                    player.displayClientMessage(Component.literal("You have not learned Item Magnet yet.").withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                boolean off = !player.getData(ModAttachments.MAGNET_OFF);
                player.setData(ModAttachments.MAGNET_OFF, off);
                player.displayClientMessage(off
                        ? Component.literal("Item Magnet: OFF").withStyle(ChatFormatting.RED)
                        : Component.literal("Item Magnet: ON").withStyle(ChatFormatting.GREEN), true);
            }
            case PerkActionPayload.DASH -> {
                if (!PerkManager.has(player, Perk.DASH)) return;
                // Movement itself is applied on the client (players move client-side); the server grants
                // brief invulnerability and clears fall distance. Allow a little slack for latency.
                long ready = DASH_READY.getOrDefault(player.getUUID(), 0L);
                if (now + 5 < ready) return;
                DASH_READY.put(player.getUUID(), now + PerksConfig.DASH_COOLDOWN.get());
                player.invulnerableTime = Math.max(player.invulnerableTime, 10);
                player.resetFallDistance();
            }
            case PerkActionPayload.DOUBLE_JUMP -> {
                if (PerkManager.has(player, Perk.DOUBLE_JUMP)) player.resetFallDistance();
            }
            case PerkActionPayload.WALL_CLING -> {
                if (PerkManager.has(player, Perk.WALL_CLING)) player.resetFallDistance();
            }
            case PerkActionPayload.PROSPECT -> prospect(player, now);
            default -> { }
        }
    }

    private static void prospect(ServerPlayer player, long now) {
        if (!PerkManager.has(player, Perk.PROSPECTOR)) {
            player.displayClientMessage(Component.literal("You have not learned Prospector's Sense yet.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        long ready = PROSPECT_READY.getOrDefault(player.getUUID(), 0L);
        if (now < ready) {
            long secs = (ready - now + 19) / 20;
            player.displayClientMessage(Component.literal("Prospector's Sense recharging: " + secs + "s").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        PROSPECT_READY.put(player.getUUID(), now + PerksConfig.PROSPECT_COOLDOWN.get() * 20L);
        int r = PerksConfig.PROSPECT_RADIUS.get();
        int max = PerksConfig.PROSPECT_MAX_BLOCKS.get();
        BlockPos c = player.blockPosition();
        var level = player.serverLevel();
        List<Long> found = new ArrayList<>();
        var cursor = new BlockPos.MutableBlockPos();
        outer:
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r * r) continue;
                    cursor.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    if (!level.isLoaded(cursor)) continue;
                    if (level.getBlockState(cursor).is(Tags.Blocks.ORES)) {
                        found.add(cursor.asLong());
                        if (found.size() >= max) break outer;
                    }
                }
            }
        }
        PacketDistributor.sendToPlayer(player, new ProspectPayload(found));
        player.displayClientMessage(Component.literal("Prospector's Sense: " + found.size() + " ore blocks nearby").withStyle(ChatFormatting.GOLD), true);
    }

    public static void forget(UUID player) {
        DASH_READY.remove(player);
        PROSPECT_READY.remove(player);
    }
}
