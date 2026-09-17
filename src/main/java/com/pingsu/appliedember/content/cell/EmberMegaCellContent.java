package com.pingsu.appliedember.content.cell;

import appeng.api.client.StorageCellModels;
import appeng.items.storage.StorageTier;
import appeng.items.tools.powered.PortableCellItem;
import appeng.menu.me.common.MEStorageMenu;
import com.pingsu.appliedember.AppliedEmber;
import com.pingsu.appliedember.AppliedEmberResources;
import com.pingsu.appliedember.me.key.EmberKeyType;
import gripe._90.megacells.definition.MEGAItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The mega-tier ember cell line — the MEGA Cells compat layer (1m / 4m / 16m / 64m / 256m ember
 * storage cells and portable cells).
 *
 * <p>MEGA Cells is an <em>optional</em> dependency: every caller must gate access to this class
 * behind {@code ModList.get().isLoaded("megacells")} (see {@code AppliedEmber}, the datagen
 * providers). This class is only class-loaded when the mod is present, which is what makes the
 * direct references to {@link MEGAItems} safe.
 *
 * <p>The line mirrors {@link EmberCellContent}, continuing the ladder where AE2's stops: the tiers,
 * capacities and idle drains are MEGA Cells' own (indexes 6–10, drain = index × 0.5, per
 * {@code MEGAItems#tier}), and the cells are built from <em>MEGA Cells'</em> storage components
 * ({@code megacells:cell_component_<tier>}), so crafting and clearing a cell hands MEGA Cells items
 * back and forth. The ember housing stays this mod's own — it is what the alchemy recipe produces.
 *
 * <p>The visuals follow the base line's structure with this mod's own art: the storage cell model is
 * the shared ember housing plus a per-tier side overlay ({@code storage_cell_side_<tier>}) and AE2's
 * LED, the portable cell keeps the ember-toned screen/housing with a per-tier portable stripe
 * ({@code portable_cell_side_<tier>}), and the drive-slot faces are baked from housing + overlay by
 * {@code tools/gen-drive-faces.ps1} ({@code block/drive/embers_cell_<tier>}).
 *
 * <p>Recipes and advancements for the line live hand-written under
 * {@code data/appliedember/recipes} and {@code data/appliedember/advancements}, guarded by a
 * {@code forge:mod_loaded} condition so they skip silently when MEGA Cells is absent.
 */
public final class EmberMegaCellContent {

    /** Modid of the optional dependency this content line requires. */
    public static final String MEGA_MODID = "megacells";

    /** AE2's customary per-type overhead, in bytes (same as the base cell line). */
    private static final int BYTES_PER_TYPE = 8;
    /** {@code EmberKey} has no NBT variants, so every cell holds exactly one type. */
    private static final int TOTAL_TYPES = 1;
    /** Ember-orange tint for the portable cell LED (same as the base cell line). */
    private static final int PORTABLE_CELL_COLOR = 0xD97A00;

    /**
     * One mega tier: the same data {@link EmberCellContent.Tier} carries, plus the {@link StorageTier}
     * MEGA Cells' portable cells use (kept instead of re-deriving index/bytes/drain).
     */
    public record MegaTier(String suffix, int index, int kilobytes, double idleDrain, ItemLike megaComponent,
            StorageTier portableTier) {

        public String cellId() {
            return "ember_storage_cell_" + suffix;
        }

        /** Registry id of this tier's portable cell. */
        public String portableCellId() {
            return "portable_ember_cell_" + suffix;
        }

        /**
         * This mod's per-tier overlay, used as the middle layer of this tier's cell model and baked
         * into the drive face (the base line uses the same structure with its own k-tier art).
         */
        public ResourceLocation sideTexture() {
            return AppliedEmberResources.id("item/storage_cell_side_" + suffix);
        }

        /** This mod's per-tier portable stripe, used as the tier overlay of the portable cell model. */
        public ResourceLocation portableSideTexture() {
            return AppliedEmberResources.id("item/portable_cell_side_" + suffix);
        }

        /** Drive-slot model for this tier (see {@link #registerDriveModels()}). */
        public ResourceLocation driveModel() {
            return AppliedEmberResources.id("block/drive/embers_cell_" + suffix);
        }
    }

    /**
     * MEGA Cells' tier ladder, with its own indexes, capacities and idle drains
     * ({@code MEGAItems#tier}: bytes = 4^(index−1) KiB, drain = index × 0.5).
     */
    private static final List<MegaTier> TIERS = List.of(
            new MegaTier("1m", 6, 1024, 3.0D, MEGAItems.CELL_COMPONENT_1M, MEGAItems.TIER_1M),
            new MegaTier("4m", 7, 4096, 3.5D, MEGAItems.CELL_COMPONENT_4M, MEGAItems.TIER_4M),
            new MegaTier("16m", 8, 16384, 4.0D, MEGAItems.CELL_COMPONENT_16M, MEGAItems.TIER_16M),
            new MegaTier("64m", 9, 65536, 4.5D, MEGAItems.CELL_COMPONENT_64M, MEGAItems.TIER_64M),
            new MegaTier("256m", 10, 262144, 5.0D, MEGAItems.CELL_COMPONENT_256M, MEGAItems.TIER_256M));

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AppliedEmber.MODID);

    private static final Map<String, RegistryObject<Item>> CELLS = registerCells();
    private static final Map<String, RegistryObject<Item>> PORTABLE_CELLS = registerPortableCells();

    private EmberMegaCellContent() {
    }

    private static Map<String, RegistryObject<Item>> registerCells() {
        Map<String, RegistryObject<Item>> cells = new LinkedHashMap<>();
        for (MegaTier tier : TIERS) {
            cells.put(tier.suffix(), ITEMS.register(tier.cellId(),
                    () -> new EmberStorageCellItem(
                            new Item.Properties().stacksTo(1),
                            tier.megaComponent(),
                            EmberCellContent.EMBER_CELL_HOUSING.get(),
                            tier.idleDrain(),
                            tier.kilobytes(),
                            BYTES_PER_TYPE)));
        }
        return Map.copyOf(cells);
    }

    private static Map<String, RegistryObject<Item>> registerPortableCells() {
        Map<String, RegistryObject<Item>> cells = new LinkedHashMap<>();
        for (MegaTier tier : TIERS) {
            cells.put(tier.suffix(), ITEMS.register(tier.portableCellId(),
                    () -> new PortableCellItem(
                            EmberKeyType.TYPE,
                            TOTAL_TYPES,
                            MEStorageMenu.PORTABLE_ITEM_CELL_TYPE,
                            tier.portableTier(),
                            new Item.Properties().stacksTo(1),
                            PORTABLE_CELL_COLOR)));
        }
        return Map.copyOf(cells);
    }

    /** Mega storage tiers in ascending order, for datagen. Only meaningful when MEGA Cells is loaded. */
    public static List<MegaTier> tiers() {
        return TIERS;
    }

    public static RegistryObject<Item> cell(String suffix) {
        return CELLS.get(suffix);
    }

    /** Portable cell of the given tier (same suffixes as {@link #cell(String)}). */
    public static RegistryObject<Item> portableCell(String suffix) {
        return PORTABLE_CELLS.get(suffix);
    }

    /** Every item this class registers, for the creative tab. */
    public static List<Supplier<? extends Item>> allItems() {
        List<Supplier<? extends Item>> items = new ArrayList<>();
        items.addAll(CELLS.values());
        items.addAll(PORTABLE_CELLS.values());
        return List.copyOf(items);
    }

    /** Registers the item deferred register. Call only when MEGA Cells is loaded. */
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    /**
     * Registers the model used to render these cells while inserted in a drive. The faces are baked
     * from the ember housing plus this mod's per-tier overlays by {@code tools/gen-drive-faces.ps1},
     * the same way the base line's faces are. The same timing rules as
     * {@link EmberCellContent#registerDriveModels()} apply: call right after the item registry is
     * filled (see that method for why client setup is too late), and only when MEGA Cells is loaded.
     */
    public static void registerDriveModels() {
        for (MegaTier tier : TIERS) {
            StorageCellModels.registerModel(CELLS.get(tier.suffix()).get(), tier.driveModel());
            StorageCellModels.registerModel(PORTABLE_CELLS.get(tier.suffix()).get(), tier.driveModel());
        }
    }
}
