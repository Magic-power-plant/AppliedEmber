package com.pingsu.appliedember.content.ember;

import com.pingsu.appliedember.AppliedEmber;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EmberContent {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AppliedEmber.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AppliedEmber.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AppliedEmber.MODID);

    public static final RegistryObject<MEEmberCellBlock> ME_EMBER_CELL = BLOCKS.register(
            "me_ember_cell",
            () -> new MEEmberCellBlock(Block.Properties.of().strength(2.0F).requiresCorrectToolForDrops().noOcclusion())
    );

    public static final RegistryObject<Item> ME_EMBER_CELL_ITEM = ITEMS.register(
            "me_ember_cell",
            () -> new BlockItem(ME_EMBER_CELL.get(), new Item.Properties())
    );

    public static final RegistryObject<BlockEntityType<MEEmberCellEntity>> ME_EMBER_CELL_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "me_ember_cell",
                    () -> BlockEntityType.Builder.of(MEEmberCellEntity::new, ME_EMBER_CELL.get()).build(null)
            );

    private EmberContent() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
