package com.pingsu.appliedember.me.strategy;

import appeng.api.config.Actionable;
import com.pingsu.appliedember.me.key.EmberKey;
import com.rekindled.embers.power.DefaultEmberCapability;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Terminal container-item interaction (§9.8): the capability-facing half of
 * {@link EmberContainerItemStrategy} — what a right-click empty sees as extractable content, and how
 * the fill/drain calls map onto the capability's simulate/modulate path. The ItemStack lookup layer
 * ({@code getCapability} resolution, carried stack) needs a Minecraft runtime and is deliberately a
 * thin shell over these helpers.
 */
class EmberContainerItemStrategyTest {

    /** {@link EmberKey#KEY} is declared as {@code AEKey}; the strategy's API wants the subtype. */
    private static final EmberKey KEY = (EmberKey) EmberKey.KEY;

    private static DefaultEmberCapability capability(double capacity, double ember) {
        var capability = new DefaultEmberCapability();
        capability.setEmberCapacity(capacity);
        capability.setEmber(ember);
        return capability;
    }

    @Test
    void emptyContainerHasNoContainedStack() {
        assertNull(EmberContainerItemStrategy.containedStack(capability(2000.0D, 0.0D)));
    }

    @Test
    void missingCapabilityHasNoContainedStack() {
        assertNull(EmberContainerItemStrategy.containedStack(null));
    }

    @Test
    void filledContainerReportsItsEmberAsContainedStack() {
        var jar = capability(2000.0D, 1500.5D);

        var contained = EmberContainerItemStrategy.containedStack(jar);

        assertSame(EmberKey.KEY, contained.what());
        assertEquals(1500L, contained.amount(), "double -> long is a saturating floor (plan 4.4)");
    }

    @Test
    void extractableContentMatchesContainedStackSemantics() {
        var jar = capability(2000.0D, 500.0D);

        var content = EmberContainerItemStrategy.INSTANCE.getExtractableContent(jar);

        assertEquals(500L, content.amount());
        assertEquals(500.0D, jar.getEmber(), "reading the extractable content must not drain the jar");
    }

    @Test
    void extractDrainsTheContainerOnModulateOnly() {
        var jar = capability(2000.0D, 800.0D);

        assertEquals(800L, EmberContainerItemStrategy.INSTANCE.extract(
                jar, KEY, 1000L, Actionable.SIMULATE));
        assertEquals(800.0D, jar.getEmber(), "a simulated drain must not change the container");

        assertEquals(800L, EmberContainerItemStrategy.INSTANCE.extract(
                jar, KEY, 1000L, Actionable.MODULATE));
        assertEquals(0.0D, jar.getEmber());
    }

    @Test
    void insertFillsTheContainerUpToItsFreeSpace() {
        var jar = capability(2000.0D, 500.0D);

        // Only 1500 free: the capability's own clamp decides, the strategy must report what was
        // actually accepted rather than what was asked for (plan B3 invariant).
        assertEquals(1500L, EmberContainerItemStrategy.INSTANCE.insert(
                jar, KEY, 10_000L, Actionable.MODULATE));
        assertEquals(2000.0D, jar.getEmber());
    }

    @Test
    void insertSimulatesWithoutCommitting() {
        var jar = capability(2000.0D, 0.0D);

        assertEquals(2000L, EmberContainerItemStrategy.INSTANCE.insert(
                jar, KEY, 2000L, Actionable.SIMULATE));
        assertEquals(0.0D, jar.getEmber(), "a simulated fill must not change the container");
    }

    @Test
    void negativeAmountsAreTreatedAsZero() {
        var jar = capability(2000.0D, 500.0D);

        assertEquals(0L, EmberContainerItemStrategy.INSTANCE.extract(
                jar, KEY, -5L, Actionable.MODULATE));
        assertEquals(0L, EmberContainerItemStrategy.INSTANCE.insert(
                jar, KEY, -5L, Actionable.MODULATE));
        assertEquals(500.0D, jar.getEmber());
    }
}
