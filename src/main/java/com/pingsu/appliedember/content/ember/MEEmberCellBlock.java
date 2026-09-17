package com.pingsu.appliedember.content.ember;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * The ME Ember Cell block: bridges a grid to neighbouring Ember machines.
 *
 * <p>Plan B2: the {@code getDrops} override that fabricated a drop when the loot table produced
 * nothing has been removed. Drops are now defined by
 * {@code data/appliedember/loot_tables/blocks/me_ember_cell.json}, so any future custom loot
 * behaviour (silk touch variants, component-aware drops, ...) works the standard way instead of
 * being short-circuited.
 *
 * <p>The class-level deprecation suppression covers the {@code BlockBehaviour} hooks overridden
 * below ({@code onRemove}, {@code hasAnalogOutputSignal}, {@code getAnalogOutputSignal}): vanilla
 * marks the block-level methods {@code @Deprecated} in favour of the {@code BlockState} wrappers, but
 * those wrappers just forward to these overrides, so overriding them is still the only hook point
 * (AE2's own blocks do the same).
 */
@SuppressWarnings("deprecation")
public class MEEmberCellBlock extends BaseEntityBlock {
    /**
     * The fluix-panel side of the block, i.e. the side that faces the ME grid. The block model is
     * authored with the panel facing north, so the blockstate only needs Y rotations.
     */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public MEEmberCellBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MEEmberCellEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() == newState.getBlock()) {
            return;
        }

        if (level.getBlockEntity(pos) instanceof MEEmberCellEntity cell) {
            // Shared, idempotent teardown (plan 4.5): the same helper runs from setRemoved() and
            // onChunkUnloaded(), so the node can never be left behind on any removal path.
            cell.destroyMainNode();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MEEmberCellEntity cell) {
            return cell.calculateComparatorLevel();
        }
        return 0;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, EmberContent.ME_EMBER_CELL_ENTITY.get(), MEEmberCellEntity::serverTick);
    }
}
