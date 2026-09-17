package com.pingsu.appliedember.gametest;

import appeng.api.config.Actionable;
import appeng.api.features.P2PTunnelAttunement;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyTypes;
import appeng.api.storage.StorageCells;
import appeng.core.definitions.AEItems;
import appeng.items.tools.powered.PortableCellItem;
import com.pingsu.appliedember.AppliedEmber;
import com.pingsu.appliedember.AppliedEmberResources;
import com.pingsu.appliedember.content.cell.EmberCellContent;
import com.pingsu.appliedember.content.cell.EmberMegaCellContent;
import com.pingsu.appliedember.content.ember.EmberContent;
import com.pingsu.appliedember.content.ember.MEEmberCellEntity;
import com.pingsu.appliedember.content.p2p.EmberP2PContent;
import com.pingsu.appliedember.content.p2p.EmberP2PTunnelPart;
import com.pingsu.appliedember.me.key.EmberKey;
import com.pingsu.appliedember.me.key.EmberKeyType;
import com.pingsu.appliedember.me.strategy.EmberExternalStorageStrategy;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.recipe.IAlchemyRecipe;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;

/**
 * In-game verification against a real (headless) Forge server with AE2 and Embers loaded — the part of
 * the plan's acceptance criteria that unit tests cannot reach (plan §5-F1/F3, R4).
 *
 * <p>Run with {@code gradlew runGameTestServer}; no Minecraft client is involved. The template is the
 * all-air structure {@code data/appliedember/structures/empty.nbt}; tests that need world content place
 * it themselves. {@code @PrefixGameTestTemplate(false)} keeps the template name exactly
 * {@code appliedember:empty} (Forge would otherwise prefix the class name).
 */
@GameTestHolder(AppliedEmber.MODID)
@PrefixGameTestTemplate(false)
public final class AppliedEmberGameTests {

    private static final String TEMPLATE = "empty";

