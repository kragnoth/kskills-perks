package com.kragnoth.kskillsperks.net;

import com.kragnoth.kskillsperks.KSkillsPerks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.HashMap;
import java.util.Map;

/** Server -> client: the player's perk levels plus the movement tuning the client needs to apply them. */
public record PerkSyncPayload(Map<String, Integer> levels, int dashCooldown, double dashSpeed,
                              double doubleJumpStrength, double wallSlideSpeed) implements CustomPacketPayload {

    public static final Type<PerkSyncPayload> TYPE = new Type<>(KSkillsPerks.id("sync"));

    public static final StreamCodec<ByteBuf, PerkSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), PerkSyncPayload::levels,
            ByteBufCodecs.VAR_INT, PerkSyncPayload::dashCooldown,
            ByteBufCodecs.DOUBLE, PerkSyncPayload::dashSpeed,
            ByteBufCodecs.DOUBLE, PerkSyncPayload::doubleJumpStrength,
            ByteBufCodecs.DOUBLE, PerkSyncPayload::wallSlideSpeed,
            PerkSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
