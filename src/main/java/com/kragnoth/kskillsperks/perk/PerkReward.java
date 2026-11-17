package com.kragnoth.kskillsperks.perk;

import com.kragnoth.kskillsperks.KSkillsPerks;
import net.minecraft.resources.ResourceLocation;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

/**
 * Pufferfish's Skills reward type "kskills_perks:perk".
 * JSON: {"type": "kskills_perks:perk", "data": {"perk": "crop_growth", "level": 2}}   ("level" defaults to 1)
 */
public final class PerkReward implements Reward {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(KSkillsPerks.MOD_ID, "perk");

    private final Perk perk;
    private final int level;

    private PerkReward(Perk perk, int level) {
        this.perk = perk;
        this.level = level;
    }

    public static void register() {
        SkillsAPI.registerReward(ID, PerkReward::parse);
    }

    private static Result<PerkReward, Problem> parse(RewardConfigContext context) {
        return context.getData()
                .andThen(JsonElement::getAsObject)
                .andThen(PerkReward::parse);
    }

    private static Result<PerkReward, Problem> parse(JsonObject root) {
        var perkId = root.getString("perk");
        if (perkId.getSuccess().isEmpty()) {
            return Result.failure(Problem.message("kskills_perks:perk reward needs a \"perk\" string"));
        }
        var perk = Perk.byId(perkId.getSuccess().get());
        if (perk == null) {
            return Result.failure(Problem.message("Unknown perk \"" + perkId.getSuccess().get() + "\""));
        }
        int level = root.get("level").getSuccess()
                .flatMap(e -> root.getInt("level").getSuccess())
                .orElse(1);
        return Result.success(new PerkReward(perk, Math.max(1, level)));
    }

    @Override
    public void update(RewardUpdateContext context) {
        PerkManager.set(context.getPlayer(), perk, this, context.getCount() > 0 ? level : 0, context.isAction());
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        PerkManager.removeSource(context.getServer(), this);
    }
}
