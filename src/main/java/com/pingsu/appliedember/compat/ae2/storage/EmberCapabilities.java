package com.pingsu.appliedember.compat.ae2.storage;

import appeng.capabilities.Capabilities;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.pingsu.appliedember.AppliedEmberResources;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exposes the ember held in an AE2 machine's generic inventory ({@code Capabilities.GENERIC_INTERNAL_INV})
 * as an Ember storage view, so third-party integrations can read it.
 *
 * <p>Plan 4.2: tightened naming — capability field {@code EMBER_TILE -> GENERIC_INV_EMBER} (the old
 * {@code _TILE} suffix was misleading and easily confused with Embers' own
 * {@code EmbersCapabilities.EMBER_CAPABILITY}) and capability id
 * {@code generic_inv_wrapper -> ember_inv_wrapper} (it used to copy AE2's own id).
 *
 * <p>Plan B4: the exposed type is the read-only {@link IGridEmberView}, replacing the old
 * {@code IAdvancedEmberCapability} adapter that had to throw from its setters and silently no-op its
 * NBT methods.
 *
 * <p>Plan 4.5: the provider is only attached to AE2 grid devices, not to every block entity in the
 * world. Those are the only block entities that can provide a generic internal inventory, and the
 * lookup itself stays lazy ({@code lazyMap}) so nothing is constructed until someone asks.
 */
public final class EmberCapabilities {

    public static final Capability<IGridEmberView> GENERIC_INV_EMBER =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    private EmberCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IGridEmberView.class);
    }

    public static void attach(AttachCapabilitiesEvent<BlockEntity> event) {
        BlockEntity blockEntity = event.getObject();
        if (!(blockEntity instanceof IGridConnectedBlockEntity)) {
            return;
        }
        event.addCapability(
                AppliedEmberResources.id("ember_inv_wrapper"),
                new GenericInventoryEmberProvider(blockEntity)
        );
    }

    private record GenericInventoryEmberProvider(BlockEntity blockEntity) implements ICapabilityProvider {
        @SuppressWarnings("UnstableApiUsage")
        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap != GENERIC_INV_EMBER) {
                return LazyOptional.empty();
            }
            return blockEntity.getCapability(Capabilities.GENERIC_INTERNAL_INV, side)
                    .lazyMap(GenericStackEmberStorage::new)
                    .cast();
        }
    }
}
