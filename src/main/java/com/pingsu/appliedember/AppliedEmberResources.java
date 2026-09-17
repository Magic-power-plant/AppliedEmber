package com.pingsu.appliedember;

import net.minecraft.resources.ResourceLocation;

/**
 * Resource-location factory for everything in the {@code appliedember} namespace.
 *
 * <p>Plan 4.2: the {@code externalId(namespace, path)} pass-through helper was removed once
 * {@link com.pingsu.appliedember.me.key.EmberKey} started using this mod's own id (plan #17).
 */
public final class AppliedEmberResources {
    private AppliedEmberResources() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AppliedEmber.MODID, path);
    }
}
