package com.pingsu.appliedember.me.strategy;

import appeng.api.networking.IManagedGridNode;
import appeng.me.helpers.IGridConnectedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plan F2 / §8.2: a storage bus must not mount AE2 grid devices (including our own ME Ember Cell) back
 * onto the network. The classification that implements the guard is tested here; the level lookup that
 * feeds it belongs to AE2.
 *
 * <p>The fake block entity is instantiated with a null type/state: {@link BlockEntity}'s constructor
 * only stores them, so no Minecraft registry has to be bootstrapped for these assertions.
 */
class EmberExternalStorageStrategyGuardTest {

    private static final class FakeGridBlockEntity extends BlockEntity implements IGridConnectedBlockEntity {
        FakeGridBlockEntity() {
            super(null, BlockPos.ZERO, null);
        }

        @Override
        public IManagedGridNode getMainNode() {
            return null;
        }

        @Override
        public void saveChanges() {
        }
    }

    private static final class FakePlainBlockEntity extends BlockEntity {
        FakePlainBlockEntity() {
            super(null, BlockPos.ZERO, null);
        }
    }

    @Test
    void gridDevicesAreRecognised() {
        assertTrue(AbstractEmberStrategy.isGridConnectedBlockEntity(new FakeGridBlockEntity()));
    }

    @Test
    void ordinaryBlockEntitiesAreNotGridDevices() {
        assertFalse(AbstractEmberStrategy.isGridConnectedBlockEntity(new FakePlainBlockEntity()));
    }

    @Test
    void missingTargetIsNotAGridDevice() {
        assertFalse(AbstractEmberStrategy.isGridConnectedBlockEntity(null));
    }

    @Test
    void theMeEmberCellItselfIsAGridDevice() {
        // Guards the concrete case from the plan: the ME Ember Cell's block entity is exactly the kind
        // of device that must stay invisible to a neighbouring storage bus.
        assertTrue(IGridConnectedBlockEntity.class.isAssignableFrom(
                com.pingsu.appliedember.content.ember.MEEmberCellEntity.class));
    }
}
