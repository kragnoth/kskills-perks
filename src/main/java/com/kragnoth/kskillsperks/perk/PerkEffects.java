package com.kragnoth.kskillsperks.perk;

import com.kragnoth.kskillsperks.KSkillsPerks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Cleans up lasting effects when a perk is lost (respec, node locked). */
public final class PerkEffects {
    private PerkEffects() { }

    public static final ResourceLocation BLOODRUSH_DAMAGE = KSkillsPerks.id("bloodrush_damage");
    public static final ResourceLocation BLOODRUSH_SPEED = KSkillsPerks.id("bloodrush_speed");
    public static final ResourceLocation ARCANE_WARD_CAP = KSkillsPerks.id("arcane_ward_cap");

    static void onLevelChanged(ServerPlayer player, Perk perk, int before, int after) {
        if (after > 0) return;
        switch (perk) {
            case BLOODRUSH -> CombatEvents.clearBloodrush(player);
            case ARCANE_WARD -> {
                var inst = player.getAttribute(Attributes.MAX_ABSORPTION);
                if (inst != null) inst.removeModifier(ARCANE_WARD_CAP);
                if (player.getAbsorptionAmount() > player.getMaxAbsorption()) {
                    player.setAbsorptionAmount(player.getMaxAbsorption());
                }
            }
            default -> { }
        }
    }
}
