package com.pingsu.appliedember.compat.ae2.storage;

import appeng.api.behaviors.GenericInternalInventory;
import com.pingsu.appliedember.me.key.EmberKey;

/**
 * Read-only Ember view over an AE2 {@link GenericInternalInventory} (the "generic inventory" of an
 * interface, pattern provider, ...).
 *
 * <p>Plan B4: this used to implement Embers' writable {@code IEmberCapability} on top of a
 * read-only backing store, which forced {@code setEmber}/{@code setEmberCapacity} to throw
 * {@link UnsupportedOperationException} — an Embers machine calling one of them would have crashed
 * the server — and left {@code writeToNBT}/{@code deserializeNBT} as silent no-ops. The adapter now
 * implements only the read-only {@link IGridEmberView}, so there is no writable method left to
 * crash on or to silently drop data: the real state lives in the block entity's own generic
 * inventory and is persisted by that block entity.
 *
 * <p>The backing inventory is inspected live on every call; this record holds no cache.
 */
@SuppressWarnings("UnstableApiUsage")
public record GenericStackEmberStorage(GenericInternalInventory inv) implements IGridEmberView {

    @Override
    public long getStoredAmount() {
        long stored = 0L;
        for (var slot = 0; slot < inv.size(); slot++) {
            if (inv.getKey(slot) == EmberKey.KEY) {
                stored += inv.getAmount(slot);
            }
        }
        return stored;
    }

    @Override
    public long getCapacity() {
        // AE2 stores a per-slot capacity per key type (see GenericSlotCapacities), which
        // EmberKeyType registers for ember, so this is the sum over the slots that are free or
        // already hold ember. Saturating multiplication guards against an unexpected overflow.
        long slots = 0L;
        for (var slot = 0; slot < inv.size(); slot++) {
            var key = inv.getKey(slot);
            if (key == null || key == EmberKey.KEY) {
                slots++;
            }
        }
        long perSlot = inv.getMaxAmount(EmberKey.KEY);
        if (slots <= 0 || perSlot <= 0) {
            return 0L;
        }
        if (perSlot > Long.MAX_VALUE / slots) {
            return Long.MAX_VALUE;
        }
        return slots * perSlot;
    }
}
