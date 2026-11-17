# Changelog

## 1.2.0
- The Hollow moves into the mod (from the pack's KubeJS): per-player Hollow state, `/kskills hollow ...`, residue drops, taken
  spawns, Hollowed gear going cold, a HUD meter. Imports the pack's old scoreboards/tags on first login. `[hollow]` config section.

## 1.1.0
- Rewards can depend on scoreboard tags: `while_tags` / `unless_tags` on `kskills_perks:perk`.
- New reward type `kskills_perks:attribute`: an attribute modifier, optionally tag-conditional.
- Conditional rewards are re-checked once a second; perk effects and client sync fire when a condition flips.

## 1.0.1
- Released build (MIT). Source verified identical to the release jar.
