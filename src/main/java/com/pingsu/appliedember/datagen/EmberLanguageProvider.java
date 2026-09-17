package com.pingsu.appliedember.datagen;

import com.pingsu.appliedember.content.cell.EmberCellContent;
import com.pingsu.appliedember.content.cell.EmberMegaCellContent;
import com.pingsu.appliedember.content.ember.EmberContent;
import com.pingsu.appliedember.content.p2p.EmberP2PContent;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.fml.ModList;

/**
 * Language entries for every user-visible name this mod adds. One instance per locale; the locale
 * string is the file name under {@code assets/appliedember/lang}.
 */
public class EmberLanguageProvider extends LanguageProvider {

    private final boolean chinese;

    private EmberLanguageProvider(PackOutput output, String locale, boolean chinese) {
        super(output, AppliedEmberDataGenerators.modId(), locale);
        this.chinese = chinese;
    }

    public static EmberLanguageProvider english(PackOutput output) {
        return new EmberLanguageProvider(output, "en_us", false);
    }

    public static EmberLanguageProvider simplifiedChinese(PackOutput output) {
        return new EmberLanguageProvider(output, "zh_cn", true);
    }

    @Override
    protected void addTranslations() {
        addBlock(EmberContent.ME_EMBER_CELL, chinese ? "ME 余烬元件" : "ME Ember Cell");
        add("aekey.appliedember.ember", chinese ? "余烬" : "Ember");

        addItem(EmberCellContent.EMBER_CELL_HOUSING, chinese ? "余烬元件外壳" : "Ember Cell Housing");
        addItem(EmberP2PContent.EMBER_P2P_TUNNEL, chinese ? "余烬 P2P 通道" : "Ember P2P Tunnel");

        for (EmberCellContent.Tier tier : EmberCellContent.tiers()) {
            String suffix = tier.suffix();
            addItem(EmberCellContent.cell(suffix),
                    chinese ? suffix + " ME 余烬存储元件" : suffix + " ME Ember Storage Cell");
            addItem(EmberCellContent.portableCell(suffix),
                    chinese ? suffix + " 便携余烬元件" : suffix + " Portable Ember Cell");
        }

        // The MEGA-tier line only exists when MEGA Cells is loaded (it always is in the data run).
        if (ModList.get().isLoaded(EmberMegaCellContent.MEGA_MODID)) {
            for (EmberMegaCellContent.MegaTier tier : EmberMegaCellContent.tiers()) {
                String suffix = tier.suffix();
                addItem(EmberMegaCellContent.cell(suffix),
                        chinese ? suffix + " MEGA 余烬存储元件" : suffix + " MEGA Ember Storage Cell");
                addItem(EmberMegaCellContent.portableCell(suffix),
                        chinese ? suffix + " MEGA 便携余烬元件" : suffix + " MEGA Portable Ember Cell");
            }
        }

        add("appliedember.attunement.ember",
                chinese ? "将 P2P 通道调谐为余烬" : "Attunes a P2P tunnel to Ember");
    }
}
