package com.kragnoth.kskillsperks.mixin;

import com.kragnoth.kskillsperks.config.PerksConfig;
import com.kragnoth.kskillsperks.perk.IndustryTicker;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stoker: when a furnace near a Stoker player lights a new piece of fuel, that fuel burns longer.
 * Applied per fuel item (not per second), so it stacks correctly with Overclock.
 * serverTick then copies litTime into litDuration, so the flame gauge stays accurate.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class StokerFurnaceMixin {
    @ModifyExpressionValue(
            method = "serverTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getBurnDuration(Lnet/minecraft/world/item/ItemStack;)I"))
    private static int kskills$stokeFuel(int burnTime,
                                         @Local(argsOnly = true) Level level,
                                         @Local(argsOnly = true) BlockPos pos) {
        if (burnTime > 0 && IndustryTicker.isStoked(level, pos)) {
            return (int) Math.min(Integer.MAX_VALUE, Math.round(burnTime * (1.0 + PerksConfig.STOKER_BONUS.get())));
        }
        return burnTime;
    }
}
