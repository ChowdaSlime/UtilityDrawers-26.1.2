---
navigation:
  parent: index.md
  title: Storage Viewer
  icon: storage_viewer
item_ids:
  - utilitydrawers:storage_viewer
---

# Storage Viewer

The <ItemLink id="storage_viewer" /> is a block that lets you browse, search, and directly extract or insert items and fluids across an entire linked [Storage Interface](storage_interface.md) network from one screen.

## Placement and auto-linking

The <ItemLink id="storage_viewer" /> attaches to the face of another block, similar to an item frame. If you place it directly against a [Storage Interface](storage_interface.md) (by shift-right clicking), it automatically detects and links to that interface.

## Manual linking

For ease of access, you can connect a <ItemLink id="storage_viewer" /> to an interface, even if it isn't directly attached to one. This is done with a [Storage Remote](storage_remote.md) in Link mode. Simply bind the remote to the interface you want, then right-click the viewer to connect it. Right-clicking again with the same bound remote disconnects it.

## Opening the viewer

Right-click the <ItemLink id="storage_viewer" /> to open its screen. It shows every distinct item and fluid currently stored across the whole connected network in a scrollable grid.

## Sorting

Two buttons next to the grid control ordering:

- Toggle between sorting **by name** or **by count**
- Toggle **ascending/descending**

## Searching

Type in the search bar to filter the grid. Similar to most searching capabilities, you can still filter searches with the following:

- **`@mod_id`** — show only items from a specific mod (matches by mod namespace)
- **`#tag_name`** — show only items or fluids with a matching tag
- **`$tooltip`** — search inside item tooltips (useful for finding enchanted or named items, apothhic items, etc.)
- Plain text searches item/fluid display names


## Extracting items

With an empty cursor:

- **Left-click** a slot to extract a full stack
- **Right-click** to extract half a stack
- **Shift + left-click** to quick-move a stack into your inventory
- **Shift + right-click** to pull just 1 item onto your cursor

If you're holding a partial stack of the same item shown in a slot, shift + right-click tops it off by 1 from the network.

## Extracting and inserting fluids

Fluids show up in the grid alongside items and work with buckets:

- Left-click a fluid slot with an empty hand to get a bucket of the given fluid (assuming there is a bucket available either in the player inventory or the drawer network), or click with an empty bucket to fill it from the network
- Right-click with a **filled bucket** to empty it into the network
- 
## Inserting items

With any non-bucket item on your cursor:

- **Left-click** any slot to insert your whole cursor stack
- **Right-click** to insert just 1