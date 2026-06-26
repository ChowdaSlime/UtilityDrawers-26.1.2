package net.drawers.utilitydrawers.block.entity;

import net.drawers.utilitydrawers.block.FramedCompactingDrawerBlock;
import net.drawers.utilitydrawers.block.FramedDrawerBlock;
import net.drawers.utilitydrawers.block.FramedFluidDrawerBlock;
import net.drawers.utilitydrawers.item.DrawerUpgradeItem;
import net.drawers.utilitydrawers.menu.DrawerFramerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class DrawerFramerBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int SLOT_SIDES   = 0;
    public static final int SLOT_FACE    = 1;
    public static final int SLOT_INPUT   = 2;
    public static final int SLOT_OUTPUT  = 3;
    public static final int SLOT_UPGRADE = 4;

    private static final int BASE_PROCESS_TICKS = 100; // 5 seconds

    private final ItemStack[] inventory = new ItemStack[]{
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY
    };

    private int progress = 0;
    private int maxProgress = BASE_PROCESS_TICKS;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> DrawerFramerBlockEntity.this.progress;
                case 1 -> DrawerFramerBlockEntity.this.maxProgress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> DrawerFramerBlockEntity.this.progress = value;
                case 1 -> DrawerFramerBlockEntity.this.maxProgress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public DrawerFramerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRAWER_FRAMER_BLOCK_ENTITY.get(), pos, state);
    }

    public ItemStack getStackInSlot(int slot) {
        return inventory[slot];
    }

    public void setSlot(int slot, ItemStack stack) {
        inventory[slot] = stack;
        setChanged();
    }

    public ItemStack removeSlot(int slot, int count) {
        ItemStack stack = inventory[slot];
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack taken = stack.copyWithCount(Math.min(count, stack.getCount()));
        stack.shrink(taken.getCount());
        if (stack.isEmpty()) inventory[slot] = ItemStack.EMPTY;
        setChanged();
        return taken;
    }

    public int getProgress()    { return progress; }
    public int getMaxProgress() { return maxProgress; }

    public boolean canProcess() {
        if (inventory[SLOT_SIDES].isEmpty())  return false;
        if (inventory[SLOT_FACE].isEmpty())   return false;
        if (inventory[SLOT_INPUT].isEmpty())  return false;
        if (!(inventory[SLOT_SIDES].getItem() instanceof BlockItem)) return false;
        if (!(inventory[SLOT_FACE].getItem()  instanceof BlockItem)) return false;
        if (!isFramedDrawer(inventory[SLOT_INPUT])) return false;
        if (!inventory[SLOT_OUTPUT].isEmpty()) return false;

        return true;
    }

    private boolean isFramedDrawer(ItemStack stack) {
        return stack.getItem() instanceof BlockItem bi &&
                (bi.getBlock() instanceof FramedDrawerBlock ||
                        bi.getBlock() instanceof FramedFluidDrawerBlock ||
                        bi.getBlock() instanceof FramedCompactingDrawerBlock);
    }

    public void tick() {
        if (level == null || level.isClientSide()) return;

        maxProgress = calculateMaxProgress();

        if (!canProcess()) {
            if (progress > 0) {
                progress = 0;
                setChanged();
            }
            return;
        }

        progress++;
        setChanged();

        if (progress >= maxProgress) {
            processOutput();
            progress = 0;
            setChanged();
        }
    }

    private int calculateMaxProgress() {
        if (inventory[SLOT_UPGRADE].isEmpty()) return BASE_PROCESS_TICKS;
        if (!(inventory[SLOT_UPGRADE].getItem() instanceof DrawerUpgradeItem upgrade)) return BASE_PROCESS_TICKS;
        int multiplier = upgrade.getMultiplier();
        return Math.max(1, BASE_PROCESS_TICKS / multiplier);
    }

    private void processOutput() {
        BlockState sidesState = ((BlockItem) inventory[SLOT_SIDES].getItem()).getBlock().defaultBlockState();
        BlockState faceState  = ((BlockItem) inventory[SLOT_FACE].getItem()).getBlock().defaultBlockState();
        ItemStack result = inventory[SLOT_INPUT].getItem().getDefaultInstance();
        result.setCount(1);
        inventory[SLOT_INPUT].shrink(1);
        if (inventory[SLOT_INPUT].isEmpty()) inventory[SLOT_INPUT] = ItemStack.EMPTY;
        inventory[SLOT_SIDES].shrink(1);
        if (inventory[SLOT_SIDES].isEmpty()) inventory[SLOT_SIDES] = ItemStack.EMPTY;
        inventory[SLOT_FACE].shrink(1);
        if (inventory[SLOT_FACE].isEmpty()) inventory[SLOT_FACE] = ItemStack.EMPTY;
        CompoundTag tag = new CompoundTag();
        if (level != null) {
            var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            tag.put("FrameState", BlockState.CODEC.encodeStart(ops, sidesState).getOrThrow());
            tag.put("FaceState",  BlockState.CODEC.encodeStart(ops, faceState).getOrThrow());
        }

        result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        inventory[SLOT_OUTPUT] = result;

        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Progress", progress);
        for (int i = 0; i < inventory.length; i++) {
            if (!inventory[i].isEmpty())
                output.child("Slot" + i).store("Item", ItemStack.CODEC, inventory[i]);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getIntOr("Progress", 0);
        for (int i = 0; i < inventory.length; i++) {
            final int idx = i;
            inventory[idx] = ItemStack.EMPTY;
            input.child("Slot" + idx).ifPresent(s ->
                    inventory[idx] = s.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Progress", progress);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ValueInput input) {
        super.onDataPacket(connection, input);
        if (level != null && level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public int getContainerSize() {
        return inventory.length;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory[slot];
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return removeSlot(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = inventory[slot];
        inventory[slot] = ItemStack.EMPTY;
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory[slot] = stack;
        setChanged();
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < inventory.length; i++) {
            inventory[i] = ItemStack.EMPTY;
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Drawer Framer");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory playerInventory,
            Player player
    ) {
        return new DrawerFramerMenu(containerId, playerInventory, this, this.dataAccess);
    }
}