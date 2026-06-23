package net.drawers.utilitydrawers.block;

import net.drawers.utilitydrawers.block.entity.StorageInterfaceBlockEntity;
import net.drawers.utilitydrawers.item.DrawerUpgradeItem;
import net.drawers.utilitydrawers.item.StorageRemoteItem;
import net.drawers.utilitydrawers.menu.StorageInterfaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageInterfaceBlock extends Block implements EntityBlock {

    private static final Map<UUID, Long> LAST_CLICK_TIME = new HashMap<>();
    public static final BooleanProperty LOCKED = BlockStateProperties.LOCKED;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

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
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (level.getBlockEntity(pos) instanceof StorageInterfaceBlockEntity interfaceEntity) {
            interfaceEntity.unlinkAllDrawers();
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || stack.getItem() instanceof DrawerUpgradeItem) {
            return openMenu(level, pos, player);
        }
        return handleInteraction(level, pos, player, stack, hand);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openMenu(level, pos, player);
    }

    private InteractionResult openMenu(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;
        if (!(level.getBlockEntity(pos) instanceof StorageInterfaceBlockEntity interfaceEntity)) return InteractionResult.FAIL;

        serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new StorageInterfaceMenu(id, inv, interfaceEntity),
                Component.translatable("block.utilitydrawers.storage_interface")
        ), buf -> buf.writeBlockPos(pos));

        return InteractionResult.CONSUME;
    }

    private InteractionResult handleInteraction(Level level, BlockPos pos, Player player, ItemStack handStack, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (handStack.getItem() instanceof StorageRemoteItem) {
            return InteractionResult.PASS;
        }

        BlockState currentState = level.getBlockState(pos);


        if (level.getBlockEntity(pos) instanceof StorageInterfaceBlockEntity interfaceEntity) {
            long currentTime = System.currentTimeMillis();
            long lastClick = LAST_CLICK_TIME.getOrDefault(player.getUUID(), 0L);
            boolean isDoubleClick = (currentTime - lastClick) < 300;
            LAST_CLICK_TIME.put(player.getUUID(), currentTime);

            boolean isFluidContainer = false;
            if (!handStack.isEmpty()) {
                ItemAccess access = ItemAccess.forStack(handStack).oneByOne();
                ResourceHandler<FluidResource> fluidHandler = access.getCapability(Capabilities.Fluid.ITEM);
                if (fluidHandler != null) {
                    try (Transaction tx = Transaction.openRoot()) {
                        int simDrain = fluidHandler.extract(FluidResource.EMPTY, Integer.MAX_VALUE, tx);
                        if (simDrain > 0) {
                            isFluidContainer = true;
                        }
                    }
                }
            }

            if (isDoubleClick && !isFluidContainer) {
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
                ItemAccess access = ItemAccess.forPlayerInteraction(player, hand).oneByOne();
                ResourceHandler<FluidResource> fluidHandler = access.getCapability(Capabilities.Fluid.ITEM);
                if (fluidHandler != null) {
                    FluidResource resource = null;
                    int totalAmount = 0;
                    for (int i = 0; i < fluidHandler.size(); i++) {
                        FluidResource r = fluidHandler.getResource(i);
                        if (!r.isEmpty()) {
                            resource = r;
                            totalAmount += fluidHandler.getAmountAsInt(i);
                        }
                    }

                    if (resource != null && totalAmount > 0) {
                        FluidStack toInsert = resource.toStack(totalAmount);
                        FluidStack leftover = interfaceEntity.insertFluidIntoNetwork(toInsert);
                        int inserted = totalAmount - leftover.getAmount();
                        if (inserted > 0) {
                            FluidResource res = resource;
                            int toExtract = inserted;
                            try (Transaction tx = Transaction.openRoot()) {
                                fluidHandler.extract(res, toExtract, tx);
                                tx.commit();
                            }
                            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
                            return InteractionResult.CONSUME;
                        }
                    }
                }

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