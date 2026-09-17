package com.pingsu.appliedember.me.storage;

import com.rekindled.embers.api.power.IEmberCapability;

/**
 * Conversions and clamps between Embers' {@code double} ember amounts and AE2's {@code long} stack
 * amounts.
 *
 * <p>Precision caveat (plan 4.4): the {@code double -> long} direction is a saturating floor. Above
 * 2^53 a {@code double} can no longer represent every integer, so amounts beyond that magnitude are
 * approximate. AE2 stack amounts are {@code long}, Embers amounts {@code double}; the two never agree
 * exactly in that range, and no mod-side fix is possible without changing one of the two APIs.
 */
public final class EmberStorageAmounts {
    private EmberStorageAmounts() {
    }

    public static long stored(IEmberCapability emberCapability) {
        return saturatingLong(emberCapability.getEmber());
    }

    public static long free(IEmberCapability emberCapability) {
        return saturatingLong(emberCapability.getEmberCapacity() - emberCapability.getEmber());
    }

    public static long clampToStored(IEmberCapability emberCapability, long requested) {
        return Math.min(nonNegative(requested), stored(emberCapability));
    }

    public static long clampToFree(IEmberCapability emberCapability, long requested) {
        return Math.min(nonNegative(requested), free(emberCapability));
    }

    public static long nonNegative(long value) {
        return Math.max(0L, value);
    }

    /**
     * Saturating {@code double -> long} conversion: {@code NaN}, negatives and negatives-zero become
     * {@code 0}; values at or above {@link Long#MAX_VALUE} saturate instead of overflowing.
     */
    public static long saturatingLong(double value) {
        if (Double.isNaN(value) || value <= 0.0D) {
            return 0L;
        }
        if (value >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return (long) value;
    }
}
