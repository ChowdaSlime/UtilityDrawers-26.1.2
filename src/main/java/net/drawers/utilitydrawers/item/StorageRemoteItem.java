package net.drawers.utilitydrawers.item;

import net.drawers.utilitydrawers.block.StorageInterfaceBlock;
import net.drawers.utilitydrawers.block.entity.*;
import net.drawers.utilitydrawers.data.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class StorageRemoteItem extends Item {

    public enum RemoteMode {
        LINK, LOCK
    }

    public enum SelectMode {
        SINGLE, MULTI
    }

    public StorageRemoteItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static RemoteMode getMode(ItemStack stack) {
        int value = stack.getOrDefault(ModDataComponents.REMOTE_MODE.get(), 0);
        return value == 1 ? RemoteMode.LOCK : RemoteMode.LINK;
    }

    public static void setMode(ItemStack stack, RemoteMode mode) {
        stack.set(ModDataComponents.REMOTE_MODE.get(), mode == RemoteMode.LOCK ? 1 : 0);
    }

    public static boolean isLinkMode(ItemStack stack) {
        return getMode(stack) == RemoteMode.LINK;
    }

    public static boolean isLockMode(ItemStack stack) {
        return getMode(stack) == RemoteMode.LOCK;
    }

    public static void cycleMode(ItemStack stack) {
        setMode(stack, getMode(stack) == RemoteMode.LINK ? RemoteMode.LOCK : RemoteMode.LINK);
    }

    public static SelectMode getSelectMode(ItemStack stack) {
        int value = stack.getOrDefault(ModDataComponents.SELECT_MODE.get(), 0);
        return value == 1 ? SelectMode.MULTI : SelectMode.SINGLE;
    }

    public static void setSelectMode(ItemStack stack, SelectMode mode) {
        stack.set(ModDataComponents.SELECT_MODE.get(), mode == SelectMode.MULTI ? 1 : 0);
        clearMultiSelectCorner(stack);
    }

    public static boolean isMultiSelectMode(ItemStack stack) {
        return getSelectMode(stack) == SelectMode.MULTI;
    }

    public static void cycleSelectMode(ItemStack stack) {
        setSelectMode(stack, getSelectMode(stack) == SelectMode.SINGLE ? SelectMode.MULTI : SelectMode.SINGLE);
    }

    public static BlockPos getBoundInterface(ItemStack stack) {
        return stack.get(ModDataComponents.BOUND_INTERFACE.get());
    }

    public static void setBoundInterface(ItemStack stack, BlockPos pos) {
        stack.set(ModDataComponents.BOUND_INTERFACE.get(), pos);
    }

    public static void clearBoundInterface(ItemStack stack) {
        stack.remove(ModDataComponents.BOUND_INTERFACE.get());
    }

    public static BlockPos getMultiSelectCorner(ItemStack stack) {
        return stack.get(ModDataComponents.MULTI_SELECT_CORNER.get());
    }

    public static void setMultiSelectCorner(ItemStack stack, BlockPos pos) {
        stack.set(ModDataComponents.MULTI_SELECT_CORNER.get(), pos);
    }

    public static void clearMultiSelectCorner(ItemStack stack) {
        stack.remove(ModDataComponents.MULTI_SELECT_CORNER.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (isLinkMode(stack)) {
            return handleLinkMode(stack, level, pos, player, context.getHand());
        } else {
            return handleLockMode(stack, level, pos, player);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                clearBoundInterface(stack);
                player.sendOverlayMessage(
                        Component.literal("Remote unbound").withStyle(ChatFormatting.YELLOW));
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    public static void onShiftLeftClickAir(ItemStack stack, Player player) {
        if (!isLinkMode(stack)) return;

        cycleSelectMode(stack);
        SelectMode newMode = getSelectMode(stack);
        player.sendOverlayMessage(
                Component.literal("Select Mode: " + (newMode == SelectMode.MULTI ? "Multi-Select" : "Single"))
                        .withStyle(ChatFormatting.AQUA));
    }

    private InteractionResult handleLinkMode(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        var blockEntity = level.getBlockEntity(pos);

        if (player.isShiftKeyDown()) {
            if (blockEntity instanceof StorageInterfaceBlockEntity) {
                setBoundInterface(stack, pos);
                clearMultiSelectCorner(stack);
                player.sendOverlayMessage(
                        Component.literal("Remote bound to interface at " + pos.toShortString())
                                .withStyle(ChatFormatting.GREEN));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        BlockPos boundPos = getBoundInterface(stack);
        if (boundPos == null) {
            player.sendOverlayMessage(
                    Component.literal("No Storage Interface bound!").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        if (!(level.getBlockEntity(boundPos) instanceof StorageInterfaceBlockEntity interfaceEntity)) {
            player.sendOverlayMessage(
                    Component.literal("Bound Storage Interface no longer exists at " + boundPos.toShortString())
                            .withStyle(ChatFormatting.RED));
            clearBoundInterface(stack);
            return InteractionResult.FAIL;
        }

        if (isMultiSelectMode(stack)) {
            return handleMultiSelect(stack, level, pos, player, boundPos, interfaceEntity);
        } else {
            return handleSingleLink(stack, level, pos, player, boundPos, interfaceEntity);
        }
    }

    private InteractionResult handleMultiSelect(ItemStack stack, Level level, BlockPos pos, Player player,
                                                BlockPos boundPos, StorageInterfaceBlockEntity interfaceEntity) {
        BlockPos corner = getMultiSelectCorner(stack);

        if (corner == null) {
            setMultiSelectCorner(stack, pos);
            player.sendOverlayMessage(
                    Component.literal("First corner set at " + pos.toShortString() + ". Right-click second corner.")
                            .withStyle(ChatFormatting.AQUA));
            return InteractionResult.SUCCESS;
        }

        clearMultiSelectCorner(stack);

        int minX = Math.min(corner.getX(), pos.getX());
        int minY = Math.min(corner.getY(), pos.getY());
        int minZ = Math.min(corner.getZ(), pos.getZ());
        int maxX = Math.max(corner.getX(), pos.getX());
        int maxY = Math.max(corner.getY(), pos.getY());
        int maxZ = Math.max(corner.getZ(), pos.getZ());

        int linked = 0;
        int outOfRange = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos target = new BlockPos(x, y, z);

                    double dx = target.getX() - boundPos.getX();
                    double dy = target.getY() - boundPos.getY();
                    double dz = target.getZ() - boundPos.getZ();
                    if (dx * dx + dy * dy + dz * dz > interfaceEntity.getMaxRangeSq()) {
                        outOfRange++;
                        continue;
                    }

                    if (tryLinkDrawerAt(level, target, boundPos, interfaceEntity)) {
                        linked++;
                    }
                }
            }
        }

        if (linked > 0) {
            String msg = "Linked " + linked + " drawer" + (linked == 1 ? "" : "s") + "!";
            if (outOfRange > 0) msg += " (" + outOfRange + " out of range)";
            player.sendOverlayMessage(Component.literal(msg).withStyle(ChatFormatting.GREEN));
        } else {
            String msg = "No drawers linked.";
            if (outOfRange > 0) msg += " (" + outOfRange + " block" + (outOfRange == 1 ? "" : "s") + " out of range)";
            player.sendOverlayMessage(Component.literal(msg).withStyle(ChatFormatting.YELLOW));
        }

        return InteractionResult.SUCCESS;
    }

    private boolean tryLinkDrawerAt(Level level, BlockPos target, BlockPos boundPos,
                                    StorageInterfaceBlockEntity interfaceEntity) {
        var blockEntity = level.getBlockEntity(target);
        if (blockEntity == null) return false;

        if (blockEntity instanceof WirelessDrawerBlockEntity wirelessDrawer) {
            return linkIfFree(wirelessDrawer.getConnectedInterface(), boundPos, target, interfaceEntity,
                    () -> wirelessDrawer.clearConnectedInterface());
        }

        if (blockEntity instanceof WirelessFluidDrawerBlockEntity wirelessFluidDrawer) {
            return linkIfFree(wirelessFluidDrawer.getConnectedInterface(), boundPos, target, interfaceEntity,
                    () -> wirelessFluidDrawer.clearConnectedInterface());
        }

        if (blockEntity instanceof DrawerBlockEntity drawer) {
            return linkIfFree(drawer.getConnectedInterface(), boundPos, target, interfaceEntity,
                    () -> drawer.clearConnectedInterface());
        }

        if (blockEntity instanceof FluidDrawerBlockEntity fluidDrawer) {
            return linkIfFree(fluidDrawer.getConnectedInterface(), boundPos, target, interfaceEntity,
                    () -> fluidDrawer.clearConnectedInterface());
        }

        if (blockEntity instanceof CompactingDrawerBlockEntity compactingDrawer) {
            return linkIfFree(compactingDrawer.getConnectedInterface(), boundPos, target, interfaceEntity,
                    () -> compactingDrawer.clearConnectedInterface());
        }

        return false;
    }

    private boolean linkIfFree(BlockPos currentInterface, BlockPos boundPos, BlockPos drawerPos,
                               StorageInterfaceBlockEntity interfaceEntity, Runnable clearFn) {
        if (currentInterface != null) {
            if (currentInterface.equals(boundPos)) {
                return false;
            }
            if (interfaceEntity.getLevel() != null &&
                    interfaceEntity.getLevel().getBlockEntity(currentInterface) instanceof StorageInterfaceBlockEntity) {
                return false;
            }
            clearFn.run();
        }
        return interfaceEntity.tryLinkDrawer(drawerPos);
    }

    private InteractionResult handleSingleLink(ItemStack stack, Level level, BlockPos pos, Player player,
                                               BlockPos boundPos, StorageInterfaceBlockEntity interfaceEntity) {
        var blockEntity = level.getBlockEntity(pos);

        double dx = pos.getX() - boundPos.getX();
        double dy = pos.getY() - boundPos.getY();
        double dz = pos.getZ() - boundPos.getZ();
        if (dx * dx + dy * dy + dz * dz > interfaceEntity.getMaxRangeSq()) {
            int range = (int) Math.sqrt(interfaceEntity.getMaxRangeSq());
            player.sendOverlayMessage(
                    Component.literal("Drawer out of range! (Max " + range + " blocks)").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        if (blockEntity instanceof WirelessDrawerBlockEntity wirelessDrawer) {
            BlockPos currentInterface = wirelessDrawer.getConnectedInterface();
            if (currentInterface != null) {
                if (!(level.getBlockEntity(currentInterface) instanceof StorageInterfaceBlockEntity)) {
                    wirelessDrawer.clearConnectedInterface();
                } else if (boundPos.equals(currentInterface)) {
                    interfaceEntity.tryUnlinkDrawer(pos);
                    player.sendOverlayMessage(Component.literal("Wireless Drawer unlinked!").withStyle(ChatFormatting.YELLOW));
                    return InteractionResult.SUCCESS;
                } else {
                    player.sendOverlayMessage(
                            Component.literal("Drawer already linked to another interface!").withStyle(ChatFormatting.RED));
                    return InteractionResult.FAIL;
                }
            }
            if (interfaceEntity.tryLinkDrawer(pos)) {
                player.sendOverlayMessage(Component.literal("Wireless Drawer linked!").withStyle(ChatFormatting.GREEN));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;

        } else if (blockEntity instanceof WirelessFluidDrawerBlockEntity wirelessFluidDrawer) {
            BlockPos currentInterface = wirelessFluidDrawer.getConnectedInterface();
            if (currentInterface != null) {
                if (!(level.getBlockEntity(currentInterface) instanceof StorageInterfaceBlockEntity)) {
                    wirelessFluidDrawer.clearConnectedInterface();
                } else if (boundPos.equals(currentInterface)) {
                    interfaceEntity.tryUnlinkDrawer(pos);
                    player.sendOverlayMessage(Component.literal("Wireless Fluid Drawer unlinked!").withStyle(ChatFormatting.YELLOW));
                    return InteractionResult.SUCCESS;
                } else {
                    player.sendOverlayMessage(
                            Component.literal("Drawer already linked to another interface!").withStyle(ChatFormatting.RED));
                    return InteractionResult.FAIL;
                }
            }
            if (interfaceEntity.tryLinkDrawer(pos)) {
                player.sendOverlayMessage(Component.literal("Wireless Fluid Drawer linked!").withStyle(ChatFormatting.GREEN));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;

        } else if (blockEntity instanceof DrawerBlockEntity drawer) {
            BlockPos currentInterface = drawer.getConnectedInterface();
            if (currentInterface != null) {
                if (!(level.getBlockEntity(currentInterface) instanceof StorageInterfaceBlockEntity)) {
                    drawer.clearConnectedInterface();
                } else if (boundPos.equals(currentInterface)) {
                    interfaceEntity.tryUnlinkDrawer(pos);
                    player.sendOverlayMessage(Component.literal("Drawer unlinked!").withStyle(ChatFormatting.YELLOW));
                    return InteractionResult.SUCCESS;
                } else {
                    player.sendOverlayMessage(
                            Component.literal("Drawer already linked to another interface!").withStyle(ChatFormatting.RED));
                    return InteractionResult.FAIL;
                }
            }
            if (interfaceEntity.tryLinkDrawer(pos)) {
                player.sendOverlayMessage(Component.literal("Drawer linked!").withStyle(ChatFormatting.GREEN));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;

        } else if (blockEntity instanceof FluidDrawerBlockEntity fluidDrawer) {
            BlockPos currentInterface = fluidDrawer.getConnectedInterface();
            if (currentInterface != null) {
                if (!(level.getBlockEntity(currentInterface) instanceof StorageInterfaceBlockEntity)) {
                    fluidDrawer.clearConnectedInterface();
                } else if (boundPos.equals(currentInterface)) {
                    interfaceEntity.tryUnlinkDrawer(pos);
                    player.sendOverlayMessage(
                            Component.literal("Fluid Drawer unlinked!").withStyle(ChatFormatting.YELLOW));
                    return InteractionResult.SUCCESS;
                } else {
                    player.sendOverlayMessage(
                            Component.literal("Drawer already linked to another interface!").withStyle(ChatFormatting.RED));
                    return InteractionResult.FAIL;
                }
            }
            if (interfaceEntity.tryLinkDrawer(pos)) {
                player.sendOverlayMessage(Component.literal("Fluid Drawer linked!").withStyle(ChatFormatting.GREEN));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;

        } else if (blockEntity instanceof CompactingDrawerBlockEntity compactingDrawer) {
            BlockPos currentInterface = compactingDrawer.getConnectedInterface();
            if (currentInterface != null) {
                if (!(level.getBlockEntity(currentInterface) instanceof StorageInterfaceBlockEntity)) {
                    compactingDrawer.clearConnectedInterface();
                } else if (boundPos.equals(currentInterface)) {
                    interfaceEntity.tryUnlinkDrawer(pos);
                    player.sendOverlayMessage(
                            Component.literal("Compacting Drawer unlinked!").withStyle(ChatFormatting.YELLOW));
                    return InteractionResult.SUCCESS;
                } else {
                    player.sendOverlayMessage(
                            Component.literal("Drawer already linked to another interface!").withStyle(ChatFormatting.RED));
                    return InteractionResult.FAIL;
                }
            }
            if (interfaceEntity.tryLinkDrawer(pos)) {
                player.sendOverlayMessage(Component.literal("Compacting Drawer linked!").withStyle(ChatFormatting.GREEN));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;

        } else if (blockEntity instanceof StorageViewerBlockEntity viewer) {
            BlockPos current = viewer.getStorageInterfacePos();
            if (current != null && current.equals(boundPos)) {
                viewer.setStorageInterfacePos(null);
                player.sendOverlayMessage(
                        Component.literal("Storage Viewer unlinked!").withStyle(ChatFormatting.YELLOW));
                return InteractionResult.SUCCESS;
            }
            viewer.setStorageInterfacePos(boundPos);
            player.sendOverlayMessage(
                    Component.literal("Storage Viewer linked to interface!").withStyle(ChatFormatting.GREEN));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private InteractionResult handleLockMode(ItemStack stack, Level level, BlockPos pos, Player player) {
        var blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof DrawerBlockEntity drawer) {
            boolean nowLocked = !drawer.isLocked();
            drawer.setLocked(nowLocked);
            player.sendOverlayMessage(
                    Component.literal(nowLocked ? "Drawer Locked" : "Drawer Unlocked")
                            .withStyle(nowLocked ? ChatFormatting.RED : ChatFormatting.GREEN));
            return InteractionResult.SUCCESS;
        }

        if (blockEntity instanceof FluidDrawerBlockEntity fluidDrawer) {
            boolean nowLocked = !fluidDrawer.isLocked();
            fluidDrawer.setLocked(nowLocked);
            player.sendOverlayMessage(
                    Component.literal(nowLocked ? "Fluid Drawer Locked" : "Fluid Drawer Unlocked")
                            .withStyle(nowLocked ? ChatFormatting.RED : ChatFormatting.GREEN));
            return InteractionResult.SUCCESS;
        }

        if (blockEntity instanceof CompactingDrawerBlockEntity compactingDrawer) {
            boolean nowLocked = !compactingDrawer.isLocked();
            compactingDrawer.setLocked(nowLocked);
            player.sendOverlayMessage(
                    Component.literal(nowLocked ? "Compacting Drawer Locked" : "Compacting Drawer Unlocked")
                            .withStyle(nowLocked ? ChatFormatting.RED : ChatFormatting.GREEN));
            return InteractionResult.SUCCESS;
        }

        if (blockEntity instanceof StorageInterfaceBlockEntity interfaceEntity) {
            boolean currentLockState = level.getBlockState(pos).getValue(StorageInterfaceBlock.LOCKED);
            boolean newLockState = !currentLockState;
            level.setBlock(pos, level.getBlockState(pos).setValue(StorageInterfaceBlock.LOCKED, newLockState), 3);
            interfaceEntity.toggleNetworkLock(newLockState);
            player.sendOverlayMessage(
                    Component.literal(newLockState ? "All Connected Drawers Locked" : "All Connected Drawers Unlocked")
                            .withStyle(newLockState ? ChatFormatting.RED : ChatFormatting.GREEN));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {

        RemoteMode mode = getMode(stack);
        builder.accept(Component.literal("Mode: " + (mode == RemoteMode.LINK ? "Link/Unlink" : "Lock/Unlock"))
                .withStyle(ChatFormatting.AQUA));

        if (mode == RemoteMode.LINK) {
            SelectMode selectMode = getSelectMode(stack);
            builder.accept(Component.literal("Select: " + (selectMode == SelectMode.MULTI ? "Multi" : "Single"))
                    .withStyle(ChatFormatting.AQUA));

            if (selectMode == SelectMode.MULTI) {
                BlockPos corner = getMultiSelectCorner(stack);
                if (corner != null) {
                    builder.accept(Component.literal("Corner 1: " + corner.toShortString())
                            .withStyle(ChatFormatting.GRAY));
                }
            }
        }

        BlockPos bound = getBoundInterface(stack);
        if (bound != null) {
            builder.accept(Component.literal("Bound to: " + bound.toShortString()).withStyle(ChatFormatting.GRAY));
        } else if (mode == RemoteMode.LINK) {
            builder.accept(Component.literal("Shift + right-click an interface to bind").withStyle(ChatFormatting.DARK_GRAY));
        }

        builder.accept(Component.literal("Shift + scroll to change mode").withStyle(ChatFormatting.DARK_GRAY));
        if (mode == RemoteMode.LINK) {
            builder.accept(Component.literal("Shift + left-click air to toggle multi-select").withStyle(ChatFormatting.DARK_GRAY));
        }

        super.appendHoverText(stack, context, display, builder, flag);
    }
}