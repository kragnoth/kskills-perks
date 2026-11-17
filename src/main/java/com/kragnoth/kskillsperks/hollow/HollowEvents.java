package com.kragnoth.kskillsperks.hollow;

import com.kragnoth.kskillsperks.KSkillsPerks;
import com.kragnoth.kskillsperks.config.PerksConfig;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/** The Hollow's world effects (1.2.0): residue drops, taken spawns, Hollowed gear going cold, state sync. */
@EventBusSubscriber(modid = KSkillsPerks.MOD_ID)
public final class HollowEvents {
    private HollowEvents() { }

    public static final String TAKEN = "gh_hollow_taken";
    private static final Map<UUID, Integer> LAST_WEIGHT = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        HollowCommands.register(e.getDispatcher());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            Hollow.absorb(p);
            Hollow.sync(p);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        LAST_WEIGHT.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !PerksConfig.HOLLOW_ENABLED.get()) return;
        if (p.tickCount % 100 == 0) {
            Hollow.absorb(p);                                     // quests add gh_residue_<n> tags
            int w = (int) Math.round(Hollow.weight(p) * 100);
            Integer last = LAST_WEIGHT.put(p.getUUID(), w);
            if (last == null || last != w) Hollow.sync(p);
        }
        if (p.tickCount % 40 == 0 && Hollow.get(p).released()) coldGear(p);
    }

    // ------------------------------------------------------------------ taken spawns

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.loadedFromDisk() || !(e.getLevel() instanceof ServerLevel level) || !PerksConfig.HOLLOW_ENABLED.get()) return;
        if (!(e.getEntity() instanceof LivingEntity mob) || mob instanceof Player) return;
        if (mob.getType().getCategory() != MobCategory.CREATURE || mob.isBaby() || mob.hasCustomName()) return;
        if (mob instanceof OwnableEntity o && o.getOwnerUUID() != null) return;
        Player p = level.getNearestPlayer(mob, PerksConfig.TAKEN_RADIUS.get());
        if (p != null && level.random.nextDouble() < Hollow.weight(p)) mob.addTag(TAKEN);
    }

    // ------------------------------------------------------------------ residue

    private static ItemStack stack(String id, int n) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Item item = rl == null ? Items.AIR : BuiltInRegistries.ITEM.get(rl);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, n);
    }

    private static double byTier(List<? extends Double> list, int tier) {
        return tier <= 0 || list.isEmpty() ? 0 : list.get(Math.min(tier, list.size()) - 1);
    }

    private static boolean gathers(Player p) {
        return PerksConfig.HOLLOW_ENABLED.get() && !p.isCreative() && Hollow.seesVision(p);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p) || !(e.getLevel() instanceof Level level) || !gathers(p)) return;
        String id = BuiltInRegistries.BLOCK.getKey(e.getState().getBlock()).toString();
        if (!PerksConfig.RESIDUE_BLOCKS.get().contains(id)) return;
        HollowData d = Hollow.get(p);
        if (level.random.nextDouble() < byTier(PerksConfig.RESIDUE_BLOCK_CHANCE.get(), d.tier()))
            Block.popResource(level, e.getPos(), stack(PerksConfig.RESIDUE_ITEM.get(), 1));
        if (d.released() && level.random.nextDouble() < PerksConfig.DISTILLED_BLOCK.get())
            Block.popResource(level, e.getPos(), stack(PerksConfig.DISTILLED_ITEM.get(), 1));
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || !gathers(p)) return;
        LivingEntity mob = e.getEntity();
        var rnd = mob.getRandom();
        HollowData d = Hollow.get(p);
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        boolean revenant = type.equals(PerksConfig.REVENANT.get()) && mob.level().dimension() == Level.OVERWORLD;
        boolean undead = mob.isInvertedHealAndHarm();
        if (revenant && d.tier() >= 5) mob.spawnAtLocation(stack(PerksConfig.RESIDUE_ITEM.get(), 1 + rnd.nextInt(2)));
        else if (undead && rnd.nextDouble() < byTier(PerksConfig.RESIDUE_UNDEAD_CHANCE.get(), d.tier()))
            mob.spawnAtLocation(stack(PerksConfig.RESIDUE_ITEM.get(), 1));
        if (d.released()) {
            double c = revenant ? PerksConfig.DISTILLED_REVENANT.get() : undead ? PerksConfig.DISTILLED_UNDEAD.get() : 0;
            if (rnd.nextDouble() < c) mob.spawnAtLocation(stack(PerksConfig.DISTILLED_ITEM.get(), 1));
        } else if (revenant && d.defeated() && rnd.nextDouble() < PerksConfig.LOCKED_UNIQUE_CHANCE.get()) {
            List<? extends String> u = PerksConfig.LOCKED_UNIQUES.get();
            if (!u.isEmpty())
                Hollow.run(p.server, "hallmarked generate for " + p.getGameProfile().getName() + " 1 unique=" + u.get(rnd.nextInt(u.size())));
        }
    }

    // ------------------------------------------------------------------ Hollowed gear goes cold on released players

    private static boolean hollowedGear(ItemStack st) {
        if (st.isEmpty()) return false;
        var setType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("hallmarked", "set_piece"));
        if (setType != null) {
            Object sp = st.get(setType);
            if (sp != null) {
                try {
                    if (PerksConfig.HOLLOWED_SET.get().equals(sp.getClass().getMethod("set").invoke(sp))) return true;
                } catch (ReflectiveOperationException ignored) { }
            }
        }
        var uniqueType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("hallmarked", "unique"));
        if (uniqueType != null && st.get(uniqueType) instanceof String u) {
            for (String id : PerksConfig.LOCKED_UNIQUES.get()) if (u.equals("hallmarked.unique." + id)) return true;
        }
        return false;
    }

    private static void stow(ServerPlayer p, ItemStack st) {
        if (!p.getInventory().add(st)) p.drop(st, false);
    }

    private static void coldGear(ServerPlayer p) {
        boolean cold = false;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = p.getItemBySlot(slot);
            if (hollowedGear(st)) {
                p.setItemSlot(slot, ItemStack.EMPTY);
                stow(p, st.copy());
                cold = true;
            }
        }
        cold |= coldCurios(p);
        if (hollowedGear(p.getMainHandItem()) || hollowedGear(p.getOffhandItem()))
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, false, true));
        if (cold) p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.kskills_perks.hollow.cold")
                .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE, net.minecraft.ChatFormatting.ITALIC), false);
    }

    private static Method curiosInventory;
    private static boolean curiosChecked;

    /** Curios is optional and not a compile dependency: reached by reflection, and quietly skipped if it isn't there. */
    private static boolean coldCurios(ServerPlayer p) {
        if (!curiosChecked) {
            curiosChecked = true;
            try {
                curiosInventory = Class.forName("top.theillusivec4.curios.api.CuriosApi").getMethod("getCuriosInventory", LivingEntity.class);
            } catch (ReflectiveOperationException ignored) { curiosInventory = null; }
        }
        if (curiosInventory == null) return false;
        boolean cold = false;
        try {
            Optional<?> inv = (Optional<?>) curiosInventory.invoke(null, p);
            if (inv.isEmpty()) return false;
            Object handler = inv.get();
            Map<?, ?> curios = (Map<?, ?>) handler.getClass().getMethod("getCurios").invoke(handler);
            for (Object stacksHandler : curios.values()) {
                Object stacks = stacksHandler.getClass().getMethod("getStacks").invoke(stacksHandler);
                if (!(stacks instanceof IItemHandlerModifiable h)) continue;
                for (int i = 0; i < h.getSlots(); i++) {
                    ItemStack st = h.getStackInSlot(i);
                    if (hollowedGear(st)) {
                        h.setStackInSlot(i, ItemStack.EMPTY);
                        stow(p, st.copy());
                        cold = true;
                    }
                }
            }
        } catch (ReflectiveOperationException | ClassCastException ex) {
            KSkillsPerks.LOGGER.debug("Curios check failed: {}", ex.toString());
        }
        return cold;
    }
}
