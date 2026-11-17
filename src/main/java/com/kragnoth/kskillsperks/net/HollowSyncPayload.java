package com.kragnoth.kskillsperks.net;

import com.kragnoth.kskillsperks.KSkillsPerks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: the player's Hollow weight (taken chance) and state, for the HUD meter (1.2.0). */
public record HollowSyncPayload(float weight, boolean held, boolean released) implements CustomPacketPayload {
    public static final Type<HollowSyncPayload> TYPE = new Type<>(KSkillsPerks.id("hollow_sync"));
    public static final StreamCodec<ByteBuf, HollowSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, HollowSyncPayload::weight,
            ByteBufCodecs.BOOL, HollowSyncPayload::held,
            ByteBufCodecs.BOOL, HollowSyncPayload::released,
            HollowSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
