package com.pingsu.appliedember.me.strategy;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.behaviors.StackTransferContext;
import appeng.api.config.Actionable;
import com.pingsu.appliedember.me.key.EmberKey;
import com.pingsu.appliedember.me.key.EmberKeyType;
import com.pingsu.appliedember.me.storage.EmberStorageAmounts;
import com.rekindled.embers.api.power.IEmberCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pulls ember from the block an import bus faces into the ME network.
 *
 * <p>Plan B3: the network insert is simulated first and the source is then drained by exactly the
 * amount the network said it would take, and the amount actually removed (not the requested amount)
 * is what gets inserted. Between that SIMULATE and the MODULATE the grid can still change, so a
 * residual is possible; it is handed back to the source and, if even the source refuses it, reported
 * as a bounded, single-operation loss rather than being silently dropped.
 *
 * <p>The algorithm lives in the static {@link #importFrom} method, which only needs the transfer
 * context and the source capability — no level — so it is unit-testable (plan §8.2).
 */
@SuppressWarnings("UnstableApiUsage")
public class EmberStorageImportStrategy extends AbstractEmberStrategy implements StackImportStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmberStorageImportStrategy.class);

    public EmberStorageImportStrategy(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        super(level, fromPos, fromSide);
    }

    @Override
    public boolean transfer(StackTransferContext context) {
        var emberCap = findCapability();
        return emberCap != null && importFrom(context, emberCap);
    }

    /**
     * Moves ember out of {@code source} into the network inventory described by {@code context}.
     *
     * @return whether anything was imported
     */
    static boolean importFrom(StackTransferContext context, IEmberCapability source) {
        if (!context.isKeyTypeEnabled(EmberKeyType.TYPE) || !context.isInFilter(EmberKey.KEY)) {
            return false;
        }

        int amountPerOperation = EmberKeyType.TYPE.getAmountPerOperation();
        long remainingTransferAmount = (long) context.getOperationsRemaining() * amountPerOperation;
        long storedInSource = EmberStorageAmounts.clampToStored(source, remainingTransferAmount);
        if (storedInSource <= 0) {
            return false;
        }

        var inv = context.getInternalStorage().getInventory();

        // Be conservative: only drain as much as the network would accept right now.
        long accepted = inv.insert(EmberKey.KEY, storedInSource, Actionable.SIMULATE, context.getActionSource());
        if (accepted <= 0) {
            return false;
        }

        // Use what the source actually gave up; a capability may return less than it advertised.
        long removed = EmberStorageAmounts.saturatingLong(source.removeAmount((double) accepted, true));
        if (removed <= 0) {
            return false;
        }

        long inserted = inv.insert(EmberKey.KEY, removed, Actionable.MODULATE, context.getActionSource());
        if (inserted < removed) {
            long leftover = removed - inserted;
            long backFill = EmberStorageAmounts.saturatingLong(source.addAmount((double) leftover, true));
            long voided = leftover - backFill;
            if (voided > 0) {
                // Trade-off documented for plan B3: the SIMULATE/MODULATE gap cannot be closed
                // without a grid-wide transaction lock, so this residual is possible. It is bounded
                // by the bus' operations-per-tick and is warned about rather than treated as a
                // critical error.
                LOGGER.warn("Storage import: {} ember neither fit the network nor the source, voided", voided);
            }
        }

        long opsUsed = Math.max(1, inserted / Math.max(1, amountPerOperation));
        context.reduceOperationsRemaining(opsUsed);

        return inserted > 0;
    }
}
