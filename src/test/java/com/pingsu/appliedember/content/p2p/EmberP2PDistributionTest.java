package com.pingsu.appliedember.content.p2p;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plan §8.2 / §5-F3: the tunnel splits ember evenly over its outputs and rolls a shortfall over to
 * the next output, so an output that cannot take its share must not make the difference vanish inside
 * the tunnel (whatever is not accepted is reported back to the caller and stays at the source).
 */
class EmberP2PDistributionTest {

    @Test
    void splitsEvenlyWhenEveryOutputAccepts() {
        var received = new ArrayList<Double>();
        double total = EmberP2PTunnelPart.distributeEvenly(1000, 4, (index, toSend) -> {
            received.add(toSend);
            return toSend;
        });

        assertEquals(1000.0D, total);
        assertEquals(List.of(250.0D, 250.0D, 250.0D, 250.0D), received);
    }

    @Test
    void singleOutputTakesEverything() {
        double total = EmberP2PTunnelPart.distributeEvenly(777, 1, (index, toSend) -> toSend);

        assertEquals(777.0D, total);
    }

    @Test
    void shortfallOfOneOutputRollsOverToTheNext() {
        // Each output can only take 100; the leftovers must be carried forward, not dropped.
        var received = new ArrayList<Double>();
        double total = EmberP2PTunnelPart.distributeEvenly(900, 3, (index, toSend) -> {
            double accepted = Math.min(toSend, 100.0D);
            received.add(accepted);
            return accepted;
        });

        assertEquals(300.0D, total);
        assertEquals(3, received.size());
        for (double accepted : received) {
            assertEquals(100.0D, accepted);
        }
        assertTrue(total <= 900.0D, "the tunnel must never claim to have moved more than it was given");
    }

    @Test
    void neverReportsMoreThanWasOffered() {
        // The first output happily takes its share, the second refuses everything.
        double total = EmberP2PTunnelPart.distributeEvenly(100, 2,
                (index, toSend) -> index == 0 ? toSend : 0.0D);

        assertEquals(50.0D, total);
    }

    @Test
    void boundaryInputsAreRefused() {
        assertEquals(0.0D, EmberP2PTunnelPart.distributeEvenly(100, 0, (index, toSend) -> toSend));
        assertEquals(0.0D, EmberP2PTunnelPart.distributeEvenly(0, 3, (index, toSend) -> toSend));
        assertEquals(0.0D, EmberP2PTunnelPart.distributeEvenly(-5, 3, (index, toSend) -> toSend));
    }

    @Test
    void fractionalAmountsAreFullyDistributed() {
        double total = EmberP2PTunnelPart.distributeEvenly(1000.5D, 3, (index, toSend) -> toSend);

        assertEquals(1000.5D, total, 1.0e-9D);
    }
}
