package net.drawers.utilitydrawers.item;

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
            player.sendOverlayMessage(Component.literal("Storage Interface support coming soon").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.SUCCESS;
        }

        if (blockEntity instanceof net.drawers.utilitydrawers.block.entity.DrawerBlockEntity) {
            BlockPos boundPos = getBoundInterface(stack);
            if (boundPos == null) {
                player.sendOverlayMessage(Component.literal("No Storage Interface bound! Shift+right-click an interface first.").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            // placeholder until linking logic exists
            player.sendOverlayMessage(Component.literal("Drawer linked to interface at " + boundPos.toShortString()).withStyle(ChatFormatting.GREEN));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private InteractionResult handleLockMode(ItemStack stack, Level level, BlockPos pos, Player player) {
        var blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof net.drawers.utilitydrawers.block.entity.DrawerBlockEntity drawer) {
            boolean nowLocked = !drawer.isLocked();
            drawer.setLocked(nowLocked);

            player.sendOverlayMessage(
                    Component.literal(nowLocked ? "Drawer Locked" : "Drawer Unlocked")
                            .withStyle(nowLocked ? ChatFormatting.RED : ChatFormatting.GREEN));

            return InteractionResult.SUCCESS;
        }

        // placeholder for Storage Interface lock-all
        // if (blockEntity instanceof StorageInterfaceBlockEntity) { ... }

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