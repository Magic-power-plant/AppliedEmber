package com.pingsu.appliedember.content.ember;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.util.AECableType;
import appeng.me.helpers.BlockEntityNodeListener;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.EnumSet;

/**
 * Block entity of the ME Ember Cell: owns the grid node and exposes the grid-backed Ember
 * capability to the six neighbouring blocks.
 *
 * <p>Node lifecycle (plan 4.5) is centralised in {@link #destroyMainNode()} / {@link #onFirstTick()}:
 * the four lifecycle overrides below only decide <em>when</em> the node is torn down or created.
 */
public class MEEmberCellEntity extends BlockEntity implements IGridConnectedBlockEntity {

    /**
     * Ember is pushed to neighbours every this many ticks, i.e. twice a second.
     *
     * <p>Embers machines accept ember continuously but their own transfer logic runs at tick rate, so
     * a two-tick-per-second push is enough to keep adjacent machines fed without doing six
     * capability lookups every tick (plan 4.4: the literal used to be undocumented).
     */
    private static final int EMBER_OUTPUT_INTERVAL = 10;

    /**
     * The ME-side face of the block: the fluix panel side, tracked by {@link MEEmberCellBlock#FACING}.
     * The grid node is only exposed on this side, so channels can connect exclusively from the front
     * the player saw when placing the block. The block has no way to rotate after placement, but the
     * exposure is still re-synced in {@link #setBlockState} so schematic-paste style rotations stay
     * consistent.
     */
    private final IManagedGridNode mainNode;
    private final IActionSource actionSource;
    private boolean saving;
    private int tickCount;

    private final GridBackedEmberCapability capability;
    private final EmberCellExporter exporter;

    public MEEmberCellEntity(BlockPos pos, BlockState blockState) {
        super(EmberContent.ME_EMBER_CELL_ENTITY.get(), pos, blockState);
        this.mainNode = GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setVisualRepresentation(EmberContent.ME_EMBER_CELL.get())
                .setInWorldNode(true)
                .setExposedOnSides(EnumSet.of(frontOf(blockState)))
                .setTagName("proxy");
        this.actionSource = IActionSource.ofMachine(getMainNode()::getNode);
        this.capability = new GridBackedEmberCapability(
                mainNode, actionSource, () -> saving, this::setChanged);
        this.exporter = new EmberCellExporter(capability);
    }

    private static Direction frontOf(BlockState state) {
        return state.getValue(MEEmberCellBlock.FACING);
    }

    private Direction getFront() {
        return frontOf(getBlockState());
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        // Keep the node's exposed side in sync when the facing changes (e.g. schematic paste).
        // ManagedGridNode handles this both before and after the in-world node is created.
        getMainNode().setExposedOnSides(EnumSet.of(frontOf(state)));
    }

    @Override
    public IManagedGridNode getMainNode() {
        return mainNode;
    }

    /**
     * Called by AE2 when the node's persisted state needs to be written.
     *
     * <p>Plan 4.5: {@code BlockEntity.setChanged()} already forwards to
     * {@code Level.blockEntityChanged(worldPosition)} for a server-side block entity, so the extra
     * explicit call that used to be here was redundant.
     */
    @Override
    public void saveChanges() {
        setChanged();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        try {
            // While saving, the capability must not touch the grid (the level may already be tearing
            // down); the `saving` flag makes it fall back to the cached snapshot instead.
            saving = true;
            super.saveAdditional(tag);
            capability.writeToNBT(tag);
        } finally {
            saving = false;
        }
        getMainNode().saveToNBT(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        capability.deserializeNBT(tag);
        getMainNode().loadFromNBT(tag);
    }

    @Override
    public AECableType getCableConnectionType(Direction direction) {
        return direction == getFront() ? AECableType.SMART : AECableType.NONE;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        destroyMainNode();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        GridHelper.onFirstTick(this, MEEmberCellEntity::onFirstTick);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        destroyMainNode();
    }

    /**
     * Tear-down shared by every lifecycle exit path. {@code IManagedGridNode.destroy()} is
     * idempotent, so being called from several of them is safe.
     */
    void destroyMainNode() {
        getMainNode().destroy();
    }

    private void onFirstTick() {
        getMainNode().create(getLevel(), getBlockPos());
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == EmbersCapabilities.EMBER_CAPABILITY) {
            return capability.getCapability(cap, side);
        }
        return super.getCapability(cap, side);
    }

    public int calculateComparatorLevel() {
        GridBackedEmberCapability.EmberSnapshot snapshot = capability.snapshot();
        long capacity = snapshot.stored() + snapshot.free();
        if (capacity <= 0 || snapshot.stored() <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) snapshot.stored() * 15.0D / capacity);
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        // Plan 4.3: refresh the cached snapshot explicitly. This used to be a side effect of calling
        // getEmberCapacity(), which made the intent unreadable.
        capability.refreshCache();
        saveChanges();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (blockEntity instanceof MEEmberCellEntity cell) {
            cell.tickCount++;
            if (cell.tickCount % EMBER_OUTPUT_INTERVAL == 0) {
                cell.outputEmberToAdjacentBlocks();
            }
        }
    }

    private void outputEmberToAdjacentBlocks() {
        if (level == null || !capability.isGridActive()) {
            return;
        }

        exporter.outputToAdjacentBlocks(level, worldPosition);
    }
}
