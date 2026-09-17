package com.pingsu.appliedember.support;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal in-memory {@link GenericInternalInventory} for unit tests. Slot contents are mutable so the
 * read-only ember view can be pointed at different states; per-slot capacity is configurable to stand
 * in for AE2's {@code GenericSlotCapacities} registration.
 */
public final class FakeGenericInventory implements GenericInternalInventory {

    private final List<GenericStack> slots;
    private final long perSlotCapacity;

    public FakeGenericInventory(int size, long perSlotCapacity, GenericStack... initial) {
        this.slots = new ArrayList<>(size);
        for (var i = 0; i < size; i++) {
            slots.add(i < initial.length ? initial[i] : null);
        }
        this.perSlotCapacity = perSlotCapacity;
    }

    @Override
    public int size() {
        return slots.size();
    }

    @Override
    public @Nullable GenericStack getStack(int slot) {
        return slots.get(slot);
    }

    @Override
    public @Nullable AEKey getKey(int slot) {
        var stack = slots.get(slot);
        return stack == null ? null : stack.what();
    }

    @Override
    public long getAmount(int slot) {
        var stack = slots.get(slot);
        return stack == null ? 0L : stack.amount();
    }

    @Override
    public long getMaxAmount(AEKey key) {
        return perSlotCapacity;
    }

    @Override
    public long getCapacity(AEKeyType keyType) {
        return perSlotCapacity;
    }

    @Override
    public boolean canInsert() {
        return true;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public void setStack(int slot, @Nullable GenericStack newStack) {
        slots.set(slot, newStack);
    }

    @Override
    public boolean isAllowed(AEKey what) {
        return true;
    }

    @Override
    public long insert(int slot, AEKey what, long amount, Actionable mode) {
        return 0;
    }

    @Override
    public long extract(int slot, AEKey what, long amount, Actionable mode) {
        return 0;
    }

    @Override
    public void beginBatch() {
    }

    @Override
    public void endBatch() {
    }

    @Override
    public void endBatchSuppressed() {
    }

    @Override
    public void onChange() {
    }
}
