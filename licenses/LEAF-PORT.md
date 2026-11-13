# Leaf port notice

Branch `lab/leaf-port` adapts optimisations from Leaf (https://github.com/Winds-Studio/Leaf,
branch `ver/26.3`) into this Paper fork. Leaf is itself built on Paper, Purpur, Gale and others.

## Licence finding

- Leaf: "Patches are licensed under MIT, unless indicated differently in their header". Some patch headers
  say GPL-3.0-only (with an additional term under GPLv3 section 7(b), see below), LGPL-3.0-only (Lithium
  derived) or Apache-2.0. Leaf binaries are GPL-3.0-only.
- This fork: GPL-3.0 as a whole (Paper inherits it from Spigot/CraftBukkit), with MIT parts.
- MIT, LGPL-3.0 and GPL-3.0 code may all be adapted into a GPL-3.0 work. Apache-2.0 would also be
  compatible with GPLv3, but nothing under Apache-2.0 is ported so far. No incompatibility was found.
- Obligations kept: every ported change names its original author and the Leaf project in the patch
  message and in a `// Leaf port` source comment. The original copyright notices of MIT and LGPL
  derived code are kept in the per-change patch messages below. For patches carrying the section 7(b)
  term the "Original patch by ..." attribution line is reproduced in the patch message.

## Section 7(b) attribution term (GPL-3.0-only patches by HaHaWTH)

"Any copy or modified version containing a substantial portion of the original contributions in this
patch must preserve the following author attribution in the patch header, source code, or another
prominent legal notice accompanying the corresponding source: Original patch by HaHaWTH
<102713261+HaHaWTH@users.noreply.github.com>" (plus co-authors named in the individual patch).

## Ported changes

Each change is its own feature patch in `paper-server/patches/features/` named `Leaf port: ...`;
the patch message holds the Leaf source reference and the author credits.

### Entity ticking port (branch `lab/entity-port`, patches 0060-0064)

All five are bit identical to vanilla (same results, same order); gameplay changing Leaf options are not ported.

- 0060 read the query box once in `ChunkEntitySlices#getEntities`: Leaf "Optimise getEntities" by Taiyou06,
  co-authored by Martijn Muijsers (MIT).
- 0061 cache `Behavior#hasRequiredMemories`: Leaf "Lithium: Reduce Brain memories lookup" by HaHaWTH, based on
  Lithium by 2No2Name (LGPL-3.0-only). Original patch by HaHaWTH <102713261+HaHaWTH@users.noreply.github.com>.
- 0062 skip the corner walk in `BlockGetter#forEachBlockIntersectedBetween` when the entity stays in its cells:
  a narrower rewrite of Leaf "Sakura: Optimise check inside blocks and traverse blocks" (Samsuik, MIT; Leaf
  additions by HaHaWTH).
- 0063 reuse the collision lists in `Entity#collide`: Leaf "cache collision list" by hayanesuru (MIT).
- 0064 skip the planar speed factor multiplication when the factor is exactly 1: Leaf "Skip negligible planar
  movement multiplication" (Martijn Muijsers, Gale, GPL-3.0-only). Leaf's 1e-6 tolerance variant is not bit
  identical and is not ported.
