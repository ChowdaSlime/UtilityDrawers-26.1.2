package net.drawers.utilitydrawers;

import net.neoforged.neoforge.common.ModConfigSpec;

public class UtilityDrawersConfig {

    public static final ModConfigSpec SPEC;

    // Drawer base stack multipliers (item/compacting/fluid drawers), keyed by slot count.
    public static final ModConfigSpec.IntValue DRAWER_MULT_1_SLOT;
    public static final ModConfigSpec.IntValue DRAWER_MULT_2_SLOT;
    public static final ModConfigSpec.IntValue DRAWER_MULT_3_SLOT;
    public static final ModConfigSpec.IntValue DRAWER_MULT_4_SLOT;

    // Fluid drawer base capacity, in mB, before slot-count multiplier and upgrades.
    public static final ModConfigSpec.IntValue FLUID_DRAWER_BASE_CAPACITY;

    // Compacting drawer base multiplier (multiplied by 64 * ratio0 * ratio1 for raw capacity).
    public static final ModConfigSpec.IntValue COMPACTING_DRAWER_BASE_MULTIPLIER;

    // Storage Interface wireless link range, in blocks, before range-upgrade multiplier.
    public static final ModConfigSpec.IntValue STORAGE_INTERFACE_BASE_RANGE;

    // How much bigger a Wireless Drawer's capacity is compared to a normal drawer of the same tier.
    public static final ModConfigSpec.IntValue WIRELESS_DRAWER_CAPACITY_MULTIPLIER;

    // Drawer upgrade item multipliers, keyed by upgrade tier (1-4).
    public static final ModConfigSpec.IntValue UPGRADE_TIER_1_MULTIPLIER;
    public static final ModConfigSpec.IntValue UPGRADE_TIER_2_MULTIPLIER;
    public static final ModConfigSpec.IntValue UPGRADE_TIER_3_MULTIPLIER;
    public static final ModConfigSpec.IntValue UPGRADE_TIER_4_MULTIPLIER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Drawer Capacity Settings",
                        "Base stack multipliers per drawer slot count. Final capacity is",
                        "multiplier * item max stack size * upgrade multiplier.")
                .push("drawer_capacity");

        DRAWER_MULT_1_SLOT = builder
                .comment("Base stack multiplier for 1-slot drawers")
                .defineInRange("drawerMultiplier1Slot", 32, 1, Integer.MAX_VALUE);

        DRAWER_MULT_2_SLOT = builder
                .comment("Base stack multiplier for 2-slot drawers")
                .defineInRange("drawerMultiplier2Slot", 16, 1, Integer.MAX_VALUE);

        DRAWER_MULT_3_SLOT = builder
                .comment("Base stack multiplier for 3-slot drawers")
                .defineInRange("drawerMultiplier3Slot", 10, 1, Integer.MAX_VALUE);

        DRAWER_MULT_4_SLOT = builder
                .comment("Base stack multiplier for 4-slot drawers")
                .defineInRange("drawerMultiplier4Slot", 8, 1, Integer.MAX_VALUE);

        builder.pop();

        builder.comment("Fluid Drawer Settings").push("fluid_drawer");

        FLUID_DRAWER_BASE_CAPACITY = builder
                .comment("Base fluid capacity (in mB) per slot-count multiplier, before upgrades")
                .defineInRange("fluidDrawerBaseCapacity", 4000, 1, Integer.MAX_VALUE);

        builder.pop();

        builder.comment("Compacting Drawer Settings").push("compacting_drawer");

        COMPACTING_DRAWER_BASE_MULTIPLIER = builder
                .comment("Base multiplier for compacting drawer raw capacity",
                        "(raw capacity = this * 64 * ratio0 * ratio1)")
                .defineInRange("compactingDrawerBaseMultiplier", 10, 1, Integer.MAX_VALUE);

        builder.pop();

        builder.comment("Storage Interface Settings").push("storage_interface");

        STORAGE_INTERFACE_BASE_RANGE = builder
                .comment("Base link range (in blocks) for the Storage Interface, before range upgrades")
                .defineInRange("storageInterfaceBaseRange", 16, 1, 512);

        builder.pop();

        builder.comment("Wireless Drawer Settings").push("wireless_drawer");

        WIRELESS_DRAWER_CAPACITY_MULTIPLIER = builder
                .comment("How much bigger a Wireless Drawer's capacity is compared to",
                        "a normal drawer with the same slot count")
                .defineInRange("wirelessDrawerCapacityMultiplier", 2, 1, Integer.MAX_VALUE);

        builder.pop();

        builder.comment("Drawer Upgrade Tier Multipliers",
                        "Multiplier applied per upgrade tier (tiers 1-4).",
                        "Stacking upgrades multiply together, same as vanilla stack multipliers.")
                .push("drawer_upgrades");

        UPGRADE_TIER_1_MULTIPLIER = builder
                .comment("Multiplier for tier 1 drawer upgrade")
                .defineInRange("upgradeTier1Multiplier", 4, 1, Integer.MAX_VALUE);

        UPGRADE_TIER_2_MULTIPLIER = builder
                .comment("Multiplier for tier 2 drawer upgrade")
                .defineInRange("upgradeTier2Multiplier", 8, 1, Integer.MAX_VALUE);

        UPGRADE_TIER_3_MULTIPLIER = builder
                .comment("Multiplier for tier 3 drawer upgrade")
                .defineInRange("upgradeTier3Multiplier", 16, 1, Integer.MAX_VALUE);

        UPGRADE_TIER_4_MULTIPLIER = builder
                .comment("Multiplier for tier 4 drawer upgrade")
                .defineInRange("upgradeTier4Multiplier", 32, 1, Integer.MAX_VALUE);

        builder.pop();

        SPEC = builder.build();
    }
}