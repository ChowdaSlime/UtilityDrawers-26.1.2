package net.drawers.utilitydrawers.item;

import net.drawers.utilitydrawers.UtilityDrawersConfig;
import net.drawers.utilitydrawers.block.entity.FluidDrawerAccess;
import net.drawers.utilitydrawers.block.entity.ItemDrawerAccess;
import net.drawers.utilitydrawers.data.ModDataComponents;
import net.drawers.utilitydrawers.menu.UpgradeConfigMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class InsertUpgradeItem extends Item {

    private static final int SLOTS_PER_DIRECTION = 9;

    public InsertUpgradeItem(Properties properties) {
        super(properties.component(ModDataComponents.ACTIVE_DIRECTIONS, List.of()).component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && !(player instanceof FakePlayer)) {
            if (player.isShiftKeyDown()) {
                ItemStack stack = player.getItemInHand(hand);
                resetUpgrade(stack);
                player.sendOverlayMessage(Component.literal("Insert Upgrade settings reset"));
                return InteractionResult.SUCCESS;
            }

            player.openMenu(new SimpleMenuProvider(
                            (containerId, playerInventory, playerEntity) ->
                                    new UpgradeConfigMenu(containerId, playerInventory, hand),
                            Component.literal("Configure Insert Upgrade")),
                    buf -> {
                        buf.writeBoolean(false);
                        buf.writeEnum(hand);
                    });
        }
        return InteractionResult.SUCCESS;
    }

    private static void resetUpgrade(ItemStack stack) {
        stack.set(ModDataComponents.ACTIVE_DIRECTIONS, List.of());
        stack.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
    }

    public static boolean tryInsert(Level level, BlockPos pos, ItemStack upgrade, ItemDrawerAccess drawer) {
        List<Direction> activeDirs = upgrade.getOrDefault(ModDataComponents.ACTIVE_DIRECTIONS, List.of());

        for (Direction dir : activeDirs) {
            BlockPos neighborPos = pos.relative(dir);
            ResourceHandler<ItemResource> neighbor =
                    level.getCapability(Capabilities.Item.BLOCK, neighborPos, dir.getOpposite());
            if (neighbor == null) continue;

            if (pullOne(drawer, neighbor, upgrade, dir)) return true;
        }
        return false;
    }

    private static boolean pullOne(ItemDrawerAccess drawer, ResourceHandler<ItemResource> neighbor, ItemStack upgrade, Direction dir) {
        for (int nSlot = 0; nSlot < neighbor.size(); nSlot++) {
            ItemResource resource = neighbor.getResource(nSlot);
            if (resource.isEmpty()) continue;

            ItemStack candidateStack = resource.toStack(1);
            if (!matchesFilter(upgrade, candidateStack, dir)) continue;

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

            if (pullOneFluid(drawer, neighbor, upgrade, dir)) return true;
        }
        return false;
    }

    private static boolean pullOneFluid(FluidDrawerAccess drawer, ResourceHandler<FluidResource> neighbor, ItemStack upgrade, Direction dir) {
        for (int nSlot = 0; nSlot < neighbor.size(); nSlot++) {
            FluidResource resource = neighbor.getResource(nSlot);
            if (resource.isEmpty()) continue;

            FluidStack candidateStack = resource.toStack(1);
            if (!matchesFilterFluid(upgrade, candidateStack, dir)) continue;

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

    private static boolean matchesFilter(ItemStack upgrade, ItemStack candidate, Direction dir) {
        ItemContainerContents filter = upgrade.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);

        int startSlot = dir.get3DDataValue() * SLOTS_PER_DIRECTION;
        int endSlot = Math.min(startSlot + SLOTS_PER_DIRECTION, filter.getSlots());
        if (startSlot >= filter.getSlots()) return true;

        boolean hasAnyFilterForDirection = false;
        for (int i = startSlot; i < endSlot; i++) {
            if (!filter.getStackInSlot(i).isEmpty()) {
                hasAnyFilterForDirection = true;
                break;
            }
        }
        if (!hasAnyFilterForDirection) return true;

        for (int i = startSlot; i < endSlot; i++) {
            ItemStack filterStack = filter.getStackInSlot(i);
            if (!filterStack.isEmpty() && ItemStack.isSameItemSameComponents(filterStack, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesFilterFluid(ItemStack upgrade, FluidStack candidate, Direction dir) {
        ItemContainerContents filter = upgrade.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);

        int startSlot = dir.get3DDataValue() * SLOTS_PER_DIRECTION;
        int endSlot = Math.min(startSlot + SLOTS_PER_DIRECTION, filter.getSlots());
        if (startSlot >= filter.getSlots()) return true;

        boolean hasAnyFilterForDirection = false;
        for (int i = startSlot; i < endSlot; i++) {
            if (!filter.getStackInSlot(i).isEmpty()) {
                hasAnyFilterForDirection = true;
                break;
            }
        }
        if (!hasAnyFilterForDirection) return true;

        FluidResource candidateResource = FluidResource.of(candidate);

        for (int i = startSlot; i < endSlot; i++) {
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        List<Direction> activeDirs = stack.getOrDefault(ModDataComponents.ACTIVE_DIRECTIONS, List.of());
        ItemContainerContents filter = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);

        if (activeDirs.isEmpty()) {
            return;
        }

        builder.accept(Component.empty());

        for (Direction dir : activeDirs) {
            int startSlot = dir.get3DDataValue() * SLOTS_PER_DIRECTION;
            int endSlot = Math.min(startSlot + SLOTS_PER_DIRECTION, filter.getSlots());

            List<String> itemNames = new ArrayList<>();

            if (startSlot < filter.getSlots()) {
                for (int i = startSlot; i < endSlot; i++) {
                    ItemStack filterStack = filter.getStackInSlot(i);
                    if (!filterStack.isEmpty()) {
                        String name = filterStack.getHoverName().getString();
                        if (!itemNames.contains(name)) {
                            itemNames.add(name);
                        }
                    }
                }
            }

            String dirString = dir.getName().substring(0, 1).toUpperCase() + dir.getName().substring(1);
            MutableComponent dirComponent = Component.literal(dirString + ": ").withStyle(ChatFormatting.AQUA);

            if (itemNames.isEmpty()) {
                dirComponent.append(Component.literal("All Items/Fluids").withStyle(ChatFormatting.GREEN));
            } else {
                dirComponent.append(Component.literal(String.join(", ", itemNames)).withStyle(ChatFormatting.WHITE));
            }

            builder.accept(dirComponent);
        }
    }
}