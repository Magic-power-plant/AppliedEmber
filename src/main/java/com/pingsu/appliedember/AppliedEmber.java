package com.pingsu.appliedember;

import appeng.api.client.AEKeyRendering;
import appeng.api.client.StorageCellModels;
import appeng.api.ids.AECreativeTabIds;
import com.pingsu.appliedember.client.render.EmberRenderer;
import com.pingsu.appliedember.content.cell.EmberCellContent;
import com.pingsu.appliedember.content.cell.EmberMegaCellContent;
import com.pingsu.appliedember.content.ember.EmberContent;
import com.pingsu.appliedember.content.p2p.EmberP2PContent;
import com.pingsu.appliedember.integration.EmberIntegration;
import com.pingsu.appliedember.me.key.EmberKey;
import com.pingsu.appliedember.me.key.EmberKeyType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Applied Energistics 2 x Embers Rekindled integration.
 *
 * <p>Embers is exposed to AE2 as a first-class {@link appeng.api.stacks.AEKeyType}, so ME networks can
 * store, import and export ember through the usual AE2 automation (import/export buses, interfaces,
 * storage cells). The {@code me_ember_cell} block bridges a grid to neighbouring Ember machines by
 * providing an {@code EmbersCapabilities.EMBER_CAPABILITY} that is backed directly by grid storage.
 *
 * <p>Responsibilities (plan §7): this class is the mod entry point and event hook-up only — content
 * registration lives in {@code content.*}, AE2 wiring in {@link EmberIntegration}.
 */
@Mod(AppliedEmber.MODID)
public class AppliedEmber {

    public static final String MODID = "appliedember";

    private static final Logger LOGGER = LoggerFactory.getLogger("AppliedEmber");

    @SuppressWarnings("removal")
    public AppliedEmber() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        EmberContent.register(modEventBus);
        EmberCellContent.register(modEventBus);
        // MEGA Cells is optional: the mega ember cell line registers only when it is loaded.
        if (ModList.get().isLoaded(EmberMegaCellContent.MEGA_MODID)) {
            EmberMegaCellContent.register(modEventBus);
        }
        EmberP2PContent.register(modEventBus);
        EmberIntegration.register(modEventBus);

        // Part models must be handed to AE2 during pre-initialization, before it freezes the registry
        // while baking the cable bus model (plan §5-F3).
        EmberP2PContent.registerModels();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        // Drive-slot models must sit in AE2's StorageCellModels before the first model bake, and the
        // initial resource reload overlaps with the mod setup phases in large packs (the drive model's
        // dependencies are collected before FMLClientSetupEvent runs) — so client setup is too late.
        // The item register event is the earliest point where the DeferredRegister items exist, and
        // it always completes well before the resource reload. LOWEST priority keeps this behind this
        // mod's own DeferredRegister handlers.
        modEventBus.addListener(EventPriority.LOWEST, false, RegisterEvent.class, this::registerDriveCellModels);

        LOGGER.debug("AppliedEmber ember/AE2 integration registered");
    }

    /**
     * The item registry has just been filled: bind every ember cell item to its drive-slot model.
     * Runs on both sides; {@link StorageCellModels} is a plain static map, so the server-side call
     * is a harmless no-op.
     */
    private void registerDriveCellModels(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.ITEM)) {
            return;
        }
        EmberCellContent.registerDriveModels();
        if (ModList.get().isLoaded(EmberMegaCellContent.MEGA_MODID)) {
            EmberMegaCellContent.registerDriveModels();
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // P2P attunement needs the tunnel part item to exist in the item registry (plan §5-F3).
        EmberP2PContent.registerAttunement();
    }

    /**
     * Adds this mod's items to AE2's creative tab, plus the ME Ember Cell to the vanilla redstone tab
     * it has always been in.
     */
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(EmberContent.ME_EMBER_CELL_ITEM);
        }

        if (event.getTabKey() == AECreativeTabIds.MAIN) {
            for (Supplier<? extends Item> item : appliedEmberItems()) {
                event.accept(item.get());
            }
        }
    }

    private static List<Supplier<? extends Item>> appliedEmberItems() {
        List<Supplier<? extends Item>> items = new ArrayList<>();
        items.add(EmberContent.ME_EMBER_CELL_ITEM);
        items.addAll(EmberCellContent.allItems());
        if (ModList.get().isLoaded(EmberMegaCellContent.MEGA_MODID)) {
            items.addAll(EmberMegaCellContent.allItems());
        }
        items.add(EmberP2PContent.EMBER_P2P_TUNNEL);
        return List.copyOf(items);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            AEKeyRendering.register(EmberKeyType.TYPE, EmberKey.class, EmberRenderer.INSTANCE);
            // Drive-slot visuals are bound in registerDriveCellModels (item register event) — client
            // setup can run after the first model bake, which is too late for StorageCellModels.
            LOGGER.debug("Ember AEKey renderer registered");
        }
    }
}
