package com.kragnoth.kskillsperks.net;

import com.kragnoth.kskillsperks.client.ClientAccess;
import com.kragnoth.kskillsperks.perk.ServerActions;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class NetworkHandler {
    private NetworkHandler() { }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        // Client-bound handlers only call into ClientAccess when actually run on a client,
        // so a dedicated server never loads client classes.
        registrar.playToClient(PerkSyncPayload.TYPE, PerkSyncPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientAccess.onSync(payload)));
        registrar.playToClient(ProspectPayload.TYPE, ProspectPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientAccess.onProspect(payload)));
        registrar.playToClient(HollowSyncPayload.TYPE, HollowSyncPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientAccess.onHollow(payload)));
        registrar.playToServer(PerkActionPayload.TYPE, PerkActionPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) ServerActions.handle(sp, payload.action());
                }));
    }
}
