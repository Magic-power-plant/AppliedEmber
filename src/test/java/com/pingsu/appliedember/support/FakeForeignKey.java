package com.pingsu.appliedember.support;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import com.pingsu.appliedember.AppliedEmberResources;
import com.pingsu.appliedember.me.key.EmberKeyType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A stand-in for "some other key type" in tests: real key instances (items/fluids) would need a
 * bootstrapped Minecraft registry, which unit tests deliberately avoid.
 */
public final class FakeForeignKey extends AEKey {

    public static final FakeForeignKey INSTANCE = new FakeForeignKey();

    private FakeForeignKey() {
    }

    @Override
    public AEKeyType getType() {
        return EmberKeyType.TYPE;
    }

    @Override
    public AEKey dropSecondary() {
        return this;
    }

    @Override
    public ResourceLocation getId() {
        return AppliedEmberResources.id("fake_foreign_key");
    }

    @Override
    public CompoundTag toTag() {
        return new CompoundTag();
    }

    @Override
    public Object getPrimaryKey() {
        return this;
    }

    @Override
    public Component computeDisplayName() {
        return Component.literal("Fake Foreign Key");
    }

    @Override
    public void writeToPacket(FriendlyByteBuf data) {
    }

    @Override
    public void addDrops(long amount, List<ItemStack> drops, Level level, BlockPos pos) {
    }
}
