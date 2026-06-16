package net.drawers.utilitydrawers.block;

import net.drawers.utilitydrawers.block.entity.StorageInterfaceBlockEntity;
import net.drawers.utilitydrawers.item.StorageRemoteItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageInterfaceBlock extends Block implements EntityBlock {

    private static final Map<UUID, Long> LAST_CLICK_TIME = new HashMap<>();
    public static final BooleanProperty LOCKED = BlockStateProperties.LOCKED;
    public static final EnumProperty<Direction> FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    public StorageInterfaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LOCKED, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LOCKED, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(LOCKED, false).setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StorageInterfaceBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return handleInteraction(level, pos, player, stack, hand);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return handleInteraction(level, pos, player, ItemStack.EMPTY, InteractionHand.MAIN_HAND);
    }

    private InteractionResult handleInteraction(Level level, BlockPos pos, Player player, ItemStack handStack, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (handStack.getItem() instanceof StorageRemoteItem) {
            return InteractionResult.PASS;
        }

        BlockState currentState = level.getBlockState(pos);

        if (handStack.is(net.minecraft.world.item.Items.TRIPWIRE_HOOK)) {
            boolean isCurrentlyLocked = currentState.getValue(LOCKED);
            boolean newLockedState = !isCurrentlyLocked;

            level.setBlock(pos, currentState.setValue(LOCKED, newLockedState), 3);

            if (level.getBlockEntity(pos) instanceof StorageInterfaceBlockEntity interfaceEntity) {
                interfaceEntity.toggleNetworkLock(newLockedState);
            }

            level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5f, 1.0f);
            return InteractionResult.CONSUME;
        }

        if (level.getBlockEntity(pos) instanceof StorageInterfaceBlockEntity interfaceEntity) {
            long currentTime = System.currentTimeMillis();
            long lastClick = LAST_CLICK_TIME.getOrDefault(player.getUUID(), 0L);
            boolean isDoubleClick = (currentTime - lastClick) < 300;
            LAST_CLICK_TIME.put(player.getUUID(), currentTime);

            if (isDoubleClick) {
                boolean insertedAny = false;

                for (int j = 0; j < 36; j++) {
                    ItemStack invStack = player.getInventory().getItem(j);
                    if (invStack.isEmpty()) continue;

                    int startingCount = invStack.getCount();

                    ItemStack remainder = interfaceEntity.insertIntoNetwork(invStack);

                    if (remainder.getCount() < startingCount) {
                        player.getInventory().setItem(j, remainder);
                        insertedAny = true;
                    }
                }

                if (insertedAny) {
                    player.inventoryMenu.broadcastChanges();
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2f, 1.0f);
                }
                return InteractionResult.CONSUME;

            } else if (!handStack.isEmpty()) {
                ItemStack remainder = interfaceEntity.insertIntoNetwork(handStack);
                if (remainder.getCount() != handStack.getCount()) {
                    player.setItemInHand(hand, remainder);
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2f, 1.0f);
                    return InteractionResult.CONSUME;
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

}