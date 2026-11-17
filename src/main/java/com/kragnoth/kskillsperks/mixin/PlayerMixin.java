package com.kragnoth.kskillsperks.mixin;

import com.kragnoth.kskillsperks.perk.Perk;
import com.kragnoth.kskillsperks.perk.PerkManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shield Wall: axes can't disable your shield. */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "disableShield", at = @At("HEAD"), cancellable = true)
    private void kskills$keepShield(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide() && PerkManager.has(self, Perk.SHIELD_WALL)) ci.cancel();
    }
}
