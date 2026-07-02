---
navigation:
  parent: index.md
  title: Storage Interface
  icon: storage_interface
item_ids:
  - utilitydrawers:storage_interface
---

# Storage Interface

The Storage Interface links a group of nearby drawers together into one network, letting you pipe items or fluids into a single block and have it automatically distribute them across every connected drawer.

## Linking drawers

Linking is done with the [Storage Remote](storage_remote.md): 

![Lock Mode](link.png)

bind a remote to the Storage Interface, then right-click drawers (or use Multi-Select to link a whole region at once) to connect them. See the Storage Remote page for the full workflow, including single-drawer and area linking.

<GameScene zoom={2.0} interactive={true}>
  <ImportStructure src="storage_interface.snbt" />
  <IsometricCamera yaw="135" pitch="30" />

  <BlockAnnotation x="11" y="0" z="7" color="#0080ff">
    Storage Interface (Unlocked)
  </BlockAnnotation>

  <BlockAnnotation x="5" y="0" z="7" color="#0080ff">
    Linked Drawer
  </BlockAnnotation>
  <BlockAnnotation x="6" y="0" z="7" color="#0080ff">
    Linked Drawer
  </BlockAnnotation>
  <BlockAnnotation x="7" y="0" z="7" color="#0080ff">
    Linked Drawer
  </BlockAnnotation>
  <BlockAnnotation x="8" y="0" z="7" color="#0080ff">
    Linked Drawer
  </BlockAnnotation>
  <BlockAnnotation x="9" y="0" z="7" color="#0080ff">
    Linked Drawer
  </BlockAnnotation>

  <BlockAnnotation x="5" y="1" z="7" color="#0080ff">
    Linked Drawer
  </BlockAnnotation>
  <BlockAnnotation x="6" y="1" z="7" color="#0080ff">
    Linked Drawer
  </BlockAnnotation>
  <BlockAnnotation x="7" y="1" z="7" color="#0080ff">
    Linked Drawer
  </BlockAnnotation>
  <BlockAnnotation x="8" y="1" z="7" color="#0080ff">
    Linked Drawer
  </BlockAnnotation>
  <BlockAnnotation x="9" y="1" z="7" color="#0080ff">
    Linked Drawer
  </BlockAnnotation>

  <BlockAnnotation x="6" y="2" z="7" color="#0080ff">
    Linked Drawer
  </BlockAnnotation>
  <BlockAnnotation x="7" y="2" z="7" color="#0080ff">
    Linked Drawer
  </BlockAnnotation>
  <BlockAnnotation x="8" y="2" z="7" color="#0080ff">
    Linked Drawer
  </BlockAnnotation>
  <BlockAnnotation x="9" y="2" z="7" color="#0080ff">
    Linked Drawer
  </BlockAnnotation>
</GameScene>

## Range

The Storage Interface can only link to drawers within its range, which by default is a fixed number of blocks. Placing a capacity upgrade tier (see [Upgrades](upgrades.md)) into the interface's upgrade slot multiplies this range instead of capacity.

## Inserting items and fluids

When something is inserted into the network:

1. It first tries to stack into any connected drawer slot that already holds a matching item or fluid.
2. If nothing matches, it fills the next available empty (and unlocked, or locked-with-matching-template) slot.

## Locking the whole network

The Storage Interface can toggle the locked state of every connected drawer at once — either by right-clicking the interface itself in [Storage Remote](storage_remote.md) Lock mode, or through the interface's own lock toggle. This is handy for keeping automation targeting consistent template slots across your whole storage room.

<GameScene zoom={3.5} interactive={true}>
  <ImportStructure src="locked.snbt" />
  <IsometricCamera yaw="135" pitch="30" />

  <BlockAnnotation x="6" y="0" z="7">
    This interface is currently unlocked.
  </BlockAnnotation>

  <BlockAnnotation x="8" y="0" z="7">
    This interface is currently locked.
  </BlockAnnotation>
</GameScene>

## Viewing network contents

Attach a [Storage Viewer](storage_viewer.md) to browse and search everything stored across the network from one screen.