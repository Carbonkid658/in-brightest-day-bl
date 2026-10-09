package dev.amble.core.drill;

import dev.amble.core.BrightestDayBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class DrillGeometry {
    public static final double REACH = 5.0;

    public static int side(int size) {
        return size * 2 - 1;
    }

    public static @Nullable BlockHitResult target(Level level, Player player, Vec3 eye, Vec3 look) {
        HitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(REACH)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK ? blockHit : null;
    }

    public static List<BlockPos> face(BlockPos center, Direction direction, int size) {
        int radius = size - 1;
        Direction.Axis axis = direction.getAxis();
        List<BlockPos> cells = new ArrayList<>((radius * 2 + 1) * (radius * 2 + 1));
        cells.add(center);
        for (int a = -radius; a <= radius; a++) {
            for (int b = -radius; b <= radius; b++) {
                if (a == 0 && b == 0) continue;
                cells.add(switch (axis) {
                    case X -> center.offset(0, a, b);
                    case Y -> center.offset(a, 0, b);
                    case Z -> center.offset(a, b, 0);
                });
            }
        }
        return cells;
    }

    public static boolean drillable(BlockGetter level, BlockPos pos, BlockState state) {
        return !state.isAir()
                && !(state.getBlock() instanceof LiquidBlock)
                && !state.is(Blocks.BUBBLE_COLUMN)
                && !state.is(BrightestDayBlocks.HARD_LIGHT)
                && !state.is(BrightestDayBlocks.CONSTRUCT_LIGHT)
                && state.getDestroySpeed(level, pos) >= 0.0F;
    }

    private DrillGeometry() {}
}
