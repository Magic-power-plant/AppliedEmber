package com.pingsu.appliedember.me.key;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import com.pingsu.appliedember.AppliedEmberResources;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The single, NBT-less Ember key.
 *
 * <p>Plan #17: the key id is now the same {@code appliedember:ember} id as
 * {@link EmberKeyType#TYPE}'s. It used to be {@code embers:ember}, which made the serialized key path
 * look like it belonged to Embers while the channel was registered under this mod.
 */
public class EmberKey extends AEKey {
    public static final AEKey KEY = new EmberKey();

    private static final ResourceLocation ID = AppliedEmberResources.id("ember");

    private EmberKey() {}

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
        return ID;
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
        return EmberKeyType.EMBER_NAME;
    }

    @Override
    public void writeToPacket(FriendlyByteBuf data) {}

    @Override
    public void addDrops(long amount, List<ItemStack> drops, Level level, BlockPos pos) {}
}
