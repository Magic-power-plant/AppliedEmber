package com.pingsu.appliedember.content.ember;

import com.pingsu.appliedember.me.storage.EmberStorageAmounts;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.power.IEmberCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

final class EmberCellExporter {

    /**
     * "As much as the target can take" sentinel. {@link Integer#MAX_VALUE} rather than
     * {@code Long.MAX_VALUE} because ember amounts travel as {@code double} and only the former is
     * exactly representable there (plan 4.4).
     */
    private static final long ALL_EMBER = Integer.MAX_VALUE;

    private final IEmberCapability source;

    EmberCellExporter(IEmberCapability source) {
        this.source = source;
    }

    void outputToAdjacentBlocks(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            transferTo(level, pos, direction);
        }
    }

    private void transferTo(Level level, BlockPos pos, Direction direction) {
        BlockEntity adjacent = level.getBlockEntity(pos.relative(direction));
        if (adjacent == null) {
            return;
        }

        IEmberCapability target = adjacent.getCapability(EmbersCapabilities.EMBER_CAPABILITY, direction.getOpposite())
                .orElse(null);
        if (target == null) {
            return;
        }

        long transferable = EmberStorageAmounts.clampToFree(target, ALL_EMBER);
        if (transferable <= 0L) {
            return;
        }

        double extracted = source.removeAmount((double) transferable, true);
        if (extracted <= 0.0D) {
            return;
        }

        double inserted = target.addAmount(extracted, true);
        double remainder = extracted - inserted;
        if (remainder > 0.0D) {
            source.addAmount(remainder, true);
        }
    }
}
