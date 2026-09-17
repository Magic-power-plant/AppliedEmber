package com.pingsu.appliedember.datagen;

import com.pingsu.appliedember.content.ember.EmberContent;
import com.pingsu.appliedember.content.ember.MEEmberCellBlock;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * Blockstate plus the block and block-item models.
 *
 * <p>{@code models/block/me_ember_cell.json} is a hand-written multi-element model (copper frame,
 * deep-line parts and a fluix core) kept in {@code src/main/resources}; the model is authored with
 * the fluix core facing north, so each {@link MEEmberCellBlock#FACING} value only needs a Y
 * rotation here. The item model that points at the block model is generated here as well.
 */
public class EmberBlockStateProvider extends BlockStateProvider {

    public EmberBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, AppliedEmberDataGenerators.modId(), existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        var cell = EmberContent.ME_EMBER_CELL.get();
        var cellModel = models().getExistingFile(modLoc("block/me_ember_cell"));
        getVariantBuilder(cell).forAllStates(state -> {
            int yRot = switch (state.getValue(MEEmberCellBlock.FACING)) {
                case NORTH -> 0;
                case EAST -> 90;
                case SOUTH -> 180;
                default -> 270;
            };
            return new ConfiguredModel[] {new ConfiguredModel(cellModel, 0, yRot, false)};
        });
        simpleBlockItem(cell, cellModel);
    }

    /**
     * AE2's namespace, for the models/textures this mod borrows (the drive cell base model and the
     * cell LED overlay).
     */
    static ResourceLocation ae2(String path) {
        return ResourceLocation.fromNamespaceAndPath("ae2", path);
    }
}
