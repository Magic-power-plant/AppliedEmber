package com.pingsu.appliedember.compat.ae2.tooltip;

import appeng.api.integrations.igtooltip.BaseClassRegistration;
import appeng.api.integrations.igtooltip.TooltipProvider;
import com.pingsu.appliedember.content.ember.MEEmberCellBlock;
import com.pingsu.appliedember.content.ember.MEEmberCellEntity;

@SuppressWarnings("UnstableApiUsage")
public class MEEmberCellTooltipProvider implements TooltipProvider {
    @Override
    public void registerBlockEntityBaseClasses(BaseClassRegistration registration) {
        registration.addBaseBlockEntity(MEEmberCellEntity.class, MEEmberCellBlock.class);
    }
}
