package com.pingsu.appliedember.me.strategy;

import appeng.api.behaviors.ContainerItemStrategy;
import appeng.api.config.Actionable;
import appeng.api.stacks.GenericStack;
import com.pingsu.appliedember.me.key.EmberKey;
import com.pingsu.appliedember.me.storage.EmberStorageAmounts;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.power.IEmberCapability;
import com.rekindled.embers.datagen.EmbersSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Lets ME terminals fill and empty Embers' handheld ember containers (ember jar / cartridge / copper
 * cell) straight from the network, AE2's built-in container-item interaction: right-click a terminal
 * entry with a filled container to dump it into the network, left-click / shift to fill the carried
 * container from the network.
 *
 * <p>Mirrors AE2's own {@code appeng.api.behaviors.FluidContainerItemStrategy} (bucket handling), with
 * two ember-specific differences:
 *
 * <ul>
 * <li>The context is the resolved {@link IEmberCapability} itself. Embers' item capabilities are bound
 * to the stack and mutate it in place (the jar stays a jar, only its NBT changes), so there is no
 * "copy the stack, swap the container item" dance like fluid buckets need.</li>
 * <li>An <em>empty</em> container is still a valid fill context as long as it reports a positive
 * capacity — that is exactly the case the left-click fill path needs to offer.</li>
 * </ul>
 *
 * <p>All amount math delegates to the capability's own simulate/modulate path (plan 4.1), so Embers'
 * per-container acceptance rules stay authoritative; AE2's {@code long} amounts are converted with
 * {@link EmberStorageAmounts}.
 */
@SuppressWarnings("UnstableApiUsage")
public final class EmberContainerItemStrategy implements ContainerItemStrategy<EmberKey, IEmberCapability> {

    public static final EmberContainerItemStrategy INSTANCE = new EmberContainerItemStrategy();

    private EmberContainerItemStrategy() {
    }

    @Nullable
    @Override
    public GenericStack getContainedStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return containedStack(findEmberCapability(stack));
    }

    @Nullable
    @Override
    public IEmberCapability findCarriedContext(Player player, AbstractContainerMenu menu) {
        return findFillableContext(menu.getCarried());
    }

    @Nullable
    @Override
    public IEmberCapability findPlayerSlotContext(Player player, int slot) {
        return findFillableContext(player.getInventory().getItem(slot));
    }

    @Override
    public long extract(IEmberCapability context, EmberKey what, long amount, Actionable mode) {
        double removed = context.removeAmount(
                (double) EmberStorageAmounts.nonNegative(amount),
                mode == Actionable.MODULATE);
        return EmberStorageAmounts.saturatingLong(removed);
    }

    @Override
    public long insert(IEmberCapability context, EmberKey what, long amount, Actionable mode) {
        double accepted = context.addAmount(
                (double) EmberStorageAmounts.nonNegative(amount),
                mode == Actionable.MODULATE);
        return EmberStorageAmounts.saturatingLong(accepted);
    }

    @Override
    public void playFillSound(Player player, EmberKey what) {
        // Filling the container: ember streams out of the network into the jar, like a receiver.
        player.playNotifySound(EmbersSounds.EMBER_RECEIVE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public void playEmptySound(Player player, EmberKey what) {
        // Emptying the container: ember streams out of the jar into the network, like an emitter.
        player.playNotifySound(EmbersSounds.EMBER_EMIT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Nullable
    @Override
    public GenericStack getExtractableContent(IEmberCapability context) {
        return containedStack(context);
    }

    /**
     * The ember contained in {@code stack}'s capability as a generic stack, or {@code null} when the
     * stack carries no capability or no ember. Same semantics for {@link #getContainedStack} and
     * {@link #getExtractableContent}; kept package-private so it is unit-testable without ItemStacks
     * (plan §8.2).
     */
    @Nullable
    static GenericStack containedStack(@Nullable IEmberCapability emberCapability) {
        if (emberCapability == null || emberCapability.getEmber() <= 0.0D) {
            return null;
        }
        return new GenericStack(EmberKey.KEY, EmberStorageAmounts.saturatingLong(emberCapability.getEmber()));
    }

    /**
     * A container qualifies as a fill target when it has the capability and can hold anything at all;
     * it does not need to contain ember already (an empty jar is exactly what left-click fill exists
     * for).
     */
    @Nullable
    private static IEmberCapability findFillableContext(ItemStack stack) {
        var emberCapability = findEmberCapability(stack);
        return emberCapability != null && emberCapability.getEmberCapacity() > 0.0D
                ? emberCapability
                : null;
    }

    @Nullable
    private static IEmberCapability findEmberCapability(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return stack.getCapability(EmbersCapabilities.EMBER_CAPABILITY).orElse(null);
    }
}
