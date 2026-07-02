package net.drawers.utilitydrawers.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import net.drawers.utilitydrawers.block.entity.CompactingDrawerBlockEntity;
import net.minecraft.network.chat.Component;

public class CompactingDrawerMEStorage implements MEStorage {

    private final CompactingDrawerBlockEntity drawer;

    public CompactingDrawerMEStorage(CompactingDrawerBlockEntity drawer) {
        this.drawer = drawer;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (!(what instanceof AEItemKey itemKey)) return 0;

        long inserted = 0;
        for (int i = 0; i < drawer.getSlotCount(); i++) {
            var stored = drawer.getStoredItem(i);

            if (!stored.isEmpty() && !itemKey.matches(stored)) continue;

            if (stored.isEmpty() && drawer.isLocked()) continue;

            var toInsert = itemKey.toStack((int) Math.min(amount - inserted, Integer.MAX_VALUE));
            var remainder = drawer.insertItemIntoSlot(i, toInsert, mode == Actionable.SIMULATE);

            int actuallyInserted = toInsert.getCount() - remainder.getCount();
            inserted += actuallyInserted;

            if (actuallyInserted > 0 || !stored.isEmpty()) {
                break;
            }
        }
        return inserted;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (!(what instanceof AEItemKey itemKey)) return 0;

        long extracted = 0;
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

            if (extracted >= amount) break;
        }
        return extracted;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        for (int i = 0; i < drawer.getSlotCount(); i++) {
            var stored = drawer.getStoredItem(i);
            long count = drawer.getStoredCount(i);
            if (!stored.isEmpty() && count > 0) {
                AEItemKey key = AEItemKey.of(stored);
                if (key != null) {
                    out.add(key, count);
                }
            }
        }
    }

    @Override
    public Component getDescription() {
        return Component.literal("Compacting Utility Drawer");
    }
}