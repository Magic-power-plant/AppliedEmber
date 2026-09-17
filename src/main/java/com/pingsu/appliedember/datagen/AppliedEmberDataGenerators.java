package com.pingsu.appliedember.datagen;

import com.pingsu.appliedember.AppliedEmber;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Datagen entry point (plan §6 #19 / 4.5: the {@code data} run used to be dead configuration).
 *
 * <p>Regenerate with {@code gradlew runData}; output goes to {@code src/generated/resources}, which is
 * part of the main resource set. Textures and the single hand-written P2P part model
 * ({@code models/part/p2p/p2p_tunnel_ember.json}) are deliberately not generated — see
 * {@link EmberItemModelProvider}.
 *
 * <p>Recipes are not generated either: they are hand-written JSON under
 * {@code src/main/resources/data/appliedember/recipes} (plus the matching unlock advancements under
 * {@code .../advancements/recipes}), so pack authors can read and tweak every recipe — including the
 * Embers alchemy transmutation that produces the cell housing — without a Java recompile.
 */
@Mod.EventBusSubscriber(modid = AppliedEmber.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AppliedEmberDataGenerators {

    private static final String MODID = AppliedEmber.MODID;

    private AppliedEmberDataGenerators() {
    }

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        ExistingFileHelper existingFiles = event.getExistingFileHelper();

        if (event.includeClient()) {
            generator.addProvider(true, new EmberBlockStateProvider(output, existingFiles));
            generator.addProvider(true, new EmberItemModelProvider(output, existingFiles));
            generator.addProvider(true, EmberLanguageProvider.english(output));
            generator.addProvider(true, EmberLanguageProvider.simplifiedChinese(output));
        }

        if (event.includeServer()) {
            var blockTags = new EmberBlockTagsProvider(output, event.getLookupProvider(), existingFiles);
            generator.addProvider(true, blockTags);
            generator.addProvider(true, new EmberItemTagsProvider(output, event.getLookupProvider(),
                    blockTags.contentsGetter(), existingFiles));
            generator.addProvider(true, new EmberLootTableProvider(output));
        }
    }

    static String modId() {
        return MODID;
    }
}
