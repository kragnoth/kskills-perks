package com.kragnoth.kskillsperks.client;

import com.kragnoth.kskillsperks.KSkillsPerks;
import com.kragnoth.kskillsperks.net.PerkActionPayload;
import com.kragnoth.kskillsperks.perk.Perk;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@EventBusSubscriber(modid = KSkillsPerks.MOD_ID, value = Dist.CLIENT)
public final class ClientGameEvents {
    private ClientGameEvents() { }

    private static boolean wasJumping = false;
    private static boolean doubleJumpUsed = false;
    private static boolean clinging = false;
    private static int airTicks = 0;
    private static long dashReady = 0;
    private static long prospectUntil = 0;

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientAccess.reset();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        long now = mc.level.getGameTime();

        while (ClientModEvents.MAGNET.consumeClick()) send(PerkActionPayload.MAGNET_TOGGLE);
        while (ClientModEvents.PROSPECT.consumeClick()) send(PerkActionPayload.PROSPECT);
        while (ClientModEvents.DASH.consumeClick()) tryDash(player, now);

        boolean jumping = player.input.jumping;
        boolean jumpPressed = jumping && !wasJumping;
        wasJumping = jumping;

        boolean grounded = player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable()
                || player.getAbilities().flying || player.isFallFlying() || player.isPassenger();
        if (grounded) {
            airTicks = 0;
            doubleJumpUsed = false;
            clinging = false;
            return;
        }
        airTicks++;

        // Wall Cling: hold sneak against a wall to slow your fall; jump to kick off it.
        boolean wasClinging = clinging;
        clinging = ClientAccess.has(Perk.WALL_CLING) && player.horizontalCollision && player.input.shiftKeyDown
                && player.getDeltaMovement().y <= 0.05;
        if (clinging) {
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x * 0.6, Math.max(v.y, -ClientAccess.wallSlideSpeed), v.z * 0.6);
            player.fallDistance = 0;
            if (!wasClinging || player.tickCount % 10 == 0) send(PerkActionPayload.WALL_CLING);
        }
        if ((clinging || wasClinging) && jumpPressed && ClientAccess.has(Perk.WALL_CLING)) {
            Vec3 look = player.getLookAngle();
            Vec3 away = new Vec3(-look.x, 0, -look.z);
            if (away.lengthSqr() > 1.0e-4) away = away.normalize().scale(0.5);
            player.setDeltaMovement(away.x, 0.6, away.z);
            player.fallDistance = 0;
            clinging = false;
            doubleJumpUsed = false; // a wall jump gives your double jump back
            send(PerkActionPayload.WALL_CLING);
            return;
        }

        // Double Jump: one extra jump in mid-air.
        if (jumpPressed && airTicks > 2 && !doubleJumpUsed && ClientAccess.has(Perk.DOUBLE_JUMP)) {
            doubleJumpUsed = true;
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x, ClientAccess.doubleJumpStrength, v.z);
            player.fallDistance = 0;
            send(PerkActionPayload.DOUBLE_JUMP);
        }
    }

    private static void tryDash(LocalPlayer player, long now) {
        if (!ClientAccess.has(Perk.DASH) || now < dashReady) return;
        float fwd = player.input.forwardImpulse, strafe = player.input.leftImpulse;
        double yaw = Math.toRadians(player.getYRot());
        double sin = Math.sin(yaw), cos = Math.cos(yaw);
        double dx, dz;
        if (Math.abs(fwd) < 0.01 && Math.abs(strafe) < 0.01) {
            dx = -sin; dz = cos; // no movement keys held: dash where you're looking
        } else {
            dx = strafe * cos - fwd * sin;
            dz = fwd * cos + strafe * sin;
        }
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0e-4) return;
        double s = ClientAccess.dashSpeed / len;
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(dx * s, Math.max(v.y, 0.15), dz * s);
        player.fallDistance = 0;
        dashReady = now + ClientAccess.dashCooldown;
        send(PerkActionPayload.DASH);
    }

    private static void send(int action) {
        PacketDistributor.sendToServer(new PerkActionPayload(action));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        if (ClientAccess.prospectFresh) {
            ClientAccess.prospectFresh = false;
            prospectUntil = now + 160;
        }
        List<Long> positions = ClientAccess.prospectPositions;
        if (now > prospectUntil || positions.isEmpty()) return;

        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        var buffers = mc.renderBuffers().bufferSource();
        var lines = buffers.getBuffer(PerkRenderTypes.ORE_LINES);
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        float fade = Math.min(1f, (prospectUntil - now) / 40f);
        for (long l : positions) {
            BlockPos p = BlockPos.of(l);
            if (!mc.level.getBlockState(p).is(Tags.Blocks.ORES)) continue; // already mined
            LevelRenderer.renderLineBox(pose, lines, new AABB(p).inflate(0.002), 1.0f, 0.78f, 0.25f, fade);
        }
        pose.popPose();
        buffers.endBatch(PerkRenderTypes.ORE_LINES);
    }
}
