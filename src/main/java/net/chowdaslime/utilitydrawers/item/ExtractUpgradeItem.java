package net.chowdaslime.utilitydrawers.item;

import net.chowdaslime.utilitydrawers.UtilityDrawersConfig;
import net.chowdaslime.utilitydrawers.block.entity.FluidDrawerAccess;
import net.chowdaslime.utilitydrawers.block.entity.ItemDrawerAccess;
import net.chowdaslime.utilitydrawers.data.ModDataComponents;
import net.chowdaslime.utilitydrawers.menu.UpgradeConfigMenu;
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

import java.util.ArrayList;
import java.util.List;

public class ExtractUpgradeItem extends Item {

    private static final int SLOTS_PER_DIRECTION = 9;

    public ExtractUpgradeItem(Properties properties) {
        super(properties
                .component(ModDataComponents.ACTIVE_DIRECTIONS, List.of())
                .component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
        );
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && !(player instanceof FakePlayer)) {
            if (player.isShiftKeyDown()) {
                ItemStack stack = player.getItemInHand(hand);
                resetUpgrade(stack);
                player.sendOverlayMessage(Component.literal("Extract Upgrade settings reset"));
                return InteractionResult.SUCCESS;
            }

            player.openMenu(new SimpleMenuProvider(
                            (containerId, playerInventory, playerEntity) ->
                                    new UpgradeConfigMenu(containerId, playerInventory, hand),
                            Component.literal("Configure Extract Upgrade")),
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

    public static boolean tryExtract(Level level, BlockPos pos, ItemStack upgrade, ItemDrawerAccess drawer) {
        List<Direction> activeDirs = upgrade.getOrDefault(ModDataComponents.ACTIVE_DIRECTIONS, List.of());

        for (Direction dir : activeDirs) {
            BlockPos neighborPos = pos.relative(dir);
            ResourceHandler<ItemResource> neighbor =
                    level.getCapability(Capabilities.Item.BLOCK, neighborPos, dir.getOpposite());

            if (neighbor == null) continue;

            for (int slot = 0; slot < drawer.getSlotCount(); slot++) {
                if (drawer.isSlotEmpty(slot)) continue;
                if (pushOne(drawer, slot, neighbor, upgrade, dir)) return true;
            }
        }
        return false;
    }

    private static boolean pushOne(ItemDrawerAccess drawer, int slot, ResourceHandler<ItemResource> neighbor, ItemStack upgrade, Direction dir) {
        ItemStack stored = drawer.getStoredItem(slot);
        if (stored.isEmpty()) return false;

        if (!matchesFilter(upgrade, stored, dir)) return false;

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
                if (pushOneFluid(drawer, slot, neighbor, upgrade, dir)) return true;
            }
        }
        return false;
    }

    private static boolean pushOneFluid(FluidDrawerAccess drawer, int slot, ResourceHandler<FluidResource> neighbor, ItemStack upgrade, Direction dir) {
        FluidStack stored = drawer.getStoredFluid(slot);
        if (stored.isEmpty()) return false;

        if (!matchesFilterFluid(upgrade, stored, dir)) return false;

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

    public static void addDynamicTooltip(ItemStack stack, List<Component> tooltip) {
        List<Direction> activeDirs = stack.getOrDefault(ModDataComponents.ACTIVE_DIRECTIONS, List.of());
        ItemContainerContents filter = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);

        if (activeDirs.isEmpty()) {
            return;
        }

        tooltip.add(Component.empty());

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

            tooltip.add(dirComponent);
        }
    }
}