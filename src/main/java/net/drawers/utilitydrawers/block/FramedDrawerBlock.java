package net.drawers.utilitydrawers.block;

import net.drawers.utilitydrawers.block.entity.DrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.FramedDrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.SlotCountProvider;
import net.drawers.utilitydrawers.item.DrawerUpgradeItem;
import net.drawers.utilitydrawers.item.StorageRemoteItem;
import net.drawers.utilitydrawers.item.VoidUpgradeItem;
import net.drawers.utilitydrawers.menu.DrawerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FramedDrawerBlock extends Block implements SlotCountProvider, EntityBlock {

    public static final EnumProperty<Direction> FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    private final int slotCount;

    private static final Map<java.util.UUID, Long> LAST_CLICK_TIME =
            new HashMap<>();

    public FramedDrawerBlock(Properties properties, int slotCount) {
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
        return new FramedDrawerBlockEntity(pos, state);
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

        if (handStack.getItem() instanceof StorageRemoteItem) {
            return InteractionResult.PASS;
        }

        if (!handStack.isEmpty() && handStack.getItem() instanceof DrawerUpgradeItem || handStack.getItem() instanceof VoidUpgradeItem) {
            if (level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer) {
                if (drawer.insertUpgrade(handStack)) {
                    if (!level.isClientSide() && !player.isCreative()) {
                        handStack.shrink(1);
                    }
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);

                    return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
                }
            }
        }

        if (hit.getDirection() != state.getValue(FACING)) {
            if (!level.isClientSide()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof DrawerBlockEntity && player instanceof ServerPlayer serverPlayer) {
                    Component title = Component.literal("Drawer");
                    serverPlayer.openMenu(new MenuProvider() {
                        @Override
                        public Component getDisplayName() {
                            return title;
                        }

                        @Override
                        public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                            return new DrawerMenu(id, inv, be);
                        }
                    }, buf -> buf.writeBlockPos(pos));
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer) {
            long currentTime = System.currentTimeMillis();
            long lastClick = LAST_CLICK_TIME.getOrDefault(player.getUUID(), 0L);
            boolean isDoubleClick = (currentTime - lastClick) < 300;
            LAST_CLICK_TIME.put(player.getUUID(), currentTime);

            int targetSlot = getTargetSlot(hit.getLocation(), state, drawer.getSlotCount());

            if (targetSlot < 0) {
                if (player instanceof ServerPlayer serverPlayer) {
                    Component title = Component.literal("Drawer");
                    serverPlayer.openMenu(new MenuProvider() {

                        @Override
                        public Component getDisplayName() {
                            return title;
                        }

                        @Override
                        public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                            return new DrawerMenu(id, inv, drawer);
                        }
                    }, buf -> buf.writeBlockPos(pos));
                }
                return InteractionResult.SUCCESS;
            }

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

            } else if (!handStack.isEmpty()) {
                if (drawer.isLocked() && !drawer.hasTemplate(targetSlot)) {
                    drawer.setTemplate(targetSlot, handStack);
                }
                ItemStack remainder = drawer.insertItemIntoSlot(targetSlot, handStack, false);
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof FramedDrawerBlockEntity be) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                be.loadContentsFromTag(customData.copyTag());
                be.requestModelDataUpdate();
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
    }

    public int getTargetSlot(Vec3 hitPos, BlockState state, int slotCount) {
        Direction facing = state.getValue(FACING);

        double u, v;
        switch (facing) {
            case NORTH -> {
                u = 1.0 - (hitPos.x - Math.floor(hitPos.x));
                v = 1.0 - (hitPos.y - Math.floor(hitPos.y));
            }
            case SOUTH -> {
                u = hitPos.x - Math.floor(hitPos.x);
                v = 1.0 - (hitPos.y - Math.floor(hitPos.y));
            }
            case WEST -> {
                u = hitPos.z - Math.floor(hitPos.z);
                v = 1.0 - (hitPos.y - Math.floor(hitPos.y));
            }
            case EAST -> {
                u = 1.0 - (hitPos.z - Math.floor(hitPos.z));
                v = 1.0 - (hitPos.y - Math.floor(hitPos.y));
            }
            default -> {
                return 0;
            }
        }

        final double EDGE = 1.0 / 16.0;
        final double TRIM = 1.0 / 16.0;

        double halfLow = 0.5 - TRIM / 2.0;
        double halfHigh = 0.5 + TRIM / 2.0;

        boolean inTopZone = v >= EDGE && v < halfLow;
        boolean inBottomZone = v > halfHigh && v <= (1.0 - EDGE);
        boolean inLeftZone = u >= EDGE && u < halfLow;
        boolean inRightZone = u > halfHigh && u <= (1.0 - EDGE);
        boolean inUEdge = u >= EDGE && u <= (1.0 - EDGE);

        return switch (slotCount) {
            case 1 -> {
                boolean inActive = u >= EDGE && u <= (1.0 - EDGE) && v >= EDGE && v <= (1.0 - EDGE);
                yield inActive ? 0 : -1;
            }
            case 2 -> {
                if (inTopZone && inUEdge) yield 0;
                if (inBottomZone && inUEdge) yield 1;
                yield -1;
            }
            case 3 -> {
                boolean inFullWidthTop = inTopZone && u >= EDGE && u <= (1.0 - EDGE);
                if (inFullWidthTop) yield 0;
                if (inBottomZone && inLeftZone) yield 1;
                if (inBottomZone && inRightZone) yield 2;
                yield -1;
            }
            case 4 -> {
                if (inTopZone && inLeftZone) yield 0;
                if (inTopZone && inRightZone) yield 1;
                if (inBottomZone && inLeftZone) yield 2;
                if (inBottomZone && inRightZone) yield 3;
                yield -1;
            }
            default -> 0;
        };
    }

}