package com.kragnoth.kskillsperks.perk;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import net.puffish.skillsmod.api.json.JsonObject;

/**
 * Optional condition on a reward: the player must carry every "while_tags" scoreboard tag and none of the "unless_tags".
 * Tags can come from anything: /tag, other mods (Wasteland Vision tags players wv_<vision>), KubeJS, quests.
 * Rewards with a condition are re-checked once a second ({@link ConditionalTicker}).
 */
public record Condition(List<String> whileTags, List<String> unlessTags) {

    public boolean test(Player player) {
        var tags = player.getTags();
        for (String t : whileTags) if (!tags.contains(t)) return false;
        for (String t : unlessTags) if (tags.contains(t)) return false;
        return true;
    }

    /** null when the JSON has neither key. Accepts a string or a list of strings for each. */
    @Nullable
    public static Condition parse(JsonObject root) {
        List<String> w = strings(root, "while_tags"), u = strings(root, "unless_tags");
        return w.isEmpty() && u.isEmpty() ? null : new Condition(List.copyOf(w), List.copyOf(u));
    }

    private static List<String> strings(JsonObject root, String key) {
        List<String> out = new ArrayList<>();
        var el = root.get(key).getSuccess();
        if (el.isEmpty()) return out;
        var s = el.get().getAsString().getSuccess();
        if (s.isPresent()) {
            out.add(s.get());
            return out;
        }
        el.get().getAsArray().getSuccess().ifPresent(arr -> arr.stream()
                .forEach(e -> e.getAsString().getSuccess().ifPresent(out::add)));
        return out;
    }
}
