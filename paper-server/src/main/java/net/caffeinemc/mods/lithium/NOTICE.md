# Lithium-derived code

The classes under `net.caffeinemc.mods.lithium` and the matching hooks in the Minecraft source patches
(sleeping block entities, hopper transfer caching, inventory change tracking, item stack change tracking)
are derived from Lithium (https://github.com/CaffeineMC/lithium, "develop" branch, mixins under
`block/hopper`, `world/block_entity_ticking/sleeping`, `util/inventory_change_listening`,
`util/inventory_comparator_tracking`, `util/entity_movement_tracking`, `util/item_component_and_count_tracking`).

- Original authors: 2No2Name and the Lithium contributors.
- Licence of the originals: GNU LGPL-3.0-only. LGPL-3.0 code may be combined into a GPL-3.0 work (LGPL-3.0 section 4 and GPL-3.0
  section 13); this project (Paper) is GPL-3.0, so the combined work is distributed under GPL-3.0 and the Lithium files keep
  their LGPL notices.
- Adaptation to Paper/Moonrise and Minecraft 26.3 taken from Leaf (https://github.com/Winds-Studio/Leaf, ver/26.3, patches
  "Leaves: Lithium Sleeping Block Entity" and "Lithium: equipment tracking", item stack parts only), which in turn take it from
  LeavesMC (GPL-3.0-only).
- Changes made here: Paper-specific behaviour (cooldown-when-full, hopper amount/transfer settings, ignore-occluding-blocks,
  move/search/prepare events, per-inventory max stack sizes) falls back to or reproduces the Paper code path; a master switch
  (`-Dpaper.sleepingBlockEntity=false`) restores the unmodified Paper paths.
