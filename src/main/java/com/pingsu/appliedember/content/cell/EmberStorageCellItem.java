package com.pingsu.appliedember.content.cell;

import appeng.items.storage.BasicStorageCell;
import com.pingsu.appliedember.me.key.EmberKeyType;
import net.minecraft.world.level.ItemLike;

/**
 * F1: drive storage cell holding Ember.
 *
 * <p>Extends AE2's own {@link BasicStorageCell} (the same base AE2's item/fluid cells use), so the built-in
 * {@code BasicCellHandler} recognizes it automatically — no custom {@code ICellHandler} registration needed.
 *
 * <p>Capacity = ({@code kilobytes} * 1024 − {@code bytesPerType} × 1) ×
 * {@link EmberKeyType#getAmountPerByte()} (= 1000 ember per byte). {@code EmberKey} is a singleton without
 * NBT, so {@code totalTypes} is pinned to 1.
 *
 * <p>Note: {@link BasicStorageCell} lives in AE2's internal {@code appeng.items} package — listed in the
 * plan's R3 internal-API risk register; re-check its constructor signature on every AE2 upgrade (plan §8.3).
 */
public class EmberStorageCellItem extends BasicStorageCell {

    public EmberStorageCellItem(Properties properties, ItemLike coreItem, ItemLike housingItem, double idleDrain,
            int kilobytes, int bytesPerType) {
        super(properties, coreItem, housingItem, idleDrain, kilobytes, bytesPerType, 1, EmberKeyType.TYPE);
    }
}
