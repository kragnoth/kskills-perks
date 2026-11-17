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

## Building
Put `puffish_skills-0.19.2-1.21-neoforge.jar` in `libs/`, then `./gradlew build`. Bump `mod_version` in `gradle.properties`
for every change.
