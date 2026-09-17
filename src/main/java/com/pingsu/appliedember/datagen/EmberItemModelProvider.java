package com.pingsu.appliedember.datagen;

import com.pingsu.appliedember.content.cell.EmberCellContent;
import com.pingsu.appliedember.content.cell.EmberMegaCellContent;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.fml.ModList;

/**
 * Item models for the F1 cell line and the F3 P2P tunnel part. Model file names mirror the registry
 * ids declared in {@code EmberCellContent} / {@code EmberP2PContent}.
 *
 * <p>Each drive cell is composed from the supplied art: the shared housing, the tier's side overlay and
 * AE2's LED — the MEGA-tier cells (generated only when MEGA Cells is in the data run) with this mod's
 * own m-tier art, see {@code EmberMegaCellContent}. Textures are not generated, and neither is the
 * part model
 * {@code models/part/p2p/p2p_tunnel_ember.json} (a single static model reusing AE2's part base with this
 * mod's ember band texture), so both stay hand-written resources.
 */
public class EmberItemModelProvider extends ItemModelProvider {

    public EmberItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, AppliedEmberDataGenerators.modId(), existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // The housing is an ordinary item (it is what the alchemy recipe produces); its texture is the
        // same art the cells use as layer 0.
        withExistingParent("ember_cell_housing", mcLoc("item/generated"))
                .texture("layer0", EmberCellContent.housingTexture());

        for (EmberCellContent.Tier tier : EmberCellContent.tiers()) {
            var cell = withExistingParent(tier.cellId(), mcLoc("item/generated"));
            cell.texture("layer0", EmberCellContent.housingTexture());
            cell.texture("layer1", tier.sideTexture());
            cell.texture("layer2", EmberBlockStateProvider.ae2("item/storage_cell_led"));
        }

        // The portable cells mirror AE2's four-layer portable model: screen, LED, housing and the
        // tier's side overlay — with the supplied ember-toned screen/housing in the first and third
        // slots and AE2's per-tier portable side art in the tier slot.
        for (EmberCellContent.Tier tier : EmberCellContent.tiers()) {
            var portable = withExistingParent(tier.portableCellId(), mcLoc("item/generated"));
            portable.texture("layer0", EmberCellContent.portableScreenTexture());
            portable.texture("layer1", EmberBlockStateProvider.ae2("item/portable_cell_led"));
            portable.texture("layer2", EmberCellContent.portableHousingTexture());
            portable.texture("layer3", tier.portableSideTexture());
        }

        // The MEGA-tier ember cells use the same model structure as the base line — ember housing,
        // this mod's per-tier side overlay, AE2's LED — with the m-tier art. Only generated when
        // MEGA Cells is part of the data run (it is a runtimeOnly dependency).
        if (ModList.get().isLoaded(EmberMegaCellContent.MEGA_MODID)) {
            for (EmberMegaCellContent.MegaTier tier : EmberMegaCellContent.tiers()) {
                var cell = withExistingParent(tier.cellId(), mcLoc("item/generated"));
                cell.texture("layer0", EmberCellContent.housingTexture());
                cell.texture("layer1", tier.sideTexture());
                cell.texture("layer2", EmberBlockStateProvider.ae2("item/storage_cell_led"));

                var portable = withExistingParent(tier.portableCellId(), mcLoc("item/generated"));
                portable.texture("layer0", EmberCellContent.portableScreenTexture());
                portable.texture("layer1", EmberBlockStateProvider.ae2("item/portable_cell_led"));
                portable.texture("layer2", EmberCellContent.portableHousingTexture());
                portable.texture("layer3", tier.portableSideTexture());
            }
        }

        // P2P tunnel parts reuse AE2's part item model and only override the band texture.
        withExistingParent("ember_p2p_tunnel", EmberBlockStateProvider.ae2("item/p2p_tunnel_base"))
                .texture("type", modLoc("block/aekey/ember"));
    }
}
