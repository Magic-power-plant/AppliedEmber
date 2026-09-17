package com.pingsu.appliedember.me.key;

import appeng.api.behaviors.GenericSlotCapacities;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import com.pingsu.appliedember.AppliedEmberResources;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.Nullable;

public class EmberKeyType extends AEKeyType {

    /** Display name of the ember channel (plan 4.2: renamed from the constant {@code Ember}). */
    public static final Component EMBER_NAME = Component.translatable("aekey.appliedember.ember");

    public static final AEKeyType TYPE = new EmberKeyType();

    /**
     * Ember is a single, NBT-less key, so the two channel scaling factors are the only knobs that
     * decide how much Ember fits in a cell and how much one bus operation moves.
     *
     * <p>Both are 1000 for the same reason fluids use 8000 per byte: Embers machines work in whole
     * ember units at roughly single-digit to low-hundreds ember per tick, and AE2's storage math is
     * {@code long}-based. 1000 ember per AE byte puts a 1k cell at
     * {@code (1 * 1024 - 8 * 1) * 1000 = 1_016_000} ember, i.e. within an order of magnitude of an
     * AE2 1k item cell's 1016 stacks, and 1000 ember per operation makes a bus move a comparable
     * amount per operation to the item/fluid channels (plan 4.4).
     */
    private static final int EMBER_PER_BYTE = 1000;
    private static final int EMBER_PER_OPERATION = 1000;

    /**
     * How much ember one ME interface / pattern provider slot holds, registered through AE2's public
     * {@link appeng.api.behaviors.GenericSlotCapacities} extension point. Without it AE2 falls back to
     * {@code Long.MAX_VALUE} per slot for unknown key types, i.e. an interface slot could report an
     * effectively unbounded ember capacity. Mirrors the fluid default of 4 buckets per slot.
     */
    public static final long EMBER_PER_SLOT = 4L * EMBER_PER_BYTE;

    private EmberKeyType() {
        super(AppliedEmberResources.id("ember"), EmberKey.class, EMBER_NAME);
    }

    /**
     * Registers the ember channel with AE2.
     *
     * <p>Timing (plan R4): AE2's key type registry only exists after AE2's own {@code NewRegistryEvent},
     * so this cannot run from the mod constructor (it would trip AE2's "AE2 isn't initialized yet"
     * check). Hooking a registry event instead is exactly what AE2 itself does — it registers its item
     * and fluid channels from its {@code Registries.BLOCK} register listener — and it is the earliest
     * point where the registry is guaranteed to exist. No world can be loaded (and therefore no key
     * deserialized) during the registry phase, so the "before any key deserialization" requirement
     * holds by construction rather than by an implicit convention.
     */
    public static void register(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.BLOCK)) {
            GenericSlotCapacities.register(TYPE, EMBER_PER_SLOT);
            AEKeyTypes.register(TYPE);
        }
    }

    @Nullable
    @Override
    public AEKey readFromPacket(FriendlyByteBuf friendlyByteBuf) {
        return EmberKey.KEY;
    }

    @Nullable
    @Override
    public AEKey loadKeyFromTag(CompoundTag compoundTag) {
        return EmberKey.KEY;
    }

    @Override
    public int getAmountPerByte() {
        return EMBER_PER_BYTE;
    }

    @Override
    public int getAmountPerOperation() {
        return EMBER_PER_OPERATION;
    }
}
