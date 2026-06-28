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

    public StorageRemoteItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static RemoteMode getMode(ItemStack stack) {
        int value = stack.getOrDefault(ModDataComponents.REMOTE_MODE.get(), 0);
        return value == 1 ? RemoteMode.LOCK : RemoteMode.LINK;
    }

    public static void setMode(ItemStack stack, RemoteMode mode) {
        stack.set(
                ModDataComponents.REMOTE_MODE.get(), mode == RemoteMode.LOCK ? 1 : 0);
    }

    public static boolean isLinkMode(ItemStack stack) {
        return getMode(stack) == RemoteMode.LINK;
    }

    public static boolean isLockMode(ItemStack stack) {
        return getMode(stack) == RemoteMode.LOCK;
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

    public static void cycleMode(ItemStack stack) {
        setMode(
                stack,
                getMode(stack) == RemoteMode.LINK ? RemoteMode.LOCK : RemoteMode.LINK);
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

    private InteractionResult handleLinkMode(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        var blockEntity = level.getBlockEntity(pos);

        if (player.isShiftKeyDown()) {
            if (blockEntity instanceof StorageInterfaceBlockEntity) {
                setBoundInterface(stack, pos);
                player.sendOverlayMessage(Component.literal("Remote bound to interface at " + pos.toShortString()).withStyle(ChatFormatting.GREEN));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        BlockPos boundPos = getBoundInterface(stack);
        if (boundPos == null) {
            player.sendOverlayMessage(Component.literal("No Storage Interface bound!").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        if (!(level.getBlockEntity(boundPos) instanceof StorageInterfaceBlockEntity interfaceEntity)) {
            player.sendOverlayMessage(Component.literal("Bound Storage Interface no longer exists at " + boundPos.toShortString()).withStyle(ChatFormatting.RED));
            clearBoundInterface(stack);
            return InteractionResult.FAIL;
        }

        double dx = pos.getX() - boundPos.getX();
        double dy = pos.getY() - boundPos.getY();
        double dz = pos.getZ() - boundPos.getZ();
        if (dx * dx + dy * dy + dz * dz > interfaceEntity.getMaxRangeSq()) {
            int range = (int) Math.sqrt(interfaceEntity.getMaxRangeSq());
            player.sendOverlayMessage(Component.literal("Drawer out of range! (Max " + range + " blocks)").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        if (blockEntity instanceof DrawerBlockEntity drawer) {
            BlockPos currentInterface = drawer.getConnectedInterface();
            if (currentInterface != null) {
                if (!(level.getBlockEntity(currentInterface) instanceof StorageInterfaceBlockEntity)) {
                    drawer.clearConnectedInterface();
                } else if (boundPos.equals(currentInterface)) {
                    interfaceEntity.tryUnlinkDrawer(pos);
                    player.sendOverlayMessage(Component.literal("Drawer unlinked!").withStyle(ChatFormatting.YELLOW));
                    return InteractionResult.SUCCESS;
                } else {
                    player.sendOverlayMessage(Component.literal("Drawer already linked to another interface!").withStyle(ChatFormatting.RED));
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
                    player.sendOverlayMessage(Component.literal("Fluid Drawer unlinked!").withStyle(ChatFormatting.YELLOW));
                    return InteractionResult.SUCCESS;
                } else {
                    player.sendOverlayMessage(Component.literal("Drawer already linked to another interface!").withStyle(ChatFormatting.RED));
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
                    player.sendOverlayMessage(Component.literal("Compacting Drawer unlinked!").withStyle(ChatFormatting.YELLOW));
                    return InteractionResult.SUCCESS;
                } else {
                    player.sendOverlayMessage(Component.literal("Drawer already linked to another interface!").withStyle(ChatFormatting.RED));
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
                player.sendOverlayMessage(Component.literal("Storage Viewer unlinked!").withStyle(ChatFormatting.YELLOW));
                return InteractionResult.SUCCESS;
            }
            viewer.setStorageInterfacePos(boundPos);
            player.sendOverlayMessage(Component.literal("Storage Viewer linked to interface!").withStyle(ChatFormatting.GREEN));
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
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                clearBoundInterface(stack);
                player.sendOverlayMessage(
                        Component.literal("Remote unbound")
                                .withStyle(ChatFormatting.YELLOW));
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {

        RemoteMode mode = getMode(stack);

        builder.accept(Component.literal("Mode: " + (mode == RemoteMode.LINK ? "Link/Unlink" : "Lock/Unlock")).withStyle(ChatFormatting.AQUA));

        BlockPos bound = getBoundInterface(stack);

        if (bound != null) {
            builder.accept(Component.literal("Bound to: " + bound.toShortString()).withStyle(ChatFormatting.GRAY));
        }
        else if (mode == RemoteMode.LINK) {
            builder.accept(Component.literal("Shift + right-click an interface to bind").withStyle(ChatFormatting.DARK_GRAY));
        }

        builder.accept(Component.literal("Shift + scroll to change mode").withStyle(ChatFormatting.DARK_GRAY));

        super.appendHoverText(stack, context, display, builder, flag);
    }
}