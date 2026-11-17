package com.kragnoth.kskillsperks.mixin;

import com.kragnoth.kskillsperks.config.PerksConfig;
import com.kragnoth.kskillsperks.perk.Perk;
import com.kragnoth.kskillsperks.perk.PerkManager;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Ironclad Upkeep (armor) and Careful Hands (tools): chance per durability point to take no wear. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @ModifyVariable(
            method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int kskills$reduceWear(int amount, @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) LivingEntity user) {
        if (amount <= 0 || !(user instanceof Player player)) return amount;
        ItemStack self = (ItemStack) (Object) this;
        double save = 0;
        if (self.getItem() instanceof ArmorItem) {
            if (PerkManager.has(player, Perk.IRONCLAD_UPKEEP)) save = PerksConfig.IRONCLAD_DURABILITY_SAVE.get();
        } else if (PerkManager.has(player, Perk.CAREFUL_HANDS)) {
            save = PerksConfig.CAREFUL_HANDS_SAVE.get();
        }
        if (save <= 0) return amount;
        int out = amount;
        for (int i = 0; i < amount; i++) {
            if (level.getRandom().nextDouble() < save) out--;
        }
        return out;
    }
}
