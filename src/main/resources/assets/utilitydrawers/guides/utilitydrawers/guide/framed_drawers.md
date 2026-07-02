---
navigation:
  parent: index.md
  title: Framed Drawers
  icon: drawer_framer
item_ids:
  - utilitydrawers:framed_drawer_1
  - utilitydrawers:framed_drawer_2
  - utilitydrawers:framed_drawer_3
  - utilitydrawers:framed_drawer_4
  - utilitydrawers:framed_fluid_drawer_1
  - utilitydrawers:framed_fluid_drawer_2
  - utilitydrawers:framed_fluid_drawer_3
  - utilitydrawers:framed_fluid_drawer_4
  - utilitydrawers:framed_compacting_drawer
  - utilitydrawers:drawer_framer
---

# Framed Drawers

Framed Drawers are functionally identical to regular [Drawers](drawers.md), [Fluid Drawers](fluid_drawers.md), and [Compacting Drawers](compacting_drawers.md) — same capacity, same locking, same upgrades. The difference is their appearance: a Framed Drawer can be textured to look like any other block, letting it blend into whatever build style you're using.

## Which drawers can be framed?

Every drawer type has a framed counterpart:

- Item drawers (all four slot counts)
- Fluid drawers (all four slot counts)
- Compacting Drawers

## Applying Textures: The Drawer Framer

New textures can be applied to the framed drawers using the **Drawer Framer**. The framer lets you pick a separate texture for the drawer's **sides** and its **face** (front), so you can match surrounding walls on the sides while keeping a distinct front face, or use the same block for both.

<GameScene zoom={3} interactive={true}>
  <ImportStructure src="framed_drawers.snbt" />
  <IsometricCamera yaw="135" pitch="30" />
</GameScene>

### Slots

The Drawer Framer has four working slots:

- **Sides** — a block item whose texture is applied to the drawer's side faces
- **Face** — a block item whose texture is applied to the drawer's front face
- **Input** — the blank Framed (item, fluid, or compacting) Drawer to be textured
- **Output** — where the finished, textured drawer appears once processing completes

An optional **upgrade slot** accepts a drawer capacity upgrade (see [Upgrades](upgrades.md)) to speed up processing — each tier divides the process time by its multiplier.

### Process

1. Insert a block item into the Sides slot and a block item into the Face slot — these can be the same block, or two different blocks for a mixed look.
2. Insert a blank Framed Drawer, Framed Fluid Drawer, or Framed Compacting Drawer into the Input slot.
3. Make sure the Output slot is empty — the framer won't start a new job while a finished drawer is waiting to be collected.
4. Wait for the progress bar to fill. The base process takes 5 seconds; placing a tier upgrade in the upgrade slot divides that time by the upgrade's multiplier.
5. Collect the finished drawer from the Output slot — it's the same block, now displaying your chosen side and face textures.

The framer consumes one Sides block, one Face block, and the input drawer per completed job. Framed Drawers can be reframed with new blocks.