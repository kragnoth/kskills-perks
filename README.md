# Kragnoth's Skill Perks (kskills_perks)

Companion mod for the Gearwright's Hollow skill tree (Pufferfish's Skills 0.19+, NeoForge 1.21.1). MIT.

## Reward types

| Type | Data |
|---|---|
| `kskills_perks:perk` | `{"perk": "hex", "level": 1}` - a keystone perk (see `Perk.java`) |
| `kskills_perks:attribute` | `{"attribute": "minecraft:generic.armor", "value": 2, "operation": "add"}` (`add`, `multiply_base`, `multiply_total`) |

Both accept `"while_tags"` and `"unless_tags"` (a string or a list): the reward only works while the player carries every
while-tag and none of the unless-tags. Re-checked once a second. Tags can come from `/tag`, quests, KubeJS or other mods
(Wasteland Vision 0.24+ tags players `wv_<vision>` while a vision is on).

```json
{"type": "kskills_perks:perk", "data": {"perk": "soul_siphon", "while_tags": "wv_occult_foundry", "unless_tags": "gh_escaped"}}
```

## The Hollow (1.2.0)
Gearwright's Hollow's endgame state lives here, saved on the player (survives death): residue tier, held / released, days held,
Escapements made. It is mirrored to tags the pack reads: `gh_hollow_defeated`, `gh_escaped`, `gh_residue_1..n` (quests may still
add `gh_residue_<n>` tags; the highest is taken in). Holding / releasing also runs `/wastelandvision lock|release`.

- `/kskills hollow status|defeat|hold|release|tier|escapements|reset <player>` (op 2; each returns a number for scripts).
- While a player sees the Foundry (`vision_tag`): Foundry Residue from deepslate and undead by tier, Foundry Revenants at tier 5,
  Distilled Residue for released players, the locked-only Hallmarked uniques for held players.
- Friendly/neutral spawns near a player may come up *taken* (tag `gh_hollow_taken`, which a Mob Rules rule turns hostile):
  chance by tier + per day held, capped.
- Hollowed Hallmarked gear goes cold on released players (armor and Curios slots emptied into the inventory; held weapons weaken).
- A small Hollow meter left of the hotbar while the chance is above 0.
- Everything is in the `[hollow]` section of `kskills_perks-common.toml`.

## Building
Put `puffish_skills-0.19.2-1.21-neoforge.jar` in `libs/`, then `./gradlew build`. Bump `mod_version` in `gradle.properties`
for every change.
