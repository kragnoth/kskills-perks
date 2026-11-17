package com.kragnoth.kskillsperks.perk;

import net.minecraft.world.level.Level;

import net.minecraft.resources.ResourceKey;

import com.kragnoth.kskillsperks.KSkillsPerks;
import com.kragnoth.kskillsperks.config.PerksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Crop growth, Rancher, Overclock, Magnet, Hearth, Stoker. */
@EventBusSubscriber(modid = KSkillsPerks.MOD_ID)
public final class IndustryTicker {
    private IndustryTicker() { }

    public static final TagKey<Block> GROWABLE = TagKey.create(Registries.BLOCK, KSkillsPerks.id("growable"));
    public static final TagKey<Block> NO_OVERCLOCK = TagKey.create(Registries.BLOCK, KSkillsPerks.id("no_overclock"));

    /** Create's machines (and addons on its framework) all extend SmartBlockEntity; extra ticks break kinetics. */
    private static Class<?> createSmartBE = null;
    private static boolean createResolved = false;
    private static final Map<Block, Boolean> OVERCLOCK_ALLOWED = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int t = server.getTickCount();
        Set<Long> boosted = new HashSet<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator()) continue;
            if (t % 40 == 0) {
                int crop = PerkManager.level(p, Perk.CROP_GROWTH);
                if (crop > 0) growCrops(p, crop);
                if (PerkManager.has(p, Perk.RANCHER)) ranch(p);
                int hearth = PerkManager.level(p, Perk.HEARTH);
                if (hearth > 0) hearth(p, hearth);
            }
            if (t % 2 == 0) {
                int oc = PerkManager.level(p, Perk.OVERCLOCK);
                if (oc > 0) overclock(p, oc, boosted);
                int mag = PerkManager.level(p, Perk.MAGNET);
                if (mag > 0) magnet(p, mag);
            }
            if (t % 20 == 0 && PerkManager.has(p, Perk.STOKER)) stoke(p);
        }
    }

    // ------------------------------------------------------------------ crops
    private static void growCrops(ServerPlayer p, int level) {
        ServerLevel world = p.serverLevel();
        int r = PerksConfig.intAt(PerksConfig.CROP_RADIUS.get(), level);
        double chance = PerksConfig.doubleAt(PerksConfig.CROP_CHANCE.get(), level);
        BlockPos base = p.blockPosition();
        var cursor = new BlockPos.MutableBlockPos();
        var rnd = world.getRandom();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    if (rnd.nextDouble() >= chance) continue;
                    cursor.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    BlockState state = world.getBlockState(cursor);
                    if (!state.isRandomlyTicking() || !state.is(GROWABLE)) continue;
                    BlockPos pos = cursor.immutable();
                    state.randomTick(world, pos, rnd);
                    world.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.25, 0.25, 0.25, 0);
                }
            }
        }
    }

    // ------------------------------------------------------------------ rancher
    private static void ranch(ServerPlayer p) {
        int r = PerksConfig.RANCHER_RADIUS.get();
        for (AgeableMob mob : p.level().getEntitiesOfClass(AgeableMob.class, p.getBoundingBox().inflate(r))) {
            if (mob.isBaby()) mob.ageUp(PerksConfig.RANCHER_SECONDS.get());
        }
    }

    // ------------------------------------------------------------------ hearth
    private static void hearth(ServerPlayer p, int level) {
        BlockPos home = p.getRespawnPosition();
        if (home == null || !p.getRespawnDimension().equals(p.level().dimension())) return;
        int r = PerksConfig.HEARTH_RADIUS.get();
        if (home.distToCenterSqr(p.position()) > (double) r * r) return;
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false));
        if (level >= 2) p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, 0, true, false));
    }

    // ------------------------------------------------------------------ magnet
    private static void magnet(ServerPlayer p, int level) {
        if (p.getData(ModAttachments.MAGNET_OFF)) return;
        if (p.isShiftKeyDown()) return;
        int r = PerksConfig.intAt(PerksConfig.MAGNET_RADIUS.get(), level);
        double speed = PerksConfig.MAGNET_SPEED.get();
        int moved = 0;
        for (Entity e : p.level().getEntities(p, p.getBoundingBox().inflate(r),
                en -> en instanceof ItemEntity || en instanceof ExperienceOrb)) {
            if (e.isRemoved()) continue;
            // Immersive Engineering conveyors (and other machines) mark items that must not be pulled.
            if (e.getPersistentData().contains("PreventRemoteMovement")) continue;
            if (e instanceof ItemEntity item) {
                if (item.hasPickUpDelay()) continue;
                Entity owner = item.getOwner();
                if (owner == p && item.getAge() < 100) continue; // don't instantly reclaim what you just threw
            }
            double dx = p.getX() - e.getX(), dy = p.getY() + 0.5 - e.getY(), dz = p.getZ() - e.getZ();
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d <= 1.5) {
                e.playerTouch(p); // vanilla pickup on the server: no client/server tug of war near the player
                continue;
            }
            if (moved++ >= 64) continue;
            double s = speed / d;
            e.setDeltaMovement(dx * s, dy * s, dz * s);
            e.hurtMarked = true; // send the new velocity to clients this tick instead of on the 20-tick item sync
        }
    }

    // ------------------------------------------------------------------ overclock
    @SuppressWarnings("unchecked")
    private static void overclock(ServerPlayer p, int level, Set<Long> boosted) {
        ServerLevel world = p.serverLevel();
        int r = PerksConfig.intAt(PerksConfig.OVERCLOCK_RADIUS.get(), level);
        int extra = PerksConfig.intAt(PerksConfig.OVERCLOCK_EXTRA_TICKS.get(), level) * 2; // runs every 2 ticks
        int max = PerksConfig.OVERCLOCK_MAX_PER_PLAYER.get();
        double r2 = (double) r * r;
        int count = 0;
        for (BlockEntity be : nearbyBlockEntities(world, p, r)) {
            if (count >= max) return;
            if (be.isRemoved() || isCreate(be)) continue;
            BlockPos pos = be.getBlockPos();
            if (pos.distToCenterSqr(p.position()) > r2) continue;
            if (!boosted.add(pos.asLong())) continue; // another player already boosted it this pass
            BlockState state = be.getBlockState();
            if (!overclockAllowed(state)) continue;
            BlockEntityTicker<BlockEntity> ticker = state.getTicker(world, (BlockEntityType<BlockEntity>) be.getType());
            if (ticker == null) continue;
            count++;
            for (int k = 0; k < extra && !be.isRemoved(); k++) {
                ticker.tick(world, pos, state, be);
            }
            if (world.getRandom().nextFloat() < 0.05f) {
                world.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0.05);
            }
        }
    }

    private static boolean overclockAllowed(BlockState state) {
        return OVERCLOCK_ALLOWED.computeIfAbsent(state.getBlock(), b -> {
            String ns = BuiltInRegistries.BLOCK.getKey(b).getNamespace();
            if (PerksConfig.OVERCLOCK_BLACKLIST_MODS.get().contains(ns)) return false;
            return true;
        }) && !state.is(NO_OVERCLOCK);
    }

    private static boolean isCreate(BlockEntity be) {
        if (!createResolved) {
            createResolved = true;
            if (ModList.get().isLoaded("create")) {
                try {
                    createSmartBE = Class.forName("com.simibubi.create.foundation.blockEntity.SmartBlockEntity");
                } catch (ClassNotFoundException e) {
                    KSkillsPerks.LOGGER.warn("Create is loaded but SmartBlockEntity was not found; Create machines are NOT excluded from Overclock");
                }
            }
        }
        return createSmartBE != null && createSmartBE.isInstance(be);
    }

    // ------------------------------------------------------------------ stoker
    /** Furnaces near a Stoker player, per dimension: packed BlockPos -> game tick the mark expires. */
    private static final Map<ResourceKey<Level>, Map<Long, Long>> STOKED = new HashMap<>();

    /** Marks furnaces near the player; StokerFurnaceMixin lengthens each new fuel item they light while marked. */
    private static void stoke(ServerPlayer p) {
        ServerLevel world = p.serverLevel();
        int r = PerksConfig.STOKER_RADIUS.get();
        double r2 = (double) r * r;
        long expires = world.getGameTime() + 40; // refreshed every 20 ticks while the player stays nearby
        Map<Long, Long> marks = STOKED.computeIfAbsent(world.dimension(), k -> new HashMap<>());
        marks.values().removeIf(exp -> exp < world.getGameTime());
        for (BlockEntity be : nearbyBlockEntities(world, p, r)) {
            if (!(be instanceof AbstractFurnaceBlockEntity) || be.isRemoved()) continue;
            if (be.getBlockPos().distToCenterSqr(p.position()) > r2) continue;
            marks.put(be.getBlockPos().asLong(), expires);
        }
    }

    public static boolean isStoked(Level level, BlockPos pos) {
        if (level == null || level.isClientSide) return false;
        Map<Long, Long> marks = STOKED.get(level.dimension());
        if (marks == null) return false;
        Long exp = marks.get(pos.asLong());
        return exp != null && exp >= level.getGameTime();
    }

    public static void clearStoked() {
        STOKED.clear();
    }

    // ------------------------------------------------------------------ helpers
    private static List<BlockEntity> nearbyBlockEntities(ServerLevel world, ServerPlayer p, int r) {
        List<BlockEntity> out = new ArrayList<>();
        int minCx = (int) Math.floor(p.getX() - r) >> 4, maxCx = (int) Math.floor(p.getX() + r) >> 4;
        int minCz = (int) Math.floor(p.getZ() - r) >> 4, maxCz = (int) Math.floor(p.getZ() + r) >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                LevelChunk chunk = world.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                // copy: ticking a machine may add or remove block entities
                out.addAll(chunk.getBlockEntities().values());
            }
        }
        return out;
    }
}
