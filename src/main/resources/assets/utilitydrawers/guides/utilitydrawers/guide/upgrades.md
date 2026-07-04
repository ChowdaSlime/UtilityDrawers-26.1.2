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
---

# Upgrades

Upgrades are placed into, well, upgrade slots. The upgrades are usable in [Drawers](drawers.md), [Storage Interfaces](storage_interface.md), and [Drawer Framer](framed_drawers.md).

## Capacity

<ItemLink id="drawer_upgrade_t1" />, <ItemLink id="drawer_upgrade_t2" />, <ItemLink id="drawer_upgrade_t3" />, and <ItemLink id="drawer_upgrade_t4" /> each multiply a drawer's capacity by a fixed amount, with higher tiers multiplying by more. Multiple capacity upgrades in the same drawer stack multiplicatively.

## Range
In the <ItemLink id="storage_interface" />, upgrades will increase range, up to 512 blocks with a max upgrade, which may be *just a little overkill*. Please be mindful on servers.

## Speed
With the <ItemLink id="drawer_framer" />, upgrades increase the processing time, up to just a fraction of a second with a <ItemLink id="drawer_upgrade_t4" />. Not super useful but can save time if framing *many* drawers.

## Removing upgrades

In [Drawers](drawers.md), tier upgrades can only be removed if doing so wouldn't leave the drawer over-capacity for what it currently holds. If removing an upgrade would shrink the drawers' capacity below the quantity of its current contents, it stays locked in place until room has been made.

The multiplier for each tier can be changed in the config file.

## Related
[Utility Upgrades](utility_upgrades.md)