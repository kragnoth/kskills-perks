package com.kragnoth.kskillsperks.perk;

import com.kragnoth.kskillsperks.KSkillsPerks;
import com.kragnoth.kskillsperks.compat.NeoVitaeCompat;
import com.kragnoth.kskillsperks.config.PerksConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Bulwark, Warpath, Deadeye and Occult perks. */
@EventBusSubscriber(modid = KSkillsPerks.MOD_ID)
public final class CombatEvents {
    private CombatEvents() { }

    private static final String MARK_KEY = "kskills_perks:marked_until";
    private static final String RICOCHET_KEY = "kskills_perks:ricochet";

    /** Game time each player last took damage (Arcane Ward, Ironclad Upkeep). */
    private static final Map<UUID, Long> LAST_HURT = new HashMap<>();
    /** Bloodrush: player -> {stacks, expiresAt}. */
    private static final Map<UUID, long[]> BLOODRUSH = new HashMap<>();
    /** Re-entrancy guard so cleave damage doesn't cleave again. */
    private static boolean cleaving = false;

    // ------------------------------------------------------------------ incoming damage (before armor)
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;
        var source = event.getSource();
        long now = target.level().getGameTime();

        if (target instanceof Player) LAST_HURT.put(target.getUUID(), now);

        // Shield Wall: allies near a blocking Shield Wall player take less damage.
        if (target instanceof Player victim) {
            double r = PerksConfig.SHIELD_WALL_RADIUS.get();
            for (Player guard : target.level().getEntitiesOfClass(Player.class, target.getBoundingBox().inflate(r))) {
                if (guard != victim && guard.isBlocking() && PerkManager.has(guard, Perk.SHIELD_WALL)) {
                    event.setAmount(event.getAmount() * (float) (1.0 - PerksConfig.SHIELD_WALL_REDUCTION.get()));
                    break;
                }
            }
        }

        // Hunter's Mark: marked targets take extra damage from everyone.
        long markedUntil = target.getPersistentData().getLong(MARK_KEY);
        if (markedUntil > now) {
            event.setAmount(event.getAmount() * (float) (1.0 + PerksConfig.MARK_BONUS.get()));
        }

        if (!(source.getEntity() instanceof ServerPlayer attacker) || attacker == target) return;
        boolean melee = source.getDirectEntity() == attacker;
        boolean projectile = source.getDirectEntity() instanceof Projectile;

