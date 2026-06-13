package net.drawers.utilitydrawers.block;

import net.drawers.utilitydrawers.block.entity.DrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.SlotCountProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DrawerBlock extends Block implements SlotCountProvider, EntityBlock {

    public static final EnumProperty<Direction> FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    private final int slotCount;

    private static final Map<java.util.UUID, Long> LAST_CLICK_TIME =
            new HashMap<>();

    public DrawerBlock(BlockBehaviour.Properties properties, int slotCount) {
        super(properties);
        this.slotCount = slotCount;
        this.registerDefaultState(
                this.stateDefinition.any().setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public int getSlotCount() {
        return slotCount;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DrawerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {

        return handleInteraction(
                state, level, pos, player, hit, stack, hand
        );
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {

        return handleInteraction(
                state,
                level,
                pos,
                player,
                hit,
                ItemStack.EMPTY,
                InteractionHand.MAIN_HAND
        );
    }

    private InteractionResult handleInteraction(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit,
            ItemStack handStack,
            InteractionHand hand) {

        if (hit.getDirection() != state.getValue(FACING)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer) {

            long currentTime = System.currentTimeMillis();
            long lastClick =
                    LAST_CLICK_TIME.getOrDefault(player.getUUID(), 0L);

            boolean isDoubleClick =
                    (currentTime - lastClick) < 300;

            LAST_CLICK_TIME.put(player.getUUID(), currentTime);

            if (isDoubleClick) {

                boolean insertedAny = false;

                for (int j = 0; j < 36; j++) {

                    ItemStack invStack =
                            player.getInventory().getItem(j);

                    if (invStack.isEmpty()) continue;

                    boolean matchesExisting = false;

                    for (int i = 0; i < drawer.getSlotCount(); i++) {

                        ItemStack storedStack =
                                drawer.getStoredItem(i);

                        if (!storedStack.isEmpty()
                                && ItemStack.isSameItem(storedStack, invStack)) {

                            matchesExisting = true;
                            break;
                        }
                    }

                    if (matchesExisting) {

                        int startingCount =
                                invStack.getCount();

                        ItemStack remainder =
                                drawer.insertItem(invStack, false);

                        if (remainder.getCount() != startingCount) {

                            player.getInventory()
                                    .setItem(j, remainder);

                            insertedAny = true;
                        }
                    }
                }

                if (insertedAny) {

                    player.inventoryMenu.broadcastChanges();

                    level.playSound(
                            null,
                            pos,
                            net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                            net.minecraft.sounds.SoundSource.BLOCKS,
                            0.2f,
                            1.0f
                    );
                }

                return InteractionResult.CONSUME;
            } else if (!handStack.isEmpty()) {

                ItemStack remainder =
                        drawer.insertItem(handStack, false);

                if (remainder.getCount() != handStack.getCount()) {

                    player.setItemInHand(hand, remainder);

                    return InteractionResult.CONSUME;
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack dropStack = new ItemStack(this);
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        if (blockEntity instanceof DrawerBlockEntity drawerEntity) {
            CompoundTag tag = drawerEntity.saveDrawerData(builder.getLevel().registryAccess());

            dropStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return List.of(dropStack);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof DrawerBlockEntity drawerEntity) {
                drawerEntity.loadContentsFromTag(customData.copyTag());
            }
        }
    }
}