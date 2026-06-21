package net.drawers.utilitydrawers.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import net.drawers.utilitydrawers.block.entity.DrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.FluidDrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.StorageInterfaceBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

public class StorageInterfaceMEStorage implements MEStorage {

    private final StorageInterfaceBlockEntity interfaceEntity;

    public StorageInterfaceMEStorage(StorageInterfaceBlockEntity interfaceEntity) {
        this.interfaceEntity = interfaceEntity;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        Level level = interfaceEntity.getLevel();
        if (level == null) return 0;

        long inserted = 0;

        if (what instanceof AEItemKey itemKey) {
            for (var pos : interfaceEntity.getConnectedDrawers()) {
                if (level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer) {
                    for (int i = 0; i < drawer.getSlotCount(); i++) {
                        var stored = drawer.getStoredItem(i);
                        if (stored.isEmpty() || !itemKey.matches(stored)) continue;
                        var toInsert = itemKey.toStack((int) Math.min(amount - inserted, Integer.MAX_VALUE));
                        var remainder = drawer.insertItemIntoSlot(i, toInsert, mode == Actionable.SIMULATE);
                        inserted += toInsert.getCount() - remainder.getCount();
                        if (inserted >= amount) return inserted;
                    }
                }
            }
            for (var pos : interfaceEntity.getConnectedDrawers()) {
                if (level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer) {
                    for (int i = 0; i < drawer.getSlotCount(); i++) {
                        if (!drawer.isSlotEmpty(i)) continue;
                        if (drawer.isLocked() && !drawer.hasTemplate(i)) continue;
                        var toInsert = itemKey.toStack((int) Math.min(amount - inserted, Integer.MAX_VALUE));
                        var remainder = drawer.insertItemIntoSlot(i, toInsert, mode == Actionable.SIMULATE);
                        inserted += toInsert.getCount() - remainder.getCount();
                        if (inserted >= amount) return inserted;
                    }
                }
            }
        } else if (what instanceof AEFluidKey fluidKey) {
            for (var pos : interfaceEntity.getConnectedDrawers()) {
                if (level.getBlockEntity(pos) instanceof FluidDrawerBlockEntity fluidDrawer) {
                    for (int i = 0; i < fluidDrawer.getSlotCount(); i++) {
                        FluidStack stored = fluidDrawer.getStoredFluid(i);
                        if (!stored.isEmpty() && !fluidKey.matches(stored)) continue;
                        if (stored.isEmpty()) continue;
                        FluidStack toInsert = fluidKey.toStack((int) Math.min(amount - inserted, Integer.MAX_VALUE));
                        FluidStack remainder = fluidDrawer.insertFluidIntoSlot(i, toInsert, mode == Actionable.SIMULATE);
                        inserted += toInsert.getAmount() - remainder.getAmount();
                        if (inserted >= amount) return inserted;
                    }
                }
            }
            for (var pos : interfaceEntity.getConnectedDrawers()) {
                if (level.getBlockEntity(pos) instanceof FluidDrawerBlockEntity fluidDrawer) {
                    for (int i = 0; i < fluidDrawer.getSlotCount(); i++) {
                        if (!fluidDrawer.isSlotEmpty(i)) continue;
                        if (fluidDrawer.isLocked() && !fluidDrawer.hasTemplate(i)) continue;
                        FluidStack toInsert = fluidKey.toStack((int) Math.min(amount - inserted, Integer.MAX_VALUE));
                        FluidStack remainder = fluidDrawer.insertFluidIntoSlot(i, toInsert, mode == Actionable.SIMULATE);
                        inserted += toInsert.getAmount() - remainder.getAmount();
                        if (inserted >= amount) return inserted;
                    }
                }
            }
        }

        return inserted;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        Level level = interfaceEntity.getLevel();
        if (level == null) return 0;

        long extracted = 0;

        if (what instanceof AEItemKey itemKey) {
            for (var pos : interfaceEntity.getConnectedDrawers()) {
                if (level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer) {
                    for (int i = 0; i < drawer.getSlotCount(); i++) {
                        var stored = drawer.getStoredItem(i);
                        if (stored.isEmpty() || !itemKey.matches(stored)) continue;
                        long available = drawer.getStoredCount(i);
                        long toExtract = Math.min(amount - extracted, available);
                        if (toExtract <= 0) continue;
                        if (mode == Actionable.MODULATE) {
                            drawer.extractItem(i, (int) toExtract, false);
                        }
                        extracted += toExtract;
                        if (extracted >= amount) return extracted;
                    }
                }
            }
        } else if (what instanceof AEFluidKey fluidKey) {
            for (var pos : interfaceEntity.getConnectedDrawers()) {
                if (level.getBlockEntity(pos) instanceof FluidDrawerBlockEntity fluidDrawer) {
                    for (int i = 0; i < fluidDrawer.getSlotCount(); i++) {
                        FluidStack stored = fluidDrawer.getStoredFluid(i);
                        if (stored.isEmpty() || !fluidKey.matches(stored)) continue;
                        long available = fluidDrawer.getStoredAmount(i);
                        long toExtract = Math.min(amount - extracted, available);
                        if (toExtract <= 0) continue;
                        if (mode == Actionable.MODULATE) {
                            fluidDrawer.extractFluid(i, (int) toExtract, false);
                        }
                        extracted += toExtract;
                        if (extracted >= amount) return extracted;
                    }
                }
            }
        }

        return extracted;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        Level level = interfaceEntity.getLevel();
        if (level == null) return;

        for (var pos : interfaceEntity.getConnectedDrawers()) {
            var be = level.getBlockEntity(pos);
            if (be instanceof DrawerBlockEntity drawer) {
                for (int i = 0; i < drawer.getSlotCount(); i++) {
                    var stored = drawer.getStoredItem(i);
                    long count = drawer.getStoredCount(i);
                    if (!stored.isEmpty() && count > 0) {
                        AEItemKey key = AEItemKey.of(stored);
                        if (key != null) out.add(key, count);
                    }
                }
            } else if (be instanceof FluidDrawerBlockEntity fluidDrawer) {
                for (int i = 0; i < fluidDrawer.getSlotCount(); i++) {
                    FluidStack stored = fluidDrawer.getStoredFluid(i);
                    long amount = fluidDrawer.getStoredAmount(i);
                    if (!stored.isEmpty() && amount > 0) {
                        AEFluidKey key = AEFluidKey.of(stored);
                        if (key != null) out.add(key, amount);
                    }
                }
            }
        }
    }

    @Override
    public Component getDescription() {
        return Component.literal("Storage Interface");
    }
}