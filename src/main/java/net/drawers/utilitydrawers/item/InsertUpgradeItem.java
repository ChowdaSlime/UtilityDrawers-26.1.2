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

public class InsertUpgradeItem extends Item {

    public InsertUpgradeItem(Properties properties) {
        super(properties.component(ModDataComponents.ACTIVE_DIRECTIONS, List.of()).component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && !(player instanceof FakePlayer)) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, playerEntity) ->
                            new UpgradeConfigMenu(containerId, playerInventory, hand), Component.literal("Configure Insert Upgrade")));
        }
        return InteractionResult.SUCCESS;
    }

    public static boolean tryInsert(Level level, BlockPos pos, ItemStack upgrade, ItemDrawerAccess drawer) {
        List<Direction> activeDirs = upgrade.getOrDefault(ModDataComponents.ACTIVE_DIRECTIONS, List.of());

        for (Direction dir : activeDirs) {
            BlockPos neighborPos = pos.relative(dir);
            ResourceHandler<ItemResource> neighbor =
                    level.getCapability(Capabilities.Item.BLOCK, neighborPos, dir.getOpposite());
            if (neighbor == null) continue;

            if (pullOne(drawer, neighbor, upgrade)) return true;
        }
        return false;
    }

    private static boolean pullOne(ItemDrawerAccess drawer, ResourceHandler<ItemResource> neighbor, ItemStack upgrade) {
        for (int nSlot = 0; nSlot < neighbor.size(); nSlot++) {
            ItemResource resource = neighbor.getResource(nSlot);
            if (resource.isEmpty()) continue;

            ItemStack candidateStack = resource.toStack(1);
            if (!matchesFilter(upgrade, candidateStack)) continue;

            int drawerSlot = findMatchingOrEmptySlot(drawer, resource);
            if (drawerSlot == -1) continue;

            long available = neighbor.getAmountAsLong(nSlot);
            int candidateAmount = (int) Math.min(available, UtilityDrawersConfig.UPGRADE_TRANSFER_ITEM_AMOUNT.get());
            ItemStack candidate = resource.toStack(candidateAmount);

            ItemStack simRemainder = drawer.insertItemIntoSlot(drawerSlot, candidate, true);
            int accepted = candidateAmount - simRemainder.getCount();
            if (accepted <= 0) continue;

            try (Transaction tx = Transaction.open(null)) {
                int extracted = neighbor.extract(nSlot, resource, accepted, tx);
                if (extracted <= 0) continue;

                drawer.insertItemIntoSlot(drawerSlot, resource.toStack(extracted), false);
                tx.commit();
                return true;
            }
        }
        return false;
    }

    private static int findMatchingOrEmptySlot(ItemDrawerAccess drawer, ItemResource resource) {
        int firstEmpty = -1;
        for (int i = 0; i < drawer.getSlotCount(); i++) {
            ItemStack stored = drawer.getStoredItem(i);
            if (!stored.isEmpty() && ItemResource.of(stored).equals(resource)) {
                return i;
            }
            if (stored.isEmpty() && firstEmpty == -1) {
                firstEmpty = i;
            }
        }
        return firstEmpty;
    }

    public static boolean tryInsert(Level level, BlockPos pos, ItemStack upgrade, FluidDrawerAccess drawer) {
        List<Direction> activeDirs = upgrade.getOrDefault(ModDataComponents.ACTIVE_DIRECTIONS, List.of());

        for (Direction dir : activeDirs) {
            BlockPos neighborPos = pos.relative(dir);
            ResourceHandler<FluidResource> neighbor =
                    level.getCapability(Capabilities.Fluid.BLOCK, neighborPos, dir.getOpposite());
            if (neighbor == null) continue;

            if (pullOneFluid(drawer, neighbor, upgrade)) return true;
        }
        return false;
    }

    private static boolean pullOneFluid(FluidDrawerAccess drawer, ResourceHandler<FluidResource> neighbor, ItemStack upgrade) {
        for (int nSlot = 0; nSlot < neighbor.size(); nSlot++) {
            FluidResource resource = neighbor.getResource(nSlot);
            if (resource.isEmpty()) continue;

            FluidStack candidateStack = resource.toStack(1);
            if (!matchesFilterFluid(upgrade, candidateStack)) continue;

            int drawerSlot = findMatchingOrEmptyFluidSlot(drawer, resource);
            if (drawerSlot == -1) continue;

            long available = neighbor.getAmountAsLong(nSlot);
            int candidateAmount = (int) Math.min(available, UtilityDrawersConfig.UPGRADE_TRANSFER_FLUID_AMOUNT.get());

            FluidStack candidate = resource.toStack(candidateAmount);
            FluidStack simRemainder = drawer.insertFluidIntoSlot(drawerSlot, candidate, true);
            int accepted = candidateAmount - simRemainder.getAmount();
            if (accepted <= 0) continue;

            try (Transaction tx = Transaction.open(null)) {
                int extracted = neighbor.extract(nSlot, resource, accepted, tx);
                if (extracted <= 0) continue;

                drawer.insertFluidIntoSlot(drawerSlot, resource.toStack(extracted), false);
                tx.commit();
                return true;
            }
        }
        return false;
    }

    private static int findMatchingOrEmptyFluidSlot(FluidDrawerAccess drawer, FluidResource resource) {
        int firstEmpty = -1;
        for (int i = 0; i < drawer.getSlotCount(); i++) {
            FluidStack stored = drawer.getStoredFluid(i);
            if (!stored.isEmpty() && FluidResource.of(stored).equals(resource)) {
                return i;
            }
            if (stored.isEmpty() && firstEmpty == -1) {
                firstEmpty = i;
            }
        }
        return firstEmpty;
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