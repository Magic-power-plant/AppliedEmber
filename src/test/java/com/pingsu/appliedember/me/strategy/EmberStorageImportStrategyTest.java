package com.pingsu.appliedember.me.strategy;

import com.pingsu.appliedember.support.FakeEmberNetworkStorage;
import com.pingsu.appliedember.support.FakeTransferContext;
import com.rekindled.embers.power.DefaultEmberCapability;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plan B3: the import path used to insert the amount it *asked* the source for rather than the amount
 * the source actually gave up, which duplicated ember whenever a capability returned less than it
 * advertised. These tests pin the fixed behaviour and the conservative simulate/modulate ordering.
 */
class EmberStorageImportStrategyTest {

    /** Capability that only gives up half of what it is asked for on the real (MODULATE) call. */
    private static final class HalfGivingEmberCapability extends DefaultEmberCapability {
        @Override
        public double removeAmount(double value, boolean doRemove) {
            if (!doRemove) {
                return super.removeAmount(value, false);
            }
            return super.removeAmount(value / 2.0D, true);
        }
    }

    @Test
    void importsStoredEmberIntoTheNetwork() {
        var network = new FakeEmberNetworkStorage(0L, 10_000L);
        var context = new FakeTransferContext(network, 2);
        var source = new DefaultEmberCapability();
        source.setEmberCapacity(1000.0D);
        source.setEmber(500.0D);

        assertTrue(EmberStorageImportStrategy.importFrom(context, source));

        assertEquals(500L, network.stored());
        assertEquals(0.0D, source.getEmber());
        // 500 ember is half an operation's worth (1000 per operation) -> at least one operation used.
        assertEquals(1, context.getOperationsRemaining());
    }

    @Test
    void neverImportsMoreThanTheNetworkCanHold() {
        var network = new FakeEmberNetworkStorage(0L, 300L);
        var context = new FakeTransferContext(network, 2);
        var source = new DefaultEmberCapability();
        source.setEmberCapacity(1000.0D);
        source.setEmber(1000.0D);

        assertTrue(EmberStorageImportStrategy.importFrom(context, source));

        assertEquals(300L, network.stored());
        assertEquals(700.0D, source.getEmber());
    }

    @Test
    void onlyTheAmountActuallyRemovedIsInserted() {
        var network = new FakeEmberNetworkStorage(0L, 10_000L);
        var context = new FakeTransferContext(network, 1);
        var source = new HalfGivingEmberCapability();
        source.setEmberCapacity(1000.0D);
        source.setEmber(1000.0D);

        assertTrue(EmberStorageImportStrategy.importFrom(context, source));

        // The source advertised 1000 but only gave 500; nothing may be duplicated out of thin air.
        assertEquals(500L, network.stored());
        assertEquals(500.0D, source.getEmber());
        assertEquals(1000L, network.stored() + (long) source.getEmber());
    }

    @Test
    void doesNothingWhenTheKeyTypeIsDisabled() {
        var network = new FakeEmberNetworkStorage(0L, 10_000L);
        var context = new FakeTransferContext(network, 2);
        context.setKeyTypeEnabled(false);
        var source = new DefaultEmberCapability();
        source.setEmberCapacity(1000.0D);
        source.setEmber(500.0D);

        assertFalse(EmberStorageImportStrategy.importFrom(context, source));
        assertEquals(0L, network.stored());
        assertEquals(500.0D, source.getEmber());
    }

    @Test
    void doesNothingWithoutOperationsLeft() {
        var network = new FakeEmberNetworkStorage(0L, 10_000L);
        var context = new FakeTransferContext(network, 0);
        var source = new DefaultEmberCapability();
        source.setEmberCapacity(1000.0D);
        source.setEmber(500.0D);

        assertFalse(EmberStorageImportStrategy.importFrom(context, source));
        assertEquals(0L, network.stored());
        assertEquals(500.0D, source.getEmber());
    }

    @Test
    void doesNothingWhenTheSourceIsEmpty() {
        var network = new FakeEmberNetworkStorage(0L, 10_000L);
        var context = new FakeTransferContext(network, 2);
        var source = new DefaultEmberCapability();
        source.setEmberCapacity(1000.0D);
        source.setEmber(0.0D);

        assertFalse(EmberStorageImportStrategy.importFrom(context, source));
        assertEquals(0L, network.stored());
    }
}
