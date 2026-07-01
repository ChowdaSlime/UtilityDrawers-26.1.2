package net.drawers.utilitydrawers.block;

import net.drawers.utilitydrawers.block.entity.WirelessDrawerBlockEntity;
import net.drawers.utilitydrawers.data.ModDataComponents;
import net.drawers.utilitydrawers.data.WirelessNetworkKey;
import net.drawers.utilitydrawers.item.StorageRemoteItem;
import net.drawers.utilitydrawers.item.WirelessDrawerBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.*;

public class WirelessDrawerBlock extends DrawerBlock {

    public WirelessDrawerBlock(BlockBehaviour.Properties properties, int slotCount) {
        super(properties, slotCount);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WirelessDrawerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return handleWirelessInteraction(state, level, pos, player, hit, stack, hand);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return handleWirelessInteraction(state, level, pos, player, hit, ItemStack.EMPTY, InteractionHand.MAIN_HAND);
    }

    private static final Map<UUID, Long> LAST_CLICK_TIME = new HashMap<>();

    private InteractionResult handleWirelessInteraction(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit, ItemStack handStack, InteractionHand hand) {
        if (handStack.getItem() instanceof StorageRemoteItem) {
            return InteractionResult.PASS;
        }

        if (handStack.getItem() instanceof WirelessDrawerBlockItem) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (!(level.getBlockEntity(pos) instanceof WirelessDrawerBlockEntity drawer)) {
            return InteractionResult.PASS;
        }

        boolean missedFace = hit.getDirection() != state.getValue(FACING);
        int targetSlot = missedFace ? -1 : getTargetSlot(hit.getLocation(), state, drawer.getSlotCount());

        if (missedFace || targetSlot < 0) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(drawer, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        long currentTime = System.currentTimeMillis();
        long lastClick = LAST_CLICK_TIME.getOrDefault(player.getUUID(), 0L);
        boolean isDoubleClick = (currentTime - lastClick) < 300;
        LAST_CLICK_TIME.put(player.getUUID(), currentTime);

        if (isDoubleClick) {
            boolean insertedAny = false;
            for (int j = 0; j < 36; j++) {
                ItemStack invStack = player.getInventory().getItem(j);
                if (invStack.isEmpty()) continue;

                for (int i = 0; i < drawer.getSlotCount(); i++) {
                    ItemStack storedStack = drawer.getStoredItem(i);
                    if (!storedStack.isEmpty() && ItemStack.isSameItem(storedStack, invStack)) {
                        int startingCount = invStack.getCount();
                        invStack = drawer.insertItemIntoSlot(i, invStack, false);
                        if (invStack.getCount() != startingCount) {
                            player.getInventory().setItem(j, invStack);
                            insertedAny = true;
                        }
                        break;
                    }
                }
            }

            if (insertedAny) {
                player.inventoryMenu.broadcastChanges();
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2f, 1.0f);
            }
            return InteractionResult.CONSUME;
        }

        if (!handStack.isEmpty()) {
            if (drawer.isLocked() && !drawer.hasTemplate(targetSlot)) {
                drawer.setTemplate(targetSlot, handStack);
            }
            ItemStack remainder = drawer.insertItemIntoSlot(targetSlot, handStack, false);
            if (remainder.getCount() != handStack.getCount()) {
                player.setItemInHand(hand, remainder);
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack dropStack = new ItemStack(this);
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof WirelessDrawerBlockEntity wireless) {
            CompoundTag tag = wireless.saveDrawerData(builder.getLevel().registryAccess());
            if (!tag.isEmpty()) {
                dropStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }

            dropStack.set(ModDataComponents.WIRELESS_NETWORK_KEY, wireless.getNetworkKey());
        }
        return List.of(dropStack);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof WirelessDrawerBlockEntity wireless) {
            WirelessNetworkKey key = stack.get(ModDataComponents.WIRELESS_NETWORK_KEY);
            if (key != null) {
                wireless.setNetworkKey(new WirelessNetworkKey(
                        key.color1(), key.color2(), key.color3(),
                        key.isPublic(), key.owner(), wireless.getSlotCount()
                ));
            }
            stack.remove(ModDataComponents.HAS_COPIED_SETTINGS);
        }
    }
}