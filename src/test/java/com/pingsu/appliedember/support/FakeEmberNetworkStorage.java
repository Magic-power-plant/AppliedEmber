package com.pingsu.appliedember.support;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * Minimal in-memory {@link MEStorage} for unit tests: holds one key (ember) up to a configurable
 * capacity, and honours {@link Actionable#SIMULATE} so the strategies' simulate/modulate flow can be
 * exercised without a level.
 */
public final class FakeEmberNetworkStorage implements MEStorage {

    private long stored;
    private long capacity;
    private int insertCalls;
    private int modulateInsertCalls;
    private int extractCalls;

    public FakeEmberNetworkStorage(long stored, long capacity) {
        this.stored = stored;
        this.capacity = capacity;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        Objects.requireNonNull(what);
        insertCalls++;
        if (mode == Actionable.MODULATE) {
            modulateInsertCalls++;
        }

        long accepted = Math.max(0L, Math.min(amount, capacity - stored));
        if (mode == Actionable.MODULATE) {
            stored += accepted;
        }
        return accepted;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        Objects.requireNonNull(what);
        extractCalls++;
        long extracted = Math.max(0L, Math.min(amount, stored));
        if (mode == Actionable.MODULATE) {
            stored -= extracted;
        }
        return extracted;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        if (stored > 0) {
            out.add(com.pingsu.appliedember.me.key.EmberKey.KEY, stored);
        }
    }

    @Override
    public Component getDescription() {
        return Component.literal("fake ember network storage");
    }

    public long stored() {
        return stored;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public int modulateInsertCalls() {
        return modulateInsertCalls;
    }

    public int insertCalls() {
        return insertCalls;
    }

    public int extractCalls() {
        return extractCalls;
    }
}
