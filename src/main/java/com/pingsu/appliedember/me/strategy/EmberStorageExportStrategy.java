package com.pingsu.appliedember.me.strategy;

import appeng.api.behaviors.StackExportStrategy;
import appeng.api.behaviors.StackTransferContext;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageHelper;
import com.pingsu.appliedember.me.key.EmberKey;
import com.pingsu.appliedember.me.storage.EmberStorageAmounts;
import com.rekindled.embers.api.power.IEmberCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pushes ember from an ME network into the block an export bus faces.
 *
 * <p>Plan B1: the previous version extracted from the network first and only then pushed into the
 * target, so any part the target refused was voided. This mirrors AE2's own
 * {@code appeng.parts.automation.StorageExportStrategy}: simulate both directions first, then commit
 * with the amount the target actually accepted, and hand any remaining overflow back to the network.
 *
 * <p>The algorithm itself lives in the static {@link #transferTo}/{@link #pushTo} methods: they only
 * need the transfer context and the target capability, so they are unit-testable without a level
 * (plan §8.2).
 */
@SuppressWarnings("UnstableApiUsage")
public class EmberStorageExportStrategy extends AbstractEmberStrategy implements StackExportStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmberStorageExportStrategy.class);

    public EmberStorageExportStrategy(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        super(level, fromPos, fromSide);
    }

    @Override
    public long transfer(StackTransferContext context, AEKey what, long amount) {
        if (!isEmberKey(what)) {
            return 0;
        }

        var emberCap = findCapability();
        return emberCap == null ? 0 : transferTo(context, emberCap, amount);
    }

    @Override
    public long push(AEKey what, long amount, Actionable mode) {
        if (!isEmberKey(what)) {
            return 0;
        }

        var emberCap = findCapability();
        return emberCap == null ? 0 : pushTo(emberCap, amount, mode);
    }

    /**
     * Moves up to {@code amount} ember out of the network inventory described by {@code context} into
     * {@code target}, returning the amount that ended up in the target.
     *
     * <p>Invariant: none of the ember taken out of the network is lost — whatever the target refuses
     * is inserted back into the network, and only an unexpected refusal on both sides is logged.
     */
    static long transferTo(StackTransferContext context, IEmberCapability target, long amount) {
        var inv = context.getInternalStorage().getInventory();

        // 1) How much can the network spare, and how much of that would the target take right now?
        long available = StorageHelper.poweredExtraction(
                context.getEnergySource(),
                inv,
                EmberKey.KEY,
                EmberStorageAmounts.nonNegative(amount),
                context.getActionSource(),
                Actionable.SIMULATE
        );
        if (available <= 0) {
            return 0;
        }

        long accepted = EmberStorageAmounts.saturatingLong(target.addAmount((double) available, false));
        if (accepted <= 0) {
            return 0;
        }

        // 2) Commit: pull the accepted amount out of the network, then push it into the target.
        long extracted = StorageHelper.poweredExtraction(
                context.getEnergySource(),
                inv,
                EmberKey.KEY,
                accepted,
                context.getActionSource(),
                Actionable.MODULATE
        );
        if (extracted <= 0) {
            return 0;
        }

        long inserted = EmberStorageAmounts.saturatingLong(target.addAmount((double) extracted, true));

        if (inserted < extracted) {
            // Be nice and try to give the overflow back to the network instead of voiding it (B1).
            long leftover = extracted - inserted;
            leftover -= inv.insert(EmberKey.KEY, leftover, Actionable.MODULATE, context.getActionSource());
            if (leftover > 0) {
                LOGGER.error("Storage export: adjacent block unexpectedly refused insert, voided {}x{}",
                        leftover, EmberKey.KEY);
            }
        }

        return inserted;
    }

    /**
     * Offers ember to {@code target} without involving a network, used by the export bus' "push" path.
     * The capability's own simulate path decides the answer, so the returned value is what it accepted.
     */
    static long pushTo(IEmberCapability target, long amount, Actionable mode) {
        return EmberStorageAmounts.saturatingLong(target.addAmount(
                (double) EmberStorageAmounts.nonNegative(amount),
                mode == Actionable.MODULATE));
    }
}
