package dev.amble.core.blockentities;

import dev.amble.core.BrightestDayBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GreenLanternBlockEntity extends BlockEntity {
    public GreenLanternBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BrightestDayBlockEntityTypes.GREEN_LANTERN_BLOCK_ENTITY_TYPE, worldPosition, blockState);
    }
}
