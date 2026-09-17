package com.pingsu.appliedember.content.ember;

import appeng.api.config.Actionable;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.StorageHelper;
import com.pingsu.appliedember.me.key.EmberKey;
import com.pingsu.appliedember.me.storage.EmberStorageAmounts;
import com.rekindled.embers.power.DefaultEmberCapability;

import java.util.function.BooleanSupplier;

/**
 * Embers capability that forwards ember reads and writes to the ME network inventory.
 *
 * <p>Plan 4.3: the cached snapshot used while saving or while the grid is unavailable is now
 * refreshed by the explicit {@link #refreshCache()} only. Previously {@code getEmberCapacity()} did
 * that work on the side, so a plain read could mark the block entity dirty — the caller in
 * {@code MEEmberCellEntity} had to call a getter purely for its side effect.
 */
final class GridBackedEmberCapability extends DefaultEmberCapability {

    /**
     * "All of it" sentinel for the grid insert/extract simulations, and the pseudo-unbounded capacity
     * reported to Embers machines (plan 4.4).
     *
     * <p>{@link Integer#MAX_VALUE} rather than {@code Long.MAX_VALUE} on purpose: ember amounts travel
     * as {@code double}, and only the former round-trips through {@code double} exactly. The capacity
     * is capped by the same value so the two stay consistent.
     */
    private static final long ALL_EMBER = Integer.MAX_VALUE;

    private final IManagedGridNode mainNode;
    private final IActionSource actionSource;
    private final BooleanSupplier saving;
    private final Runnable changedCallback;

    GridBackedEmberCapability(
            IManagedGridNode mainNode,
            IActionSource actionSource,
            BooleanSupplier saving,
            Runnable changedCallback
    ) {
        this.mainNode = mainNode;
        this.actionSource = actionSource;
        this.saving = saving;
        this.changedCallback = changedCallback;
        // Embers machines expect a capacity number; the grid decides the real limit, so report a
        // huge-but-exact value instead of "0" while the grid is not yet connected.
        setEmberCapacity(ALL_EMBER);
    }

    @Override
    public double addAmount(double value, boolean doAdd) {
        if (!isGridActive()) {
            return 0.0D;
        }

        var grid = mainNode.getGrid();
        return (double) StorageHelper.poweredInsert(
                grid.getEnergyService(),
                grid.getStorageService().getInventory(),
                EmberKey.KEY,
                EmberStorageAmounts.saturatingLong(value),
                actionSource,
                doAdd ? Actionable.MODULATE : Actionable.SIMULATE
        );
    }

    @Override
    public double removeAmount(double value, boolean doRemove) {
        if (!isGridActive()) {
            return 0.0D;
        }

        var grid = mainNode.getGrid();
        return (double) StorageHelper.poweredExtraction(
                grid.getEnergyService(),
                grid.getStorageService().getInventory(),
                EmberKey.KEY,
                EmberStorageAmounts.saturatingLong(value),
                actionSource,
                doRemove ? Actionable.MODULATE : Actionable.SIMULATE
        );
    }

    @Override
    public double getEmber() {
        var grid = mainNode.getGrid();
        if (grid == null || saving.getAsBoolean()) {
            // No grid to ask (unloaded, or the block entity is being serialized): fall back to the
            // last refreshed snapshot.
            return super.getEmber();
        }
        if (!mainNode.isActive()) {
            return 0.0D;
        }
        return (double) grid.getStorageService()
                .getInventory()
                .extract(EmberKey.KEY, ALL_EMBER, Actionable.SIMULATE, actionSource);
    }

    @Override
    public double getEmberCapacity() {
        var grid = mainNode.getGrid();
        if (grid == null || saving.getAsBoolean()) {
            return super.getEmberCapacity();
        }

        if (!mainNode.isActive()) {
            return 0.0D;
        }

        EmberSnapshot snapshot = snapshot();
        long capacity = snapshot.stored() + snapshot.free();
        return capacity > ALL_EMBER ? (double) ALL_EMBER : (double) capacity;
    }

    /**
     * Refreshes the cached ember amount that {@link #getEmber()} and {@link #getEmberCapacity()} fall
     * back to when the grid is unavailable, and notifies the block entity if the value changed.
     *
     * <p>Explicit by design (plan 4.3): reading a capability value must not mutate the block entity.
     * Call this from grid state changes and before the value is persisted.
     */
    void refreshCache() {
        var grid = mainNode.getGrid();
        if (grid == null) {
            return;
        }

        if (!mainNode.isActive()) {
            updateCachedEmber(0L);
            return;
        }

        updateCachedEmber(snapshot().stored());
    }

    EmberSnapshot snapshot() {
        var grid = mainNode.getGrid();
        if (grid == null || !mainNode.isActive()) {
            long stored = EmberStorageAmounts.saturatingLong(getEmber());
            long free = Math.max(0L, EmberStorageAmounts.saturatingLong(getEmberCapacity()) - stored);
            return new EmberSnapshot(stored, free);
        }

        var storage = grid.getStorageService().getInventory();
        long stored = storage.extract(EmberKey.KEY, ALL_EMBER, Actionable.SIMULATE, actionSource);
        long free = storage.insert(EmberKey.KEY, ALL_EMBER, Actionable.SIMULATE, actionSource);
        return new EmberSnapshot(stored, free);
    }

    /**
     * Whether the backing grid node is connected and active.
     *
     * <p>Plan 4.1: single implementation, shared with {@link MEEmberCellEntity} instead of being
     * duplicated verbatim in both classes.
     */
    boolean isGridActive() {
        return mainNode.getGrid() != null && mainNode.isActive();
    }
    private void updateCachedEmber(long stored) {
        if (super.getEmber() != (double) stored) {
            super.setEmber((double) stored);
            changedCallback.run();
        }
    }

    record EmberSnapshot(long stored, long free) {
    }
}
