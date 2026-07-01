package net.drawers.utilitydrawers.block;

import net.drawers.utilitydrawers.block.entity.WirelessFluidDrawerBlockEntity;
import net.drawers.utilitydrawers.data.ModDataComponents;
import net.drawers.utilitydrawers.data.WirelessNetworkKey;
import net.drawers.utilitydrawers.item.StorageRemoteItem;
import net.drawers.utilitydrawers.item.WirelessFluidDrawerBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;

public class WirelessFluidDrawerBlock extends FluidDrawerBlock {

    public WirelessFluidDrawerBlock(BlockBehaviour.Properties properties, int slotCount) {
        super(properties, slotCount);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WirelessFluidDrawerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return handleWirelessInteraction(state, level, pos, player, hit, stack, hand);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return handleWirelessInteraction(state, level, pos, player, hit, ItemStack.EMPTY, InteractionHand.MAIN_HAND);
    }

    private InteractionResult handleWirelessInteraction(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit, ItemStack handStack, InteractionHand hand) {
        if (handStack.getItem() instanceof StorageRemoteItem) {
            return InteractionResult.PASS;
        }

        if (handStack.getItem() instanceof WirelessFluidDrawerBlockItem) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (!(level.getBlockEntity(pos) instanceof WirelessFluidDrawerBlockEntity drawer)) {
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

        if (!handStack.isEmpty()) {
            ItemAccess access = ItemAccess.forPlayerInteraction(player, hand).oneByOne();
            ResourceHandler<FluidResource> fluidHandler = access.getCapability(Capabilities.Fluid.ITEM);

            if (fluidHandler != null) {
                boolean interacted = false;

                FluidResource heldResource = null;
                int heldAmount = 0;
                for (int i = 0; i < fluidHandler.size(); i++) {
                    FluidResource r = fluidHandler.getResource(i);
                    if (!r.isEmpty()) {
                        heldResource = r;
                        heldAmount += fluidHandler.getAmountAsInt(i);
                    }
                }

                if (heldResource != null && heldAmount > 0) {
                    FluidStack simInsert = heldResource.toStack(heldAmount);

                    if (drawer.isLocked() && !drawer.hasTemplate(targetSlot)) {
                        drawer.setTemplate(targetSlot, simInsert);
                    }

                    FluidStack remainder = drawer.insertFluidIntoSlot(targetSlot, simInsert, true);
                    int amountToDrain = heldAmount - remainder.getAmount();

                    if (amountToDrain > 0) {
                        FluidResource res = heldResource;
                        try (Transaction tx = Transaction.openRoot()) {
                            int drained = fluidHandler.extract(res, amountToDrain, tx);
                            if (drained > 0) {
                                drawer.insertFluidIntoSlot(targetSlot, res.toStack(drained), false);
                                tx.commit();
                                interacted = true;
                            }
                        }
                        if (interacted) {
                            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
                        }
                    }
                }

                if (!interacted && !drawer.isSlotEmpty(targetSlot)) {
                    FluidStack storedFluid = drawer.getStoredFluid(targetSlot);
                    long storedAmount = drawer.getStoredAmount(targetSlot);

                    if (storedAmount > 0) {
                        FluidResource storedResource = FluidResource.of(storedFluid);
                        int maxExtractable = (int) Math.min(Integer.MAX_VALUE, storedAmount);

                        try (Transaction tx = Transaction.openRoot()) {
                            int simFill = fluidHandler.insert(storedResource, maxExtractable, tx);
                            if (simFill > 0) {
                                FluidStack actuallyExtracted = drawer.extractFluid(targetSlot, simFill, false);
                                if (actuallyExtracted.getAmount() > 0) {
                                    tx.commit();
                                    interacted = true;
                                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BUCKET_FILL, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
                                }
                            }
                        }
                    }
                }

                if (interacted) {
                    return InteractionResult.CONSUME;
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack dropStack = new ItemStack(this);
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof WirelessFluidDrawerBlockEntity wireless) {
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
        if (be instanceof WirelessFluidDrawerBlockEntity wireless) {
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