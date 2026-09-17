package com.pingsu.appliedember.compat.ae2.storage;

import appeng.api.stacks.GenericStack;
import com.pingsu.appliedember.me.key.EmberKey;
import com.pingsu.appliedember.support.FakeGenericInventory;
import com.rekindled.embers.api.power.IEmberCapability;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plan B4: the adapter over an AE2 generic inventory must expose a read-only contract. These tests
 * pin down both halves of that: it reports the right numbers, and it structurally cannot be called
 * through Embers' writable capability interface (the previous version threw from its setters).
 */
class GenericStackEmberStorageTest {

    private static final long PER_SLOT_CAPACITY = 4000L;

    @Test
    void sumsEmberAcrossSlots() {
        var inventory = new FakeGenericInventory(4, PER_SLOT_CAPACITY,
                new GenericStack(EmberKey.KEY, 1000L),
                null,
                new GenericStack(EmberKey.KEY, 250L),
                null);

        var view = new GenericStackEmberStorage(inventory);

        assertEquals(1250L, view.getStoredAmount());
        assertEquals(4 * PER_SLOT_CAPACITY, view.getCapacity());
    }

    @Test
    void emptyInventoryReportsZeroStoredAndFullCapacity() {
        var view = new GenericStackEmberStorage(new FakeGenericInventory(2, PER_SLOT_CAPACITY));

        assertEquals(0L, view.getStoredAmount());
        assertEquals(2 * PER_SLOT_CAPACITY, view.getCapacity());
    }

    @Test
    void slotsHoldingOtherKeysDoNotCountTowardsEmberCapacity() {
        // A slot occupied by a foreign key is not available for ember, so it must not be counted.
        var inventory = new FakeGenericInventory(3, PER_SLOT_CAPACITY,
                new GenericStack(EmberKey.KEY, 10L),
                new GenericStack(com.pingsu.appliedember.support.FakeForeignKey.INSTANCE, 64L),
                null);
        var view = new GenericStackEmberStorage(inventory);

        assertEquals(10L, view.getStoredAmount());
        assertEquals(2 * PER_SLOT_CAPACITY, view.getCapacity());
    }

    @Test
    void adapterDoesNotImplementTheWritableEmberCapability() {
        assertFalse(IEmberCapability.class.isAssignableFrom(GenericStackEmberStorage.class),
                "the adapter must not be reachable through Embers' writable capability");
        assertTrue(IGridEmberView.class.isAssignableFrom(GenericStackEmberStorage.class));
    }

    @Test
    void readOnlyViewDeclaresNoMutators() {
        for (Method method : IGridEmberView.class.getDeclaredMethods()) {
            var name = method.getName();
            assertFalse(name.startsWith("set") || name.startsWith("add") || name.startsWith("remove")
                            || name.startsWith("write") || name.startsWith("deserialize"),
                    () -> "IGridEmberView must not expose mutators, found " + name);
        }
        assertTrue(Arrays.stream(IGridEmberView.class.getDeclaredMethods()).count() > 0);
    }
}
