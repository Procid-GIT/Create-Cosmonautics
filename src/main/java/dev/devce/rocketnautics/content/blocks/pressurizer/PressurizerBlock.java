package dev.devce.rocketnautics.content.blocks.pressurizer;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import dev.devce.rocketnautics.registry.RocketBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

public class PressurizerBlock extends DirectionalBlock implements IBE<PressurizerBlockEntity> {
    public static final MapCodec<PressurizerBlock> CODEC = simpleCodec(PressurizerBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    @Override
    protected MapCodec<? extends DirectionalBlock> codec() { return CODEC; }

    public PressurizerBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    public Class<PressurizerBlockEntity> getBlockEntityClass() {
        return PressurizerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PressurizerBlockEntity> getBlockEntityType() {
        return RocketBlockEntities.PRESSURIZER.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(FACING, POWERED));
    }

    @javax.annotation.Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PressurizerBlockEntity(RocketBlockEntities.PRESSURIZER.get(), pos, state);
    }
}
