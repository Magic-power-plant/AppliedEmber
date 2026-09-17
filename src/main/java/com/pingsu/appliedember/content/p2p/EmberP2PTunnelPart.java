package com.pingsu.appliedember.content.p2p;

import java.util.List;
import java.util.function.ToDoubleBiFunction;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.parts.p2p.CapabilityP2PTunnelPart;
import appeng.parts.p2p.P2PModels;
import com.pingsu.appliedember.AppliedEmberResources;
import com.pingsu.appliedember.me.key.EmberKeyType;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.power.IEmberCapability;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

/**
 * F3: P2P tunnel for Embers' {@link IEmberCapability}, modelled on AE2's {@code FEP2PTunnelPart}.
 *
 * <p>The input side accepts ember from an adjacent source and splits it evenly across all outputs (with
 * remainder roll-over, mirroring {@code FEP2PTunnelPart.receiveEnergy}); the output side lets an adjacent
 * machine pull ember from the input. Recursion between touching tunnels is prevented by the base class's
 * {@code CapabilityGuard} access-depth mechanism.
 *
 * <p>Note on the boolean parameter of {@link IEmberCapability#addAmount} / {@code removeAmount}: Embers
 * Rekindled uses {@code true} = actually perform the transfer (the opposite of Forge's usual
 * {@code simulate} convention), matching how the rest of this mod calls it.
 *
 * <p>Acceptance criteria (plan §5-F3): source at the input side and machines at the output side transfer
 * across the tunnel; multi-output splitting leaves no voided ember; nested tunnels do not recurse.
 */
@SuppressWarnings("UnstableApiUsage")
public class EmberP2PTunnelPart extends CapabilityP2PTunnelPart<EmberP2PTunnelPart, IEmberCapability> {

    private static final P2PModels MODELS = new P2PModels(AppliedEmberResources.id("part/p2p/p2p_tunnel_ember"));
    private static final IEmberCapability NULL_HANDLER = new NullEmberCapability();

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    public EmberP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem, EmbersCapabilities.EMBER_CAPABILITY);
        inputHandler = new InputEmberHandler();
        outputHandler = new OutputEmberHandler();
        emptyHandler = NULL_HANDLER;
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(isPowered(), isActive());
    }

    /**
     * Splits {@code amount} evenly over {@code outputCount} outputs, rolling each output's shortfall
     * over to the next one so a partially filled output does not silently swallow the difference.
     *
     * <p>Mirrors {@code FEP2PTunnelPart.receiveEnergy}, but as a pure function: the caller supplies the
     * acceptance behaviour per output, which keeps the arithmetic verifiable by unit tests (plan §8.2).
     *
     * @param acceptor called once per output with the output index and the amount offered to it; must
     *                 return how much that output actually accepted
     * @return the total amount accepted
     */
    static double distributeEvenly(double amount, int outputCount, ToDoubleBiFunction<Integer, Double> acceptor) {
        if (outputCount <= 0 || amount <= 0) {
            return 0;
        }

        double total = 0;
        final double amountPerOutput = amount / outputCount;
        double overflow = amountPerOutput == 0 ? amount : amount % amountPerOutput;

        for (var index = 0; index < outputCount; index++) {
            final double toSend = amountPerOutput + overflow;
            final double accepted = acceptor.applyAsDouble(index, toSend);
            overflow = toSend - accepted;
            total += accepted;
        }

        return total;
    }

    /**
     * Exposed on the input side: accepts ember and distributes it across all output sides.
     */
    private class InputEmberHandler extends HandlerBase {

        @Override
        public double addAmount(double amount, boolean doAdd) {
            final var outputs = getOutputs();
            // Even split with shortfall roll-over; the algorithm is shared/static so the "nothing is
            // dropped inside the tunnel" property is unit-testable (plan §8.2).
            double total = distributeEvenly(amount, outputs.size(), (index, toSend) -> {
                try (CapabilityGuard guard = outputs.get(index).getAdjacentCapability()) {
                    return guard.get().addAmount(toSend, doAdd);
                }
            });

            if (doAdd && total > 0) {
                // Energy-cost decision (plan §5-F3): Ember has no counterpart in AE2's PowerUnits, so
                // the FE-style energy tax cannot be applied. Charge AE2's generic P2P transport tax
                // instead, measured in ember operations — the same mechanism AE2 uses for key types
                // that are not FE.
                deductTransportCost((long) total, EmberKeyType.TYPE);
            }
            return total;
        }

        @Override
        public double removeAmount(double amount, boolean doRemove) {
            return 0; // the input side never provides ember
        }

        @Override
        public double getEmber() {
            double total = 0;
            for (EmberP2PTunnelPart output : getOutputs()) {
                try (CapabilityGuard guard = output.getAdjacentCapability()) {
                    total += guard.get().getEmber();
                }
            }
            return total;
        }

        @Override
        public double getEmberCapacity() {
            double total = 0;
            for (EmberP2PTunnelPart output : getOutputs()) {
                try (CapabilityGuard guard = output.getAdjacentCapability()) {
                    total += guard.get().getEmberCapacity();
                }
            }
            return total;
        }
    }

    /**
     * Exposed on each output side: lets an adjacent machine pull ember from the input side.
     */
    private class OutputEmberHandler extends HandlerBase {

        @Override
        public double addAmount(double amount, boolean doAdd) {
            return 0; // the output side never accepts ember
        }

        @Override
        public double removeAmount(double amount, boolean doRemove) {
            try (CapabilityGuard input = getInputCapability()) {
                final double removed = input.get().removeAmount(amount, doRemove);
                if (doRemove && removed > 0) {
                    // Same energy-cost decision as InputEmberHandler.addAmount above.
                    deductTransportCost((long) removed, EmberKeyType.TYPE);
                }
                return removed;
            }
        }

        @Override
        public double getEmber() {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().getEmber();
            }
        }

        @Override
        public double getEmberCapacity() {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().getEmberCapacity();
            }
        }
    }

    /**
     * A tunnel holds no state of its own — everything is derived from the adjacent capabilities — so the
     * persistence and mutation members of {@link IEmberCapability} are no-ops for all tunnel handlers.
     */
    private abstract static class HandlerBase implements IEmberCapability {

        /**
         * Tunnel handlers are never exposed as capabilities themselves — the part wraps them in its own
         * {@code LazyOptional} — so this is always empty.
         */
        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            return LazyOptional.empty();
        }

        @Override
        public void setEmber(double ember) {
        }

        @Override
        public void setEmberCapacity(double emberCapacity) {
        }

        @Override
        public void writeToNBT(CompoundTag nbt) {
        }

        @Override
        public void onContentsChanged() {
        }

        @Override
        public void invalidate() {
        }

        @Override
        public CompoundTag serializeNBT() {
            return new CompoundTag();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
        }
    }

    private static final class NullEmberCapability extends HandlerBase {

        @Override
        public double getEmber() {
            return 0;
        }

        @Override
        public double getEmberCapacity() {
            return 0;
        }

        @Override
        public double addAmount(double amount, boolean doAdd) {
            return 0;
        }

        @Override
        public double removeAmount(double amount, boolean doRemove) {
            return 0;
        }
    }
}
