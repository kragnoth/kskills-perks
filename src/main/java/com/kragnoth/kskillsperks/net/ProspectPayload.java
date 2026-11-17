package com.kragnoth.kskillsperks.net;

import com.kragnoth.kskillsperks.KSkillsPerks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Server -> client: ore positions (BlockPos.asLong) to outline for Prospector's Sense. */
public record ProspectPayload(List<Long> positions) implements CustomPacketPayload {
    public static final Type<ProspectPayload> TYPE = new Type<>(KSkillsPerks.id("prospect"));

    public static final StreamCodec<ByteBuf, ProspectPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()).map(ProspectPayload::new, ProspectPayload::positions);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
