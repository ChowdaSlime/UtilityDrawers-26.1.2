---
navigation:
  parent: index.md
  title: Fluid Drawers
  icon: fluid_drawer_4
item_ids:
  - utilitydrawers:fluid_drawer_1
  - utilitydrawers:fluid_drawer_2
  - utilitydrawers:fluid_drawer_3
  - utilitydrawers:fluid_drawer_4
---

# Fluid Drawers

Fluid Drawers work exactly like regular [Drawers](drawers.md), but store fluids instead of items. They come in the same four slot-count variants — 1, 2, 3, and 4 slots — with the same tradeoff: fewer slots per drawer means more capacity per slot.

## Filling a fluid drawer

Use a filled fluid container (bucket, bottle, etc.) on an empty slot to set its template fluid, then keep using filled containers on that slot to top it off. Use an empty container on a filled slot to drain some back out.

## Capacity

Each slot's base capacity is measured in mB and scales with the drawer's slot-count multiplier, the same way item drawers scale off max stack size. Modpack developers can adjust the base fluid capacity per drawer in the config file.

## Locking, Upgrades, and Wireless

Fluid drawers support the same locking behavior, upgrade slots, and wireless networking as item drawers — see [Drawers](drawers.md#locking), [Upgrades](upgrades.md), and [Wireless Drawers](wireless_drawers.md) for details.

## Framed variant

Compacting Drawers have a framed variant to match a variety of different aesthetics.