---
navigation:
  parent: index.md
  title: Storage Remote
  icon: storage_remote
item_ids:
- utilitydrawers:storage_remote
---

# Storage Remote

The <ItemLink id="storage_remote" /> is the tool used to link drawers and Storage Viewers to a [Storage Interface](storage_interface.md), and to lock or unlock drawers.

It has two independent modes: 
* **Link/Unlink** 
* **Lock/Unlock**. 

Shift + scroll while holding the remote to switch between them. The current mode is shown in the item's tooltip.

## Link/Unlink mode

![Link Mode](link.png)

### Binding to a Storage Interface

Shift + right-click a [Storage Interface](storage_interface.md) to bind the remote to it. The bound interface's position is shown in the tooltip. Shift + right-click while pointing at open air to unbind.

### Linking drawers

Once bound, right-click any drawer (item, fluid, compacting, framed, or wireless) to link it to the bound interface, as long as it is within the interface's range.

**Quick Reference**

* **Unlinked drawer** → Links it.
* **Drawer linked to the bound interface** → Unlinks it.
* **Drawer linked to another interface** → Well, can't do that.

### Linking a Storage Viewer

The same right-click toggle works on a [Storage Viewer](storage_viewer.md). This lets you rebind a viewer to a different interface or disconnect it without needing to break and replace the block.

### Single vs. Multi-Select

Shift + left-click the air to toggle between **Single** and **Multi-Select** modes.

**Single** mode links or unlinks one drawer at a time.

**Multi-Select** mode lets you link every drawer inside a given area:

1. Right-click the first corner of the selection.
2. Right-click the opposite corner.
3. Every drawer inside the region that is within range and not already linked elsewhere is linked.

Drawers already linked to the bound interface are left unchanged.

## Locking/Unlocking Mode

In the locking mode, you can either lock individual drawers, or a whole network, depending on which block you lock. On a single drawer, just that drawer will be locked. On a [Storage Interface](storage_interface.md), every connected drawer will change its lock state to match that of the interface.

![Locking Mode](lock.png)

**Quick Reference**

- **Right-click a drawer** → Toggle that drawer's locked state.
- **Right-click a Storage Interface** → Toggle the locked state of every connected drawer.