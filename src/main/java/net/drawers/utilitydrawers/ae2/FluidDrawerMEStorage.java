package net.drawers.utilitydrawers.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import net.drawers.utilitydrawers.block.entity.FluidDrawerBlockEntity;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;

public class FluidDrawerMEStorage implements MEStorage {

    private final FluidDrawerBlockEntity drawer;

    public FluidDrawerMEStorage(FluidDrawerBlockEntity drawer) {
        this.drawer = drawer;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (!(what instanceof AEFluidKey fluidKey)) return 0;

        long inserted = 0;
        for (int i = 0; i < drawer.getSlotCount(); i++) {
            FluidStack stored = drawer.getStoredFluid(i);
            if (!stored.isEmpty() && !fluidKey.matches(stored)) continue;
            if (stored.isEmpty() && drawer.isLocked() && !drawer.hasTemplate(i)) continue;

            FluidStack toInsert = fluidKey.toStack((int) Math.min(amount - inserted, Integer.MAX_VALUE));
            FluidStack remainder = drawer.insertFluidIntoSlot(i, toInsert, mode == Actionable.SIMULATE);
            inserted += toInsert.getAmount() - remainder.getAmount();
            if (inserted >= amount) break;
        }
        return inserted;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (!(what instanceof AEFluidKey fluidKey)) return 0;

        long extracted = 0;
        for (int i = 0; i < drawer.getSlotCount(); i++) {
            FluidStack stored = drawer.getStoredFluid(i);
            if (stored.isEmpty() || !fluidKey.matches(stored)) continue;

            long available = drawer.getStoredAmount(i);
            long toExtract = Math.min(amount - extracted, available);
            if (toExtract <= 0) continue;

            if (mode == Actionable.MODULATE) {
                drawer.extractFluid(i, (int) toExtract, false);
            }
            extracted += toExtract;
            if (extracted >= amount) break;
        }
        return extracted;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        for (int i = 0; i < drawer.getSlotCount(); i++) {
            FluidStack stored = drawer.getStoredFluid(i);
            long amount = drawer.getStoredAmount(i);
            if (!stored.isEmpty() && amount > 0) {
                AEFluidKey key = AEFluidKey.of(stored);
                if (key != null) {
                    out.add(key, amount);
                }
            }
        }
    }

    @Override
    public Component getDescription() {
        return Component.literal("Fluid Drawer");
    }
}