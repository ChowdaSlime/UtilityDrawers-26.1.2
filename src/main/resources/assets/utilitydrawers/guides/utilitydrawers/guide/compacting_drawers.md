---
navigation:
  parent: index.md
  title: Compacting Drawers
  icon: compacting_drawer
item_ids:
  - utilitydrawers:compacting_drawer
---

# Compacting Drawer

A Compacting Drawer stores an item and its compressed and decompressed crafting forms all in one block, at the same time — for example raw iron nuggets, ingots, and blocks, all sharing one drawer.

## How it detects the chain

Insert any item from a compress/decompress chain into an empty Compacting Drawer. It automatically searches nearby crafting recipes to figure out the rest of the chain:

- If what you inserted can be **decompressed** into something else (like a block into ingots), it looks one step further to find the base form.
- If what you inserted can be **compressed** (like ingots into a block), it looks for that too.

Once detected, the drawer shows three slots — **Base**, **Mid**, and **Block** — matching whatever forms it found. Items automatically convert between forms as needed when you insert or extract from any of the three slots.

## Capacity

Compacting Drawers track everything internally as "raw units" of the base item, so inserting or extracting from the Mid or Block slot converts to/from raw units behind the scenes. The base capacity multiplier can be adjusted by modpack developers in the config file.

## Locking and Upgrades

Compacting Drawers support the same locking and upgrade behavior as regular [Drawers](drawers.md) — sneak right-click with an empty hand to lock, and add <ItemLink id="drawer_upgrade_t1" /> through <ItemLink id="drawer_upgrade_t4" /> or a <ItemLink id="void_upgrade" /> to its upgrade slots. See [Upgrades](upgrades.md) for details.

## Framed variant

Compacting Drawers have a framed variant to match a variety of different aesthetics.
