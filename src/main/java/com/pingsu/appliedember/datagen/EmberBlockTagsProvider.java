package com.pingsu.appliedember.datagen;

import com.pingsu.appliedember.content.ember.EmberContent;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Block tags: the ME Ember Cell is a pickaxe-mineable metal block, matching the pre-datagen
 * hand-written tag.
 */
public class EmberBlockTagsProvider extends BlockTagsProvider {

    public EmberBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
            @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, AppliedEmberDataGenerators.modId(), existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(EmberContent.ME_EMBER_CELL.get());
    }
}
