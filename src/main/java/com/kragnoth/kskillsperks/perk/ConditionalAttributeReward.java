package com.kragnoth.kskillsperks.perk;

import com.kragnoth.kskillsperks.KSkillsPerks;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
 * Pufferfish's Skills reward type "kskills_perks:attribute" (1.1.0): an attribute modifier that can depend on tags.
 * JSON: {"type": "kskills_perks:attribute", "data": {"attribute": "minecraft:generic.armor", "value": 2,
 *        "operation": "add" | "multiply_base" | "multiply_total", "while_tags": "...", "unless_tags": [...]}}
 * Without tags it behaves like Pufferfish's own attribute reward.
 */
public final class ConditionalAttributeReward implements Reward {
    public static final ResourceLocation ID = KSkillsPerks.id("attribute");
    private static final AtomicInteger NEXT = new AtomicInteger();
    /** player -> rewards currently unlocked for them */
    private static final Map<UUID, Set<ConditionalAttributeReward>> UNLOCKED = new ConcurrentHashMap<>();

    private final Holder<Attribute> attribute;
    private final AttributeModifier modifier;
    private final Condition condition;

    private ConditionalAttributeReward(Holder<Attribute> attribute, double value, AttributeModifier.Operation op, Condition condition) {
        this.attribute = attribute;
        this.modifier = new AttributeModifier(KSkillsPerks.id("conditional_" + NEXT.getAndIncrement()), value, op);
        this.condition = condition;
    }

    public static void register() {
        SkillsAPI.registerReward(ID, ConditionalAttributeReward::parse);
    }

    private static Result<ConditionalAttributeReward, Problem> parse(RewardConfigContext context) {
        return context.getData().andThen(JsonElement::getAsObject).andThen(ConditionalAttributeReward::parse);
    }

    private static Result<ConditionalAttributeReward, Problem> parse(JsonObject root) {
        var id = root.getString("attribute").getSuccess();
        if (id.isEmpty()) return Result.failure(Problem.message("kskills_perks:attribute needs an \"attribute\""));
        var rl = ResourceLocation.tryParse(id.get());
        var holder = rl == null ? null : BuiltInRegistries.ATTRIBUTE.getHolder(rl).orElse(null);
        if (holder == null) return Result.failure(Problem.message("Unknown attribute \"" + id.get() + "\""));
        var value = root.getDouble("value").getSuccess();
        if (value.isEmpty()) return Result.failure(Problem.message("kskills_perks:attribute needs a numeric \"value\""));
        String opName = root.getString("operation").getSuccess().orElse("add").toLowerCase(Locale.ROOT);
        AttributeModifier.Operation op = switch (opName) {
            case "add", "addition", "add_value" -> AttributeModifier.Operation.ADD_VALUE;
            case "multiply_base", "add_multiplied_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "multiply_total", "add_multiplied_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> null;
        };
        if (op == null) return Result.failure(Problem.message("Unknown operation \"" + opName + "\""));
        return Result.success(new ConditionalAttributeReward(holder, value.get(), op, Condition.parse(root)));
    }

    @Override
    public void update(RewardUpdateContext context) {
        ServerPlayer player = context.getPlayer();
        var set = UNLOCKED.computeIfAbsent(player.getUUID(), u -> ConcurrentHashMap.newKeySet());
        if (context.getCount() > 0) set.add(this);
        else set.remove(this);
        ConditionalTicker.watch(player.getUUID());   // also re-adds transient modifiers after a respawn
        apply(player, context.getCount() > 0);
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        for (ServerPlayer player : context.getServer().getPlayerList().getPlayers()) {
            var set = UNLOCKED.get(player.getUUID());
            if (set != null) set.remove(this);
            var inst = player.getAttribute(attribute);
            if (inst != null) inst.removeModifier(modifier.id());
        }
    }

    private void apply(ServerPlayer player, boolean unlocked) {
        var inst = player.getAttribute(attribute);
        if (inst == null) return;
        boolean want = unlocked && (condition == null || condition.test(player));
        boolean has = inst.hasModifier(modifier.id());
        if (want && !has) inst.addTransientModifier(modifier);
        else if (!want && has) inst.removeModifier(modifier.id());
    }

    static void recheck(ServerPlayer player) {
        var set = UNLOCKED.get(player.getUUID());
        if (set == null) return;
        for (var r : set) r.apply(player, true);
    }

    public static void forget(UUID player) {
        UNLOCKED.remove(player);
    }
}
