---
navigation:
  parent: index.md
  title: Storage Viewer
  icon: storage_viewer
item_ids:
  - utilitydrawers:storage_viewer
---

# Storage Viewer

The Storage Viewer is a block that lets you browse, search, and directly extract or insert items and fluids across an entire linked [Storage Interface](storage_interface.md) network from one screen — without opening individual drawers.

## Placement and auto-linking

The Storage Viewer attaches to the face of another block, similar to an item frame. If you place it directly against a [Storage Interface](storage_interface.md), it automatically detects and links to that interface — no manual linking needed for the most common setup.

## Manual linking

To connect a Storage Viewer to an interface it isn't directly attached to, use a [Storage Remote](storage_remote.md) in Link mode: bind the remote to the interface you want, then right-click the viewer to connect it. Right-clicking again with the same bound remote disconnects it.

## Opening the viewer

Right-click the Storage Viewer to open its screen. It shows every distinct item and fluid currently stored across the whole connected network in a scrollable grid.

## Searching

Type in the search bar to filter the grid. A few special prefixes change what's being searched:

- **`@mod_id`** — show only items from a specific mod (matches by mod namespace)
- **`#tag_name`** — show only items or fluids with a matching tag
- **`$text`** — search inside item tooltips (useful for finding enchanted or named items)
- Plain text searches item/fluid display names

## Sorting

Two buttons next to the grid control ordering:

- Toggle between sorting **by name** or **by count**
- Toggle **ascending/descending**

## Extracting items

With an empty cursor:

- **Left-click** a slot to extract a full stack
- **Right-click** to extract half a stack
- **Shift + left-click** to quick-move a stack into your inventory
- **Shift + right-click** to pull just 1 item onto your cursor

If you're holding a partial stack of the same item shown in a slot, shift + right-click tops it off by 1 from the network.

## Extracting and inserting fluids

Fluids show up in the grid alongside items and work with buckets:

- Left-click a fluid slot with an **empty bucket** to fill it from the network
- Right-click with a **filled bucket** to empty it into the network
- The viewer automatically looks for buckets in your inventory or the network itself rather than requiring you to hold one first, when applicable

## Inserting items

With any non-bucket item on your cursor:

- **Left-click** any slot to insert your whole cursor stack
- **Right-click** to insert just 1

There's no need to match the item to an existing slot — inserting a new item type adds it to the network's display automatically, distributing into whichever connected drawer can hold it.
