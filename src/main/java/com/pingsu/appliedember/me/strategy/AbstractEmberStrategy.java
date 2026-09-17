package com.pingsu.appliedember.me.strategy;

import appeng.api.stacks.AEKey;
import appeng.me.helpers.IGridConnectedBlockEntity;
import appeng.util.BlockApiCache;
import com.pingsu.appliedember.me.key.EmberKey;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.power.IEmberCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Shared base for the three Ember stack-world strategies (external storage / export / import).
 *
 * <p>Plan #6: collapses the identical {@code BlockApiCache.create} + side bookkeeping constructor
 * boilerplate and the repeated {@code what instanceof EmberKey} guard. Plan #7 (F2) hangs the
 * storage-bus guard {@link #isGridConnectedTarget()} off this base.
 */
@SuppressWarnings("UnstableApiUsage")
public abstract class AbstractEmberStrategy {

    protected final ServerLevel level;
    protected final BlockPos fromPos;
    protected final Direction fromSide;

    private final BlockApiCache<IEmberCapability> apiCache;

    protected AbstractEmberStrategy(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        this.level = level;
        this.fromPos = fromPos;
        this.fromSide = fromSide;
        this.apiCache = BlockApiCache.create(EmbersCapabilities.EMBER_CAPABILITY, level, fromPos);
    }

    /**
     * The Ember capability of the block this strategy faces, or {@code null} if there is none.
     */
    @Nullable
    protected IEmberCapability findCapability() {
        return apiCache.find(fromSide);
    }

    protected static boolean isEmberKey(AEKey what) {
        return what instanceof EmberKey;
    }

    /**
     * F2 guard: AE2 grid-connected devices (including our own ME Ember Cell) must not be exposed as
     * external storage, otherwise the network's contents get mounted back onto the network (loop /
     * double counting).
     *
     * <p>Must be evaluated per {@code createWrapper} call, not cached in the constructor: the target
     * block entity may be placed or removed after the bus cached this strategy.
     *
     * <p>Using {@link IGridConnectedBlockEntity} rather than a concrete class also blocks every other
     * AE2 grid device, which is the intended semantic: "an AE2 grid device is not an external
     * inventory".
     *
     * <p>Note: {@link IGridConnectedBlockEntity} lives in AE2's internal {@code appeng.me.helpers}
     * package — listed in the plan's R3 internal-API risk register.
     */
    protected boolean isGridConnectedTarget() {
        return isGridConnectedBlockEntity(level.getBlockEntity(fromPos));
    }

    /**
     * The F2 classification itself, split from the level lookup so its semantics are unit-testable
     * without a level (plan §8.2/§9.3): only block entities that participate in an AE2 grid count, and
     * a grid node host that is not a block entity does not.
     */
    static boolean isGridConnectedBlockEntity(@Nullable BlockEntity blockEntity) {
        return blockEntity instanceof IGridConnectedBlockEntity;
    }
}