        // Hunter's Mark: your first hit marks the target.
        if (markedUntil <= now && PerkManager.has(attacker, Perk.HUNTERS_MARK)) {
            int dur = PerksConfig.MARK_DURATION.get() * 20;
            target.getPersistentData().putLong(MARK_KEY, now + dur);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur, 0, false, false));
        }

        // Long Shot: projectile damage grows with distance.
        if (projectile && PerkManager.has(attacker, Perk.LONG_SHOT)) {
            double frac = Math.min(1.0, attacker.distanceTo(target) / PerksConfig.LONG_SHOT_MAX_DISTANCE.get());
            event.setAmount(event.getAmount() * (float) (1.0 + frac * PerksConfig.LONG_SHOT_MAX_BONUS.get()));
        }

        // Executioner: bonus damage on wounded targets, outright kill on nearly dead non-bosses.
        if (PerkManager.has(attacker, Perk.EXECUTIONER)) {
            float frac = target.getHealth() / Math.max(1f, target.getMaxHealth());
            if (frac < PerksConfig.EXECUTE_KILL_BELOW.get() && !target.getType().is(Tags.EntityTypes.BOSSES)) {
                event.setAmount(Math.max(event.getAmount(), target.getMaxHealth() * 10f));
            } else if (frac < PerksConfig.EXECUTE_BONUS_BELOW.get()) {
                event.setAmount(event.getAmount() * 2f);
            }
        }

        // Cleave: melee hits splash to enemies around the target.
        if (melee && !cleaving && PerkManager.has(attacker, Perk.CLEAVE)) {
            float splash = event.getAmount() * PerksConfig.CLEAVE_FRACTION.get().floatValue();
            if (splash > 0) {
                double r = PerksConfig.CLEAVE_RADIUS.get();
                cleaving = true;
                try {
                    for (LivingEntity other : target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(r))) {
                        if (other == target || other == attacker || other instanceof Player) continue;
                        if (other instanceof OwnableEntity own && attacker.getUUID().equals(own.getOwnerUUID())) continue;
                        if (other.isAlliedTo(attacker)) continue;
                        other.hurt(attacker.damageSources().playerAttack(attacker), splash);
                    }
                } finally {
                    cleaving = false;
                }
            }
        }
    }

    // ------------------------------------------------------------------ damage dealt (after armor)
    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || event.getNewDamage() <= 0) return;
        var source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer attacker) || attacker == target) return;

        // Hex: chance to stack Weakness and Wither.
        if (PerkManager.has(attacker, Perk.HEX) && attacker.getRandom().nextDouble() < PerksConfig.HEX_CHANCE.get()) {
            stackEffect(target, MobEffects.WEAKNESS, 100);
            stackEffect(target, MobEffects.WITHER, 100);
        }

        // Ricochet: an arrow hit bounces once to the nearest other enemy.
        if (source.getDirectEntity() instanceof AbstractArrow arrow
                && !arrow.getPersistentData().getBoolean(RICOCHET_KEY)
                && PerkManager.has(attacker, Perk.RICOCHET)) {
            double r = PerksConfig.RICOCHET_RANGE.get();
            LivingEntity next = null;
            double best = Double.MAX_VALUE;
            for (LivingEntity e : target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(r))) {
                if (e == target || e == attacker || e instanceof Player || !e.isAlive()) continue;
                if (e instanceof OwnableEntity own && attacker.getUUID().equals(own.getOwnerUUID())) continue;
                double d = e.distanceToSqr(target);
                if (d < best) { best = d; next = e; }
            }
            if (next != null) {
                Arrow bounce = new Arrow(target.level(), attacker, new ItemStack(Items.ARROW), null);
                bounce.setPos(target.getX(), target.getY(0.6), target.getZ());
                bounce.setBaseDamage(arrow.getBaseDamage());
                bounce.pickup = AbstractArrow.Pickup.DISALLOWED;
                bounce.getPersistentData().putBoolean(RICOCHET_KEY, true);
                double dx = next.getX() - bounce.getX();
                double dy = next.getY(0.5) - bounce.getY();
                double dz = next.getZ() - bounce.getZ();
                bounce.shoot(dx, dy, dz, 2.0f, 0.0f);
                target.level().addFreshEntity(bounce);
            }
        }
    }

    private static void stackEffect(LivingEntity target, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int duration) {
        var cur = target.getEffect(effect);
        int amp = cur == null ? 0 : Math.min(2, cur.getAmplifier() + 1);
        target.addEffect(new MobEffectInstance(effect, duration, amp));
    }

    // ------------------------------------------------------------------ deaths
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dying = event.getEntity();
        if (dying.level().isClientSide()) return;
        long now = dying.level().getGameTime();

        // Last Stand: survive a lethal hit at 1 HP (reusable totem with a cooldown).
        if (dying instanceof ServerPlayer player && PerkManager.has(player, Perk.LAST_STAND)
                && now >= player.getData(ModAttachments.LAST_STAND_READY)) {
            event.setCanceled(true);
            player.setHealth(1.0f);
            player.removeAllEffects();
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 3));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            player.setData(ModAttachments.LAST_STAND_READY, now + PerksConfig.LAST_STAND_COOLDOWN.get() * 20L);
            player.level().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.2f);
            player.displayClientMessage(Component.literal("Last Stand!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
            return;
        }

        if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || killer == dying) return;

        // Bloodrush: each kill adds a stack.
        if (PerkManager.has(killer, Perk.BLOODRUSH)) {
            long[] state = BLOODRUSH.computeIfAbsent(killer.getUUID(), u -> new long[2]);
            state[0] = Math.min(PerksConfig.BLOODRUSH_MAX_STACKS.get(), state[0] + 1);
            state[1] = now + PerksConfig.BLOODRUSH_DURATION.get() * 20L;
            applyBloodrush(killer, (int) state[0]);
        }

        // Soul Siphon: heal on kill, and feed NeoVitae's soul network if installed.
        if (PerkManager.has(killer, Perk.SOUL_SIPHON)) {
            float heal = (float) Math.min(PerksConfig.SOUL_SIPHON_HEAL_MAX.get(),
                    dying.getMaxHealth() * PerksConfig.SOUL_SIPHON_HEAL_FRACTION.get());
            if (heal > 0) killer.heal(heal);
            int lp = (int) (dying.getMaxHealth() * PerksConfig.SOUL_SIPHON_LP_PER_HEALTH.get());
            if (lp > 0) NeoVitaeCompat.addLP(killer, lp);
        }
    }

    private static void applyBloodrush(ServerPlayer player, int stacks) {
        double bonus = stacks * PerksConfig.BLOODRUSH_PER_STACK.get();
        var dmg = player.getAttribute(Attributes.ATTACK_DAMAGE);
        var spd = player.getAttribute(Attributes.ATTACK_SPEED);
        if (dmg != null) dmg.addOrUpdateTransientModifier(new AttributeModifier(PerkEffects.BLOODRUSH_DAMAGE, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        if (spd != null) spd.addOrUpdateTransientModifier(new AttributeModifier(PerkEffects.BLOODRUSH_SPEED, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    static void clearBloodrush(ServerPlayer player) {
        BLOODRUSH.remove(player.getUUID());
        var dmg = player.getAttribute(Attributes.ATTACK_DAMAGE);
        var spd = player.getAttribute(Attributes.ATTACK_SPEED);
        if (dmg != null) dmg.removeModifier(PerkEffects.BLOODRUSH_DAMAGE);
        if (spd != null) spd.removeModifier(PerkEffects.BLOODRUSH_SPEED);
    }

    // ------------------------------------------------------------------ per-player ticking
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();

        long[] rush = BLOODRUSH.get(player.getUUID());
        if (rush != null && now >= rush[1]) clearBloodrush(player);

        if (now % 20 != 0) return;
        long sinceHurt = now - LAST_HURT.getOrDefault(player.getUUID(), 0L);

        // Ironclad Upkeep: full armor set regenerates slowly out of combat.
        if (now % 40 == 0 && PerkManager.has(player, Perk.IRONCLAD_UPKEEP) && sinceHurt > 100
                && player.getHealth() < player.getMaxHealth() && fullArmor(player)) {
            player.heal(1.0f);
        }

        // Arcane Ward: an absorption shield that refills out of combat.
        if (PerkManager.has(player, Perk.ARCANE_WARD)) {
            int cap = wardCap(player);
            var inst = player.getAttribute(Attributes.MAX_ABSORPTION);
            if (inst != null) {
                inst.addOrUpdateTransientModifier(new AttributeModifier(PerkEffects.ARCANE_WARD_CAP, cap, AttributeModifier.Operation.ADD_VALUE));
            }
            if (sinceHurt >= PerksConfig.ARCANE_WARD_OUT_OF_COMBAT.get() * 20L && player.getAbsorptionAmount() < cap) {
                player.setAbsorptionAmount(Math.min(cap, player.getAbsorptionAmount() + 1.0f));
            }
        }
    }

    private static int wardCap(ServerPlayer player) {
        double prof = 0;
        var holder = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE
                .getHolder(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("lodestone", "magic_proficiency"));
        if (holder.isPresent() && player.getAttribute(holder.get()) != null) {
            prof = player.getAttributeValue(holder.get());
        }
        return (int) Math.min(PerksConfig.ARCANE_WARD_MAX.get(), PerksConfig.ARCANE_WARD_BASE.get() + Math.floor(prof));
    }

    private static boolean fullArmor(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (player.getItemBySlot(slot).isEmpty()) return false;
        }
        return true;
    }

    public static void forget(UUID player) {
        LAST_HURT.remove(player);
        BLOODRUSH.remove(player);
    }

    @SuppressWarnings("unused")
    private static AABB box(LivingEntity e, double r) {
        return e.getBoundingBox().inflate(r);
    }
}
