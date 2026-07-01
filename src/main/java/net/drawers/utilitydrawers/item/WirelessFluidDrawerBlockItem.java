package net.drawers.utilitydrawers.item;

import net.drawers.utilitydrawers.block.WirelessFluidDrawerBlock;
import net.drawers.utilitydrawers.block.entity.WirelessFluidDrawerBlockEntity;
import net.drawers.utilitydrawers.data.ModDataComponents;
import net.drawers.utilitydrawers.data.WirelessNetworkKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WirelessFluidDrawerBlockItem extends  FluidDrawerBlockItem{

    public WirelessFluidDrawerBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(pos);

        if (clickedState.getBlock() instanceof WirelessFluidDrawerBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof WirelessFluidDrawerBlockEntity wireless) {
                WirelessNetworkKey key = wireless.getNetworkKey();
                if (!level.isClientSide()) {
                    stack.set(ModDataComponents.WIRELESS_NETWORK_KEY, key);
                    stack.set(ModDataComponents.HAS_COPIED_SETTINGS, true);
                    context.getPlayer().sendOverlayMessage(Component.literal("Settings Copied!"));
                }
                return InteractionResult.SUCCESS;
            }
        }

        return super.useOn(context);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.HAS_COPIED_SETTINGS, false);
    }
}