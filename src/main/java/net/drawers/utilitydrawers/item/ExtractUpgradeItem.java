package net.drawers.utilitydrawers.item;

import net.drawers.utilitydrawers.UtilityDrawersConfig;
import net.drawers.utilitydrawers.block.entity.FluidDrawerAccess;
import net.drawers.utilitydrawers.block.entity.ItemDrawerAccess;
import net.drawers.utilitydrawers.data.ModDataComponents;
import net.drawers.utilitydrawers.menu.UpgradeConfigMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;

public class ExtractUpgradeItem extends Item {

    public ExtractUpgradeItem(Properties properties) {
        super(properties
                .component(ModDataComponents.ACTIVE_DIRECTIONS, List.of())
                .component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
        );
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && !(player instanceof FakePlayer)) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, playerEntity) ->
                            new UpgradeConfigMenu(containerId, playerInventory, hand),
                    Component.literal("Configure Extract Upgrade")));
        }
        return InteractionResult.SUCCESS;
    }

    public static boolean tryExtract(Level level, BlockPos pos, ItemStack upgrade, ItemDrawerAccess drawer) {
        List<Direction> activeDirs = upgrade.getOrDefault(ModDataComponents.ACTIVE_DIRECTIONS, List.of());

        for (Direction dir : activeDirs) {
            BlockPos neighborPos = pos.relative(dir);
            ResourceHandler<ItemResource> neighbor =
                    level.getCapability(Capabilities.Item.BLOCK, neighborPos, dir.getOpposite());

            if (neighbor == null) continue;

            for (int slot = 0; slot < drawer.getSlotCount(); slot++) {
                if (drawer.isSlotEmpty(slot)) continue;
                if (pushOne(drawer, slot, neighbor, upgrade)) return true;
            }
        }
        return false;
    }

    private static boolean pushOne(ItemDrawerAccess drawer, int slot, ResourceHandler<ItemResource> neighbor, ItemStack upgrade) {
        ItemStack stored = drawer.getStoredItem(slot);
        if (stored.isEmpty()) return false;

        if (!matchesFilter(upgrade, stored)) return false;

        ItemResource resource = ItemResource.of(stored);
        ItemStack simExtract = drawer.extractItem(slot, UtilityDrawersConfig.UPGRADE_TRANSFER_ITEM_AMOUNT.get(), true);
        int candidateAmount = simExtract.getCount();
        if (candidateAmount <= 0) return false;

        try (Transaction tx = Transaction.open(null)) {
            int inserted = neighbor.insert(resource, candidateAmount, tx);
            if (inserted <= 0) return false;

            ItemStack extracted = drawer.extractItem(slot, inserted, false);
            if (extracted.getCount() != inserted) {
                if (!extracted.isEmpty()) {
                    drawer.insertItemIntoSlot(slot, extracted, false);
                }
                return false;
            }

            tx.commit();
            return true;
        }
    }

    public static boolean tryExtract(Level level, BlockPos pos, ItemStack upgrade, FluidDrawerAccess drawer) {
        List<Direction> activeDirs = upgrade.getOrDefault(ModDataComponents.ACTIVE_DIRECTIONS, List.of());

        for (Direction dir : activeDirs) {
            BlockPos neighborPos = pos.relative(dir);
            ResourceHandler<FluidResource> neighbor =
                    level.getCapability(Capabilities.Fluid.BLOCK, neighborPos, dir.getOpposite());

            if (neighbor == null) continue;

            for (int slot = 0; slot < drawer.getSlotCount(); slot++) {
                if (drawer.isSlotEmpty(slot)) continue;
                if (pushOneFluid(drawer, slot, neighbor, upgrade)) return true;
            }
        }
        return false;
    }

    private static boolean pushOneFluid(FluidDrawerAccess drawer, int slot, ResourceHandler<FluidResource> neighbor, ItemStack upgrade) {
        FluidStack stored = drawer.getStoredFluid(slot);
        if (stored.isEmpty()) return false;

        if (!matchesFilterFluid(upgrade, stored)) return false;

        FluidResource resource = FluidResource.of(stored);
        FluidStack simExtract = drawer.extractFluid(slot, UtilityDrawersConfig.UPGRADE_TRANSFER_FLUID_AMOUNT.get(), true);
        int candidateAmount = simExtract.getAmount();
        if (candidateAmount <= 0) return false;

        try (Transaction tx = Transaction.open(null)) {
            int inserted = neighbor.insert(resource, candidateAmount, tx);
            if (inserted <= 0) return false;

            FluidStack extracted = drawer.extractFluid(slot, inserted, false);
            if (extracted.getAmount() != inserted) {
                if (!extracted.isEmpty()) {
                    drawer.insertFluidIntoSlot(slot, extracted, false);
                }
                return false;
            }

            tx.commit();
            return true;
        }
    }


    private static boolean matchesFilter(ItemStack upgrade, ItemStack candidate) {
        ItemContainerContents filter = upgrade.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (filter.getSlots() == 0) return true;

        for (int i = 0; i < filter.getSlots(); i++) {
            ItemStack filterStack = filter.getStackInSlot(i);
            if (!filterStack.isEmpty() && ItemStack.isSameItemSameComponents(filterStack, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesFilterFluid(ItemStack upgrade, FluidStack candidate) {
        ItemContainerContents filter = upgrade.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (filter.getSlots() == 0) return true;

        FluidResource candidateResource = FluidResource.of(candidate);

        for (int i = 0; i < filter.getSlots(); i++) {
            ItemStack filterStack = filter.getStackInSlot(i);
            if (filterStack.isEmpty()) continue;

            ResourceHandler<FluidResource> fluidHandler = ItemAccess.forStack(filterStack).getCapability(Capabilities.Fluid.ITEM);

            if (fluidHandler != null) {
                for (int j = 0; j < fluidHandler.size(); j++) {
                    FluidResource resource = fluidHandler.getResource(j);
                    if (!resource.isEmpty() && resource.equals(candidateResource)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}