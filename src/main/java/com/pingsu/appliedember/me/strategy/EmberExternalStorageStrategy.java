package com.pingsu.appliedember.me.strategy;

import appeng.api.behaviors.ExternalStorageStrategy;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.core.localization.GuiText;
import com.pingsu.appliedember.me.key.EmberKey;
import com.pingsu.appliedember.me.key.EmberKeyType;
import com.pingsu.appliedember.me.storage.EmberStorageAmounts;
import com.rekindled.embers.api.power.IEmberCapability;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Lets an AE2 storage bus see a neighbouring Ember machine (or pipe) as external storage.
 *
 * <p>Plan F2: {@link #createWrapper} refuses AE2 grid devices, so an ME Ember Cell (or any other grid
 * machine) facing a storage bus can never be mounted back onto the network it belongs to.
 */
@SuppressWarnings("UnstableApiUsage")
public class EmberExternalStorageStrategy extends AbstractEmberStrategy implements ExternalStorageStrategy {

    public EmberExternalStorageStrategy(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        super(level, fromPos, fromSide);
    }

    @Nullable
    @Override
    public MEStorage createWrapper(boolean extractableOnly, Runnable injectOrExtractCallback) {
        // F2 guard, evaluated per call on purpose: the target may be placed/removed after the bus
        // cached this strategy, and it must be re-checked every time a wrapper is requested.
        if (isGridConnectedTarget()) {
            return null;
        }

        var emberCap = findCapability();
        return emberCap != null ? new Adaptor(emberCap, injectOrExtractCallback) : null;
    }

    /**
     * {@link MEStorage} view over an Ember capability. Package-private (rather than private) so it can
     * be unit-tested directly — it has no dependency on the level (plan §8.2).
     */
    record Adaptor(IEmberCapability emberCapability, Runnable injectOrExtractCallback) implements MEStorage {
        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource actionSource) {
            if (!isEmberKey(what)) {
                return 0;
            }

            // Ask the capability itself (its own simulate path) instead of pre-clamping by hand: that
            // keeps Embers' per-machine acceptance rules authoritative and avoids a second
            // implementation of the same clamp-then-modulate logic (plan 4.1).
            double accepted = emberCapability.addAmount(
                    (double) EmberStorageAmounts.nonNegative(amount),
                    mode == Actionable.MODULATE);
            long inserted = EmberStorageAmounts.saturatingLong(accepted);
            if (inserted > 0 && mode == Actionable.MODULATE) {
                injectOrExtractCallback.run();
            }
            return inserted;
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource actionSource) {
            if (!isEmberKey(what)) {
                return 0;
            }

            double removed = emberCapability.removeAmount(
                    (double) EmberStorageAmounts.nonNegative(amount),
                    mode == Actionable.MODULATE);
            long extracted = EmberStorageAmounts.saturatingLong(removed);
            if (extracted > 0 && mode == Actionable.MODULATE) {
                injectOrExtractCallback.run();
            }
            return extracted;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            long currentEmber = EmberStorageAmounts.stored(emberCapability);
            if (currentEmber != 0) {
                out.add(EmberKey.KEY, currentEmber);
            }
        }

        @Override
        public Component getDescription() {
            return GuiText.ExternalStorage.text(EmberKeyType.TYPE.getDescription());
        }
    }
}
