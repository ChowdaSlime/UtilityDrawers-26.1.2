package net.drawers.utilitydrawers.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.HashMap;
import java.util.Map;

public class WirelessNetworkSavedData extends SavedData {

    private static final Codec<NetworkItemState> ITEM_STATE_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("stacks").forGetter(s -> java.util.Arrays.asList(s.stacks)),
            Codec.LONG.listOf().fieldOf("counts").forGetter(s -> {
                java.util.List<Long> list = new java.util.ArrayList<>();
                for (long c : s.counts) list.add(c);
                return list;
            }),
            Codec.BOOL.fieldOf("locked").forGetter(s -> s.locked)
    ).apply(inst, (stacks, counts, locked) -> {
        NetworkItemState state = new NetworkItemState(stacks.size());
        for (int i = 0; i < stacks.size(); i++) {
            state.stacks[i] = stacks.get(i);
            state.counts[i] = counts.get(i);
        }
        state.locked = locked;
        return state;
    }));

    private static final Codec<NetworkFluidState> FLUID_STATE_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            FluidStack.OPTIONAL_CODEC.listOf().fieldOf("fluids").forGetter(s -> java.util.Arrays.asList(s.fluids)),
            Codec.LONG.listOf().fieldOf("amounts").forGetter(s -> {
                java.util.List<Long> list = new java.util.ArrayList<>();
                for (long a : s.amounts) list.add(a);
                return list;
            }),
            Codec.BOOL.fieldOf("locked").forGetter(s -> s.locked)
    ).apply(inst, (fluids, amounts, locked) -> {
        NetworkFluidState state = new NetworkFluidState(fluids.size());
        for (int i = 0; i < fluids.size(); i++) {
            state.fluids[i] = fluids.get(i);
            state.amounts[i] = amounts.get(i);
        }
        state.locked = locked;
        return state;
    }));

    private static final Codec<WirelessNetworkSavedData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.unboundedMap(Codec.STRING, ITEM_STATE_CODEC).fieldOf("item_networks").forGetter(d -> d.itemNetworks),
            Codec.unboundedMap(Codec.STRING, FLUID_STATE_CODEC).fieldOf("fluid_networks").forGetter(d -> d.fluidNetworks)
    ).apply(inst, (items, fluids) -> {
        WirelessNetworkSavedData data = new WirelessNetworkSavedData();
        data.itemNetworks.putAll(items);
        data.fluidNetworks.putAll(fluids);
        return data;
    }));

    public static final SavedDataType<WirelessNetworkSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("utilitydrawers", "wireless_networks"),
            WirelessNetworkSavedData::new,
            CODEC,
            null
    );

    public final Map<String, NetworkItemState> itemNetworks = new HashMap<>();
    public final Map<String, NetworkFluidState> fluidNetworks = new HashMap<>();

    public WirelessNetworkSavedData() {}

    public static WirelessNetworkSavedData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public static WirelessNetworkSavedData get(ServerLevel level) {
        return get(level.getServer());
    }

    public NetworkItemState getItemNetwork(WirelessNetworkKey key, int slotCount) {
        return itemNetworks.computeIfAbsent(key.toKey(), k -> new NetworkItemState(slotCount));
    }

    public NetworkFluidState getFluidNetwork(WirelessNetworkKey key, int slotCount) {
        return fluidNetworks.computeIfAbsent(key.toKey(), k -> new NetworkFluidState(slotCount));
    }

    public static class NetworkItemState {
        public ItemStack[] stacks;
        public long[] counts;
        public boolean locked = false;

        public NetworkItemState(int slots) {
            stacks = new ItemStack[slots];
            counts = new long[slots];
            for (int i = 0; i < slots; i++) stacks[i] = ItemStack.EMPTY;
        }
    }

    public static class NetworkFluidState {
        public FluidStack[] fluids;
        public long[] amounts;
        public boolean locked = false;

        public NetworkFluidState(int slots) {
            fluids = new FluidStack[slots];
            amounts = new long[slots];
            for (int i = 0; i < slots; i++) fluids[i] = FluidStack.EMPTY;
        }
    }
}