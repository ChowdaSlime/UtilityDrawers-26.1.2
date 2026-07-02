---
navigation:
  parent: index.md
  title: Upgrades
  icon: drawer_upgrade_t4
item_ids:
  - utilitydrawers:drawer_upgrade_t1
  - utilitydrawers:drawer_upgrade_t2
  - utilitydrawers:drawer_upgrade_t3
  - utilitydrawers:drawer_upgrade_t4
  - utilitydrawers:void_upgrade
---

# Upgrades

Upgrades are placed into a drawer's (or [Storage Interface's](storage_interface.md)) upgrade slots to change how it behaves. Every drawer type has up to four upgrade slots.

## Capacity Upgrades

<ItemLink id="drawer_upgrade_t1" />, <ItemLink id="drawer_upgrade_t2" />, <ItemLink id="drawer_upgrade_t3" />, and <ItemLink id="drawer_upgrade_t4" /> each multiply a drawer's capacity by a fixed amount, with higher tiers multiplying by more. Multiple capacity upgrades in the same drawer stack multiplicatively.

These same upgrades, when placed in a [Storage Interface](storage_interface.md), instead multiply its wireless linking range rather than storage capacity.

## Void Upgrade

The <ItemLink id="void_upgrade" /> destroys any overflow instead of rejecting it once a drawer is full. Useful for automatically discarding excess items or fluids from an automated system instead of having it back up.

## Removing upgrades

An upgrade can only be removed if doing so wouldn't leave the drawer over-capacity for what it currently holds — if removing a capacity upgrade would shrink the drawer below its current contents, it stays locked in place until you make room.

Exact multiplier values for each tier are set by modpack developers in the config file; check with your pack if the numbers you see in-game don't match what's described here.
