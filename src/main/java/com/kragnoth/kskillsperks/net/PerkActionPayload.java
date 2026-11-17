package com.kragnoth.kskillsperks.net;

import com.kragnoth.kskillsperks.KSkillsPerks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: a perk key was pressed, or a client-side movement perk fired. */
public record PerkActionPayload(int action) implements CustomPacketPayload {
    public static final int MAGNET_TOGGLE = 0;
    public static final int DASH = 1;
    public static final int DOUBLE_JUMP = 2;
    public static final int WALL_CLING = 3;
    public static final int PROSPECT = 4;

    public static final Type<PerkActionPayload> TYPE = new Type<>(KSkillsPerks.id("action"));

    public static final StreamCodec<ByteBuf, PerkActionPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(PerkActionPayload::new, PerkActionPayload::action);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
