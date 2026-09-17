package com.pingsu.appliedember.datagen;

import com.pingsu.appliedember.content.ember.EmberContent;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;

/**
 * Loot tables for this mod's blocks (plan B2: the block used to fabricate a drop from a
 * {@code getDrops} override because no loot table existed).
 *
 * <p>{@code getKnownBlocks} is narrowed on purpose — the vanilla base class would otherwise demand a
 * loot table for every block in the game.
 */
public class EmberLootTableProvider extends LootTableProvider {

    public EmberLootTableProvider(PackOutput output) {
        super(output, Set.of(), List.of(new SubProviderEntry(EmberBlockLoot::new, LootContextParamSets.BLOCK)));
    }

    private static final class EmberBlockLoot extends BlockLootSubProvider {

        private EmberBlockLoot() {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags());
        }

        @Override
        protected void generate() {
            dropSelf(EmberContent.ME_EMBER_CELL.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(EmberContent.ME_EMBER_CELL.get());
        }
    }
}
