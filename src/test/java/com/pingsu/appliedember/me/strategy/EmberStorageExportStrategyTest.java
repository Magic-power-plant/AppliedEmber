package com.pingsu.appliedember.me.strategy;

import appeng.api.config.Actionable;
import com.pingsu.appliedember.support.FakeEmberNetworkStorage;
import com.pingsu.appliedember.support.FakeTransferContext;
import com.rekindled.embers.power.DefaultEmberCapability;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plan B1: the export path used to extract from the network and then push into the target, voiding
 * whatever the target refused. These tests pin the fixed behaviour: the amount taken out of the
 * network is either delivered to the target or put back, never lost.
 */
class EmberStorageExportStrategyTest {

    /** Capability that simulates more acceptance than it actually performs, like a target filling up mid-transfer. */
    private static final class StingyEmberCapability extends DefaultEmberCapability {
        private final double actuallyAccepts;

        StingyEmberCapability(double actuallyAccepts) {
            this.actuallyAccepts = actuallyAccepts;
        }

        @Override
        public double addAmount(double value, boolean doAdd) {
            if (!doAdd) {
                return super.addAmount(value, false);
            }
            return super.addAmount(Math.min(value, actuallyAccepts), true);
        }
    }

    @Test
    void transfersWhatTheTargetCanHold() {
        var network = new FakeEmberNetworkStorage(1000L, 10_000L);
        var context = new FakeTransferContext(network, 8);
        var target = new DefaultEmberCapability();
        target.setEmberCapacity(100.0D);
        target.setEmber(0.0D);

        long moved = EmberStorageExportStrategy.transferTo(context, target, 1000L);

        assertEquals(100L, moved);
        assertEquals(100.0D, target.getEmber());
        assertEquals(900L, network.stored());
    }

    @Test
    void voidedEmberIsReturnedToTheNetwork() {
        var network = new FakeEmberNetworkStorage(1000L, 10_000L);
        var context = new FakeTransferContext(network, 8);
        // Simulates as if it could take 100, but only ever accepts 40.
        var target = new StingyEmberCapability(40.0D);
        target.setEmberCapacity(100.0D);
        target.setEmber(0.0D);

        long moved = EmberStorageExportStrategy.transferTo(context, target, 1000L);

        assertEquals(40L, moved);
        assertEquals(40.0D, target.getEmber());
        // 100 was taken from the network, 40 reached the target, the remaining 60 came back.
        assertEquals(960L, network.stored());
        assertEquals(1000L, network.stored() + (long) target.getEmber());
        assertTrue(network.modulateInsertCalls() >= 1, "the overflow should have been reinserted");
    }

    @Test
    void nothingIsTakenWhenTheTargetIsFull() {
        var network = new FakeEmberNetworkStorage(1000L, 10_000L);
        var context = new FakeTransferContext(network, 8);
        var target = new DefaultEmberCapability();
        target.setEmberCapacity(100.0D);
        target.setEmber(100.0D);

        assertEquals(0L, EmberStorageExportStrategy.transferTo(context, target, 500L));
        assertEquals(1000L, network.stored());
        assertEquals(100.0D, target.getEmber());
    }

    @Test
    void nothingIsTakenWhenTheNetworkIsEmpty() {
        var network = new FakeEmberNetworkStorage(0L, 10_000L);
        var context = new FakeTransferContext(network, 8);
        var target = new DefaultEmberCapability();
        target.setEmberCapacity(100.0D);
        target.setEmber(0.0D);

        assertEquals(0L, EmberStorageExportStrategy.transferTo(context, target, 500L));
        assertEquals(0.0D, target.getEmber());
    }

    @Test
    void pushPathHonoursSimulateAndModulate() {
        var target = new DefaultEmberCapability();
        target.setEmberCapacity(100.0D);
        target.setEmber(0.0D);

        assertEquals(100L, EmberStorageExportStrategy.pushTo(target, 250L, Actionable.SIMULATE));
        assertEquals(0.0D, target.getEmber(), "a simulated push must not change the target");

        assertEquals(100L, EmberStorageExportStrategy.pushTo(target, 250L, Actionable.MODULATE));
        assertEquals(100.0D, target.getEmber());
    }
}
