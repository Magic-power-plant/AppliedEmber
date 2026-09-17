package com.pingsu.appliedember.content.p2p;

import appeng.api.features.P2PTunnelAttunement;
import appeng.api.parts.PartModels;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import com.pingsu.appliedember.AppliedEmber;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * F3: registration holder for the Ember P2P tunnel — part item, part models and attunement.
 */
public final class EmberP2PContent {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AppliedEmber.MODID);

    public static final RegistryObject<PartItem<EmberP2PTunnelPart>> EMBER_P2P_TUNNEL = ITEMS.register(
            "ember_p2p_tunnel",
            () -> new PartItem<>(new Item.Properties(), EmberP2PTunnelPart.class, EmberP2PTunnelPart::new));

    private EmberP2PContent() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    /**
     * Registers the tunnel's part models with AE2.
     *
     * <p>Must run during pre-initialization, i.e. from the mod constructor: AE2 freezes
     * {@link PartModels} when it bakes the cable bus model, and
     * {@link PartModels#registerModels} throws after that.
     */
    public static void registerModels() {
        PartModels.registerModels(PartModelsHelper.createModels(EmberP2PTunnelPart.class));
    }

    /**
     * Attunement: right-clicking an item exposing {@link EmbersCapabilities#EMBER_CAPABILITY} attunes a
     * generic P2P tunnel to Ember, aligned with how AE2's FE tunnel is attuned.
     *
     * <p>The item-tag path is registered as well, so server packs can add trigger items through
     * {@code appliedember:p2p_attunements/ember_p2p_tunnel} (populated in this mod's data tags).
     *
     * <p>Must run after the part item is registered — called from {@code FMLCommonSetupEvent}
     * ({@code registerAttunementApi} is synchronized, no {@code enqueueWork} needed).
     */
    public static void registerAttunement() {
        P2PTunnelAttunement.registerAttunementTag(EMBER_P2P_TUNNEL.get());
        P2PTunnelAttunement.registerAttunementApi(
                EMBER_P2P_TUNNEL.get(),
                EmbersCapabilities.EMBER_CAPABILITY,
                Component.translatable("appliedember.attunement.ember"));
    }
}
