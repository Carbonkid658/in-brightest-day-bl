package dev.amble.core.blockentities;

import dev.amble.core.BrightestDayBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LanternBlockEntity extends BlockEntity {
    public LanternBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BrightestDayBlockEntityTypes.LANTERN_BLOCK_ENTITY_TYPE, worldPosition, blockState);
    }
}
