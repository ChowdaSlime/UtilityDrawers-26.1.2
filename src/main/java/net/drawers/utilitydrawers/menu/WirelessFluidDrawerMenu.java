package net.drawers.utilitydrawers.menu;

import net.drawers.utilitydrawers.block.entity.WirelessFluidDrawerBlockEntity;
import net.drawers.utilitydrawers.data.WirelessNetworkKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;
import java.util.UUID;

public class WirelessFluidDrawerMenu extends FluidDrawerMenu {

    private final ContainerData networkData;
    private final WirelessFluidDrawerBlockEntity wirelessEntity;

    public WirelessFluidDrawerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, playerInventory.player.level()
                .getBlockEntity(buf.readBlockPos()));
    }

    public WirelessFluidDrawerMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        super(ModMenuTypes.WIRELESS_FLUID_DRAWER_MENU.get(), containerId, playerInventory, blockEntity, false);
        this.wirelessEntity = (WirelessFluidDrawerBlockEntity) blockEntity;

        WirelessNetworkKey key = this.wirelessEntity.getNetworkKey();

        int idx1 = colorToIndex(key.color1());
        int idx2 = colorToIndex(key.color2());
        int idx3 = colorToIndex(key.color3());
        int pub  = key.isPublic() ? 1 : 0;

        this.networkData = new SimpleContainerData(4);
        this.networkData.set(0, idx1);
        this.networkData.set(1, idx2);
        this.networkData.set(2, idx3);
        this.networkData.set(3, pub);

        addDataSlots(this.networkData);
    }

    public void cycleColor(int colorSlot, boolean reverse) {
        int current = networkData.get(colorSlot);
        int next;

        if (reverse) {
            next = (current - 1 + WirelessDrawerMenu.NETWORK_COLORS.length) % WirelessDrawerMenu.NETWORK_COLORS.length;
        } else {
            next = (current + 1) % WirelessDrawerMenu.NETWORK_COLORS.length;
        }

        networkData.set(colorSlot, next);
    }

    public void togglePublic() {
        int current = networkData.get(3);
        networkData.set(3, current == 1 ? 0 : 1);
    }

    public int getColorIndex(int slot) {
        return networkData.get(slot);
    }

    public boolean isPublic() {
        return networkData.get(3) == 1;
    }

    public BlockPos getBlockPos() {
        return wirelessEntity.getBlockPos();
    }

    public WirelessNetworkKey buildCurrentKey() {
        int c1 = WirelessDrawerMenu.NETWORK_COLORS[networkData.get(0)];
        int c2 = WirelessDrawerMenu.NETWORK_COLORS[networkData.get(1)];
        int c3 = WirelessDrawerMenu.NETWORK_COLORS[networkData.get(2)];
        boolean pub = networkData.get(3) == 1;

        Optional<UUID> owner = pub
                ? Optional.empty()
                : wirelessEntity.getNetworkKey().owner();

        return new WirelessNetworkKey(c1, c2, c3, pub, owner, getDrawerSlotCount());
    }

    private static int colorToIndex(int color) {
        for (int i = 0; i < WirelessDrawerMenu.NETWORK_COLORS.length; i++) {
            if (WirelessDrawerMenu.NETWORK_COLORS[i] == color) return i;
        }
        return 0;
    }

    public void refreshFromBlockEntity() {
        WirelessNetworkKey key = wirelessEntity.getNetworkKey();
        networkData.set(0, colorToIndex(key.color1()));
        networkData.set(1, colorToIndex(key.color2()));
        networkData.set(2, colorToIndex(key.color3()));
        networkData.set(3, key.isPublic() ? 1 : 0);
    }
}