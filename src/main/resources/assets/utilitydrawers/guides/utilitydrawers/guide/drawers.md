---
navigation:
  parent: index.md
  title: Drawers
  icon: oak_drawer_4
item_ids:
  - utilitydrawers:oak_drawer_1
  - utilitydrawers:oak_drawer_2
  - utilitydrawers:oak_drawer_3
  - utilitydrawers:oak_drawer_4
  - utilitydrawers:spruce_drawer_1
  - utilitydrawers:spruce_drawer_2
  - utilitydrawers:spruce_drawer_3
  - utilitydrawers:spruce_drawer_4
  - utilitydrawers:birch_drawer_1
  - utilitydrawers:birch_drawer_2
  - utilitydrawers:birch_drawer_3
  - utilitydrawers:birch_drawer_4
  - utilitydrawers:acacia_drawer_1
  - utilitydrawers:acacia_drawer_2
  - utilitydrawers:acacia_drawer_3
  - utilitydrawers:acacia_drawer_4
  - utilitydrawers:jungle_drawer_1
  - utilitydrawers:jungle_drawer_2
  - utilitydrawers:jungle_drawer_3
  - utilitydrawers:jungle_drawer_4
  - utilitydrawers:dark_oak_drawer_1
  - utilitydrawers:dark_oak_drawer_2
  - utilitydrawers:dark_oak_drawer_3
  - utilitydrawers:dark_oak_drawer_4
  - utilitydrawers:mangrove_drawer_1
  - utilitydrawers:mangrove_drawer_2
  - utilitydrawers:mangrove_drawer_3
  - utilitydrawers:mangrove_drawer_4
  - utilitydrawers:cherry_drawer_1
  - utilitydrawers:cherry_drawer_2
  - utilitydrawers:cherry_drawer_3
  - utilitydrawers:cherry_drawer_4
  - utilitydrawers:pale_oak_drawer_1
  - utilitydrawers:pale_oak_drawer_2
  - utilitydrawers:pale_oak_drawer_3
  - utilitydrawers:pale_oak_drawer_4
  - utilitydrawers:bamboo_drawer_1
  - utilitydrawers:bamboo_drawer_2
  - utilitydrawers:bamboo_drawer_3
  - utilitydrawers:bamboo_drawer_4
  - utilitydrawers:crimson_drawer_1
  - utilitydrawers:crimson_drawer_2
  - utilitydrawers:crimson_drawer_3
  - utilitydrawers:crimson_drawer_4
  - utilitydrawers:warped_drawer_1
  - utilitydrawers:warped_drawer_2
  - utilitydrawers:warped_drawer_3
  - utilitydrawers:warped_drawer_4
---

# Drawers

Drawers are the basic building block of the mod. Each drawer comes in every wood type and in four sizes: 1, 2, 3, or 4 slots. The slot count controls how much capacity each individual slot has — fewer slots means more storage per slot.

## Filling a drawer

Place any item into an empty drawer slot to set it as that slot's template. Once a slot has a template, only matching items can be added to it — right-click with the same item to insert more, or right-click with an empty hand to take some out.

## Capacity

A drawer's capacity per slot depends on its slot count and the item's own max stack size:

- **1-slot drawers** hold the most per slot
- **2-slot drawers** hold less per slot, but have two independent slots
- **3-slot** and **4-slot** drawers trade capacity per slot for more slots

These base multipliers, along with every other capacity value in this guide, can be changed by modpack developers in the mod's config file — the numbers shown here are the defaults.

## Locking

Right-click a drawer with an empty hand while sneaking to lock it. A locked drawer keeps its template item even after it's fully emptied, so hoppers and automation won't cause it to "forget" what it's supposed to hold.

## Upgrades

Drawers have upgrade slots that accept <ItemLink id="drawer_upgrade_t1" /> through <ItemLink id="drawer_upgrade_t4" /> to multiply capacity, and a <ItemLink id="void_upgrade" /> to destroy overflow instead of rejecting it. See the [Upgrades](upgrades.md) page for details.

## Framed variant

Compacting Drawers have a framed variant to match a variety of different aesthetics.

## Related

- [Fluid Drawers](fluid_drawers.md) — the liquid equivalent
- [Framed Drawers](framed_drawers.md) — reskin a drawer to blend into any build
- [Wireless Drawers](wireless_drawers.md) — share storage across placed drawers without a physical link
