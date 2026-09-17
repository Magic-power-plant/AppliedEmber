package com.pingsu.appliedember.compat.ae2.storage;

/**
 * Read-only view of the Ember stored in an ME network, as seen through a grid-backed capability.
 *
 * <p>Plan B4: {@code GenericStackEmberStorage} currently exposes Embers' writable {@code IEmberCapability}
 * while throwing {@link UnsupportedOperationException} from its setters — any Embers machine that calls a
 * setter crashes the server, and its empty {@code writeToNBT}/{@code deserializeNBT} implementations are
 * silent data-loss points. The fix is to expose only this read-only contract to the outside world and keep
 * the writable adapter internal.
 *
 * <p>Plan 4.2: this interface also replaces the empty marker {@code IAdvancedEmberCapability} (the
 * "Advanced" name carried no semantics).
 *
 * <p>Amounts are reported in whole ember units, saturated to {@code long} via
 * {@code EmberStorageAmounts} — see plan 4.4 for the double→long precision caveat above 2^53.
 */
public interface IGridEmberView {

    /**
     * Ember currently stored in the backing network inventory, in whole ember units.
     */
    long getStoredAmount();

    /**
     * Total ember the backing network inventory can hold, in whole ember units.
     */
    long getCapacity();
}