    private AppliedEmberGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void emberChannelIsRegisteredWithAe2(GameTestHelper helper) {
        helper.assertTrue(AEKeyTypes.get(AppliedEmberResources.id("ember")) == EmberKeyType.TYPE,
                "the ember key type must be registered under appliedember:ember (plan R4)");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void emberKeyRoundTripsThroughItsGenericTag(GameTestHelper helper) {
        var restored = AEKey.fromTagGeneric(EmberKey.KEY.toTagGeneric());
        helper.assertTrue(restored == EmberKey.KEY,
                "the ember key must survive AE2's generic tag round trip, got " + restored + " (plan R4/#17)");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void meEmberCellExposesTheEmberCapability(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, EmberContent.ME_EMBER_CELL.get());

        var blockEntity = helper.getBlockEntity(new BlockPos(1, 1, 1));
        helper.assertTrue(blockEntity instanceof MEEmberCellEntity,
                "placing the ME Ember Cell must create its block entity, got " + blockEntity);

        var emberCapability = blockEntity.getCapability(EmbersCapabilities.EMBER_CAPABILITY, Direction.UP);
        helper.assertTrue(emberCapability.isPresent(),
                "the ME Ember Cell must expose Embers' ember capability to its neighbours");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void emberStorageCellIsRecognisedByAe2(GameTestHelper helper) {
        for (EmberCellContent.Tier tier : EmberCellContent.tiers()) {
            var stack = new ItemStack(EmberCellContent.cell(tier.suffix()).get());

            helper.assertTrue(StorageCells.isCellHandled(stack),
                    "AE2's built-in BasicCellHandler must recognise the " + tier.suffix()
                            + " ember cell without a custom handler (plan §5-F1)");
            helper.assertTrue(StorageCells.getHandler(stack) != null,
                    "the " + tier.suffix() + " ember cell must resolve to a cell handler");
            helper.assertTrue(StorageCells.getCellInventory(stack, null) != null,
                    "the " + tier.suffix() + " ember cell must expose a cell inventory");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void emberStorageCellStoresAndReturnsEmber(GameTestHelper helper) {
        var stack = new ItemStack(EmberCellContent.cell("1k").get());
        var cell = StorageCells.getCellInventory(stack, null);
        helper.assertTrue(cell != null, "the 1k ember cell must be recognised as a storage cell");

        long simulated = cell.insert(EmberKey.KEY, 2_000_000L, Actionable.SIMULATE, IActionSource.empty());
        helper.assertTrue(simulated == 1_016_000L,
                "a 1k ember cell holds (1024 - 8) * 1000 = 1_016_000 ember, got " + simulated);

        long inserted = cell.insert(EmberKey.KEY, 5_000L, Actionable.MODULATE, IActionSource.empty());
        helper.assertTrue(inserted == 5_000L, "expected to store 5000 ember, stored " + inserted);
        helper.assertTrue(cell.getAvailableStacks().get(EmberKey.KEY) == 5_000L,
                "the stored ember must be readable back from the cell");

        long extracted = cell.extract(EmberKey.KEY, 2_000L, Actionable.MODULATE, IActionSource.empty());
        helper.assertTrue(extracted == 2_000L, "expected to extract 2000 ember, extracted " + extracted);
        helper.assertTrue(cell.getAvailableStacks().get(EmberKey.KEY) == 3_000L,
                "the cell must report the remaining ember after an extraction");
        helper.succeed();
    }

    /** Every tier must follow AE2's capacity formula {@code (kb * 1024 - 8) * 1000}. */
    @GameTest(template = TEMPLATE)
    public static void everyEmberStorageTierHasItsDocumentedCapacity(GameTestHelper helper) {
        for (EmberCellContent.Tier tier : EmberCellContent.tiers()) {
            var stack = new ItemStack(EmberCellContent.cell(tier.suffix()).get());
            var cell = StorageCells.getCellInventory(stack, null);
            helper.assertTrue(cell != null, "the " + tier.suffix() + " ember cell must be a storage cell");

            long expected = (long) (tier.kilobytes() * 1024 - 8) * EmberKeyType.TYPE.getAmountPerByte();
            long simulated = cell.insert(EmberKey.KEY, Long.MAX_VALUE, Actionable.SIMULATE, IActionSource.empty());
            helper.assertTrue(simulated == expected,
                    "the " + tier.suffix() + " ember cell should hold " + expected + " ember, got " + simulated);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void portableEmberCellsRunOnTheEmberChannel(GameTestHelper helper) {
        for (EmberCellContent.Tier tier : EmberCellContent.tiers()) {
            var item = EmberCellContent.portableCell(tier.suffix()).get();
            helper.assertTrue(item instanceof PortableCellItem,
                    "the " + tier.suffix() + " portable ember cell must be a PortableCellItem");

            var portable = (PortableCellItem) item;
            helper.assertTrue(portable.getKeyType() == EmberKeyType.TYPE,
                    "the " + tier.suffix() + " portable cell must carry the ember channel");

            var stack = new ItemStack(portable);
            helper.assertTrue(portable.getBytes(stack) > 0,
                    "the " + tier.suffix() + " portable ember cell must have storage capacity");
            helper.assertTrue(portable.getChargeRate(stack) > 0.0D,
                    "the " + tier.suffix() + " portable ember cell must draw AE power like AE2's own "
                            + "portable cells (plan §5-F1)");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void emberP2PTunnelPartItemCreatesOurPart(GameTestHelper helper) {
        var item = EmberP2PContent.EMBER_P2P_TUNNEL.get();
        helper.assertTrue(item.getPartClass() == EmberP2PTunnelPart.class,
                "the tunnel item must create EmberP2PTunnelPart, got " + item.getPartClass());
        helper.assertTrue(item.createPart() instanceof EmberP2PTunnelPart,
                "instantiating the part from its item must yield our ember tunnel part");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void emberMachinesAttuneTheTunnel(GameTestHelper helper) {
        var emberFunnel = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("embers", "ember_funnel"));
        helper.assertTrue(emberFunnel != null && emberFunnel != Items.AIR,
                "embers:ember_funnel must be registered by the loaded Embers mod");

        var attuned = P2PTunnelAttunement.getTunnelPartByTriggerItem(new ItemStack(emberFunnel));
        helper.assertTrue(attuned.is(EmberP2PContent.EMBER_P2P_TUNNEL.get()),
                "right-clicking a tunnel with an ember machine must attune it to our tunnel, got " + attuned);
        helper.succeed();
    }

    /**
     * Survival obtainability (plan §9.3 item 3: this mod's items used to have no recipes at all): every
     * recipe shipped in {@code data/appliedember/recipes} must be parseable by the server's recipe
     * manager, and the housing must be the Embers alchemy transmutation it is documented to be.
     */
    @GameTest(template = TEMPLATE)
    public static void emberRecipesAreLoadedByTheServer(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();

        var housingId = AppliedEmberResources.id("alchemy/ember_cell_housing");
        var housingRecipe = recipes.byKey(housingId);
        helper.assertTrue(housingRecipe.isPresent(), "the alchemy recipe " + housingId + " must be loaded");
        helper.assertTrue(housingRecipe.get() instanceof IAlchemyRecipe,
                "the housing must be made in Embers' alchemy, got " + housingRecipe.get().getClass());

        var alchemy = (IAlchemyRecipe) housingRecipe.get();
        helper.assertTrue(alchemy.getResultItem().is(EmberCellContent.EMBER_CELL_HOUSING.get()),
                "the alchemy recipe must output the ember cell housing, got " + alchemy.getResultItem());
        helper.assertTrue(alchemy.getCenterInput().test(new ItemStack(AEItems.ITEM_CELL_HOUSING)),
                "the alchemy recipe must consume AE2's ordinary ME cell housing in the exchange tablet");

        for (EmberCellContent.Tier tier : EmberCellContent.tiers()) {
            var id = AppliedEmberResources.id("network/cells/ember_storage_cell_" + tier.suffix());
            helper.assertTrue(recipes.byKey(id).isPresent(),
                    "the " + tier.suffix() + " ember cell must be craftable (" + id + ")");
        }

        for (EmberCellContent.Tier tier : EmberCellContent.tiers()) {
            var id = AppliedEmberResources.id("tools/portable_ember_cell_" + tier.suffix());
            helper.assertTrue(recipes.byKey(id).isPresent(),
                    "the " + tier.suffix() + " portable ember cell must be craftable (" + id + ")");
        }
        helper.assertTrue(recipes.byKey(AppliedEmberResources.id("network/parts/ember_p2p_tunnel")).isPresent(),
                "the ember P2P tunnel must be craftable");
        helper.succeed();
    }

    /**
     * MEGA Cells compat: the mega ember cell line must be registered and craftable when (and only
     * when) MEGA Cells is loaded. Guarded by ModList so the same test class still passes in an
     * environment without the optional dependency.
     */
    @GameTest(template = TEMPLATE)
    public static void megaEmberCellsAreRegisteredAndCraftable(GameTestHelper helper) {
        if (!ModList.get().isLoaded(EmberMegaCellContent.MEGA_MODID)) {
            helper.succeed();
            return;
        }

        var recipes = helper.getLevel().getRecipeManager();
        for (EmberMegaCellContent.MegaTier tier : EmberMegaCellContent.tiers()) {
            var cell = EmberMegaCellContent.cell(tier.suffix());
            helper.assertTrue(ForgeRegistries.ITEMS.containsValue(cell.get()),
                    "the " + tier.suffix() + " mega ember cell must be registered with MEGA Cells loaded");

            var cellId = AppliedEmberResources.id("network/cells/ember_storage_cell_" + tier.suffix());
            helper.assertTrue(recipes.byKey(cellId).isPresent(),
                    "the " + tier.suffix() + " mega ember cell must be craftable (" + cellId + ")");

            var portableId = AppliedEmberResources.id("tools/portable_ember_cell_" + tier.suffix());
            helper.assertTrue(recipes.byKey(portableId).isPresent(),
                    "the " + tier.suffix() + " mega portable ember cell must be craftable (" + portableId + ")");
        }
        helper.succeed();
    }

    /**
     * Plan F2 acceptance, first half: a storage bus facing an AE2 grid device must see no external
     * storage at all, which is what stops the network from mounting itself back onto itself.
     */
    @GameTest(template = TEMPLATE)
    public static void storageBusSeesNoExternalStorageOnAGridDevice(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, EmberContent.ME_EMBER_CELL.get());

        var strategy = new EmberExternalStorageStrategy(
                helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), Direction.UP);

        helper.assertTrue(strategy.createWrapper(false, () -> {
        }) == null,
                "the ME Ember Cell must be invisible to a storage bus (plan F2)");
        helper.succeed();
    }

    /**
     * Plan F2 acceptance, second half: ordinary Ember machines stay visible to the storage bus.
     */
    @GameTest(template = TEMPLATE)
    public static void storageBusStillSeesOrdinaryEmberMachines(GameTestHelper helper) {
        var emberFunnel = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("embers", "ember_funnel"));
        helper.assertTrue(emberFunnel != null && emberFunnel != Blocks.AIR,
                "embers:ember_funnel must be registered by the loaded Embers mod");
        helper.setBlock(1, 1, 1, emberFunnel);

        var blockEntity = helper.getBlockEntity(new BlockPos(1, 1, 1));
        helper.assertTrue(blockEntity != null, "the Embers machine must have a block entity");
        helper.assertTrue(blockEntity.getCapability(EmbersCapabilities.EMBER_CAPABILITY, Direction.UP).isPresent(),
                "embers:ember_funnel must expose Embers' ember capability, otherwise this test proves nothing");

        var strategy = new EmberExternalStorageStrategy(
                helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), Direction.UP);

        helper.assertTrue(strategy.createWrapper(false, () -> {
        }) != null,
                "an ordinary Ember machine must remain visible to a storage bus (plan F2)");
        helper.succeed();
    }

    /**
     * The alchemy query command must answer recipe ids with the pedestal input -> aspectus mapping,
     * and reject ids that are not loaded alchemy recipes.
     */
    @GameTest(template = TEMPLATE)
    public static void alchemyCommandReportsAspectsByRecipeId(GameTestHelper helper) {
        var messages = new ArrayList<Component>();
        var source = new CommandSourceStack(new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                messages.add(message);
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }

            @Override
            public boolean alwaysAccepts() {
                return true;
            }
        }, Vec3.ZERO, Vec2.ZERO, helper.getLevel(), 4, "gametest", Component.literal("gametest"),
                helper.getLevel().getServer(), null);

        var server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(source,
                "appliedember alchemy appliedember:alchemy/ember_cell_housing");
        helper.assertTrue(messages.stream().anyMatch(m -> m.getString().contains("基座")),
                "the alchemy command must list the pedestal inputs for the housing recipe");

        messages.clear();
        server.getCommands().performPrefixedCommand(source, "appliedember alchemy appliedember:nope");
        helper.assertTrue(messages.stream().anyMatch(m -> m.getString().contains("不是一个已加载的炼金配方")),
                "the alchemy command must reject ids that are not loaded alchemy recipes");
        helper.succeed();
    }
}
