package com.pingsu.appliedember.me.key;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Plan #17 (key id and channel id unified) and plan 4.4 (the channel's scaling constants are part of
 * the storage/transfer maths, so they are pinned here).
 */
class EmberKeyIdentityTest {

    @Test
    void keyIdMatchesItsChannelId() {
        assertEquals(EmberKeyType.TYPE.getId(), EmberKey.KEY.getId());
        assertEquals("appliedember", EmberKey.KEY.getId().getNamespace());
        assertEquals("ember", EmberKey.KEY.getId().getPath());
    }

    @Test
    void keyBelongsToTheEmberChannel() {
        assertSame(EmberKeyType.TYPE, EmberKey.KEY.getType());
        assertEquals(EmberKey.class, EmberKeyType.TYPE.getKeyClass());
    }

    @Test
    void displayNameIsTheChannelName() {
        assertEquals(EmberKeyType.EMBER_NAME, EmberKey.KEY.getDisplayName());
    }

    @Test
    void scalingConstantsDriveCellAndOperationSizes() {
        assertEquals(1000, EmberKeyType.TYPE.getAmountPerByte());
        assertEquals(1000, EmberKeyType.TYPE.getAmountPerOperation());
        assertEquals(4000L, EmberKeyType.EMBER_PER_SLOT);
    }

    @Test
    void aOneKilobyteCellHoldsTheDocumentedAmount() {
        int kilobytes = 1;
        int bytesPerType = 8;
        int totalTypes = 1;

        long expected = (long) (kilobytes * 1024 - bytesPerType * totalTypes)
                * EmberKeyType.TYPE.getAmountPerByte();

        assertEquals(1_016_000L, expected);
    }
}
