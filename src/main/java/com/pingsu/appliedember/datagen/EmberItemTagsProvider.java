package com.pingsu.appliedember.datagen;

import appeng.api.features.P2PTunnelAttunement;
import com.pingsu.appliedember.content.p2p.EmberP2PContent;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Item tags: the P2P attunement trigger tag that {@code EmberP2PContent.registerAttunement} wires up,
 * populated with Embers' Ember machines so a player can right-click the tunnel with one to attune it.
 *
 * <p>Entries are added optionally so that a future Embers version renaming an item only drops that
 * single trigger instead of producing a tag error.
 */
public class EmberItemTagsProvider extends ItemTagsProvider {

    private static final String[] EMBER_TRIGGER_ITEMS = {
            "ember_bore",
            "ember_activator",
            "ember_emitter",
            "ember_receiver",
            "ember_funnel",
            "ember_injector",
            "ember_ejector",
            "ember_siphon",
            "ember_relay",
            "ember_dial",
            "ember_jar",
            "ember_cartridge",
    };

    public EmberItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
            CompletableFuture<TagsProvider.TagLookup<Block>> blockTags,
            @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, AppliedEmberDataGenerators.modId(), existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var tag = tag(P2PTunnelAttunement.getAttunementTag(EmberP2PContent.EMBER_P2P_TUNNEL.get()));
        for (String path : EMBER_TRIGGER_ITEMS) {
            tag.addOptional(ResourceLocation.fromNamespaceAndPath("embers", path));
        }
    }
}
