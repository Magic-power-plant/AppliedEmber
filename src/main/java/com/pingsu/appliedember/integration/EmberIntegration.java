package com.pingsu.appliedember.integration;

import appeng.api.behaviors.ContainerItemStrategy;
import appeng.parts.automation.StackWorldBehaviors;
import com.pingsu.appliedember.compat.ae2.storage.EmberCapabilities;
import com.pingsu.appliedember.me.key.EmberKey;
import com.pingsu.appliedember.me.key.EmberKeyType;
import com.pingsu.appliedember.me.strategy.EmberContainerItemStrategy;
import com.pingsu.appliedember.me.strategy.EmberExternalStorageStrategy;
import com.pingsu.appliedember.me.strategy.EmberStorageExportStrategy;
import com.pingsu.appliedember.me.strategy.EmberStorageImportStrategy;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Wires the Embers energy type into AE2: key-type registration, capability registration/attachment,
 * the three stack-world strategies (external storage, export bus, import bus), and the terminal
 * container-item strategy (fill/empty handheld ember jars).
 *
 * <p>Home of {@code AppliedEmber.registerEmberIntegration} in the plan's §7 target structure. Feature
 * registrations added by the plan hook in here as well: F1 cell content and F3 P2P attunement
 * ({@link com.pingsu.appliedember.content.p2p.EmberP2PContent#registerAttunement()}).
 */
public final class EmberIntegration {

    private EmberIntegration() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EmberKeyType::register);
        modEventBus.addListener(EmberCapabilities::register);
        MinecraftForge.EVENT_BUS.addGenericListener(
                BlockEntity.class,
                EventPriority.NORMAL,
                EmberCapabilities::attach
        );

        StackWorldBehaviors.registerExternalStorageStrategy(EmberKeyType.TYPE, EmberExternalStorageStrategy::new);
        StackWorldBehaviors.registerExportStrategy(EmberKeyType.TYPE, EmberStorageExportStrategy::new);
        StackWorldBehaviors.registerImportStrategy(EmberKeyType.TYPE, EmberStorageImportStrategy::new);

        ContainerItemStrategy.register(EmberKeyType.TYPE, EmberKey.class, EmberContainerItemStrategy.INSTANCE);
    }
}
