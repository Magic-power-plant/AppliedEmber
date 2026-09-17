package com.pingsu.appliedember.me.storage;

import com.rekindled.embers.power.DefaultEmberCapability;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Plan 4.4: the {@code double}/{@code long} boundary is where ember amounts silently go wrong, so the
 * conversion helpers get a real test rather than only a comment.
 */
class EmberStorageAmountsTest {

    @Test
    void saturatingConversionHandlesNonFiniteAndNegativeValues() {
        assertEquals(0L, EmberStorageAmounts.saturatingLong(Double.NaN));
        assertEquals(0L, EmberStorageAmounts.saturatingLong(-1.0D));
        assertEquals(0L, EmberStorageAmounts.saturatingLong(0.0D));
        assertEquals(0L, EmberStorageAmounts.saturatingLong(0.9D));
    }

    @Test
    void saturatingConversionFloorsAndSaturates() {
        assertEquals(12L, EmberStorageAmounts.saturatingLong(12.7D));
        assertEquals(Integer.MAX_VALUE, EmberStorageAmounts.saturatingLong(Integer.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, EmberStorageAmounts.saturatingLong(Double.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, EmberStorageAmounts.saturatingLong(Long.MAX_VALUE));
    }

    @Test
    void nonNegativeClampsAtZero() {
        assertEquals(0L, EmberStorageAmounts.nonNegative(-42L));
        assertEquals(7L, EmberStorageAmounts.nonNegative(7L));
    }

    @Test
    void storedAndFreeReflectTheCapability() {
        var capability = new DefaultEmberCapability();
        capability.setEmberCapacity(1000.0D);
        capability.setEmber(400.0D);

        assertEquals(400L, EmberStorageAmounts.stored(capability));
        assertEquals(600L, EmberStorageAmounts.free(capability));
    }

    @Test
    void clampsNeverExceedWhatTheCapabilityHasOrCanTake() {
        var capability = new DefaultEmberCapability();
        capability.setEmberCapacity(1000.0D);
        capability.setEmber(400.0D);

        assertEquals(400L, EmberStorageAmounts.clampToStored(capability, 500L));
        // 500 fits into the 600 free, so the request itself is the limit; a larger request is capped
        // by the free space instead.
        assertEquals(500L, EmberStorageAmounts.clampToFree(capability, 500L));
        assertEquals(600L, EmberStorageAmounts.clampToFree(capability, 5000L));
        assertEquals(0L, EmberStorageAmounts.clampToStored(capability, -5L));
        assertEquals(0L, EmberStorageAmounts.clampToFree(capability, -5L));
    }

    @Test
    void fullCapabilityReportsNoFreeSpace() {
        var capability = new DefaultEmberCapability();
        capability.setEmberCapacity(100.0D);
        capability.setEmber(100.0D);

        assertEquals(0L, EmberStorageAmounts.free(capability));
        assertEquals(0L, EmberStorageAmounts.clampToFree(capability, 50L));
    }
}
