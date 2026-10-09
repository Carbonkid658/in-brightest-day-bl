package dev.amble.core.walls;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class WallGeometry {
    private static final double TRACE_STEP = 0.35;

    public static int width(int size) {
        return size * 2 + 1;
    }

    public static int height(int size) {
        return size + 2;
    }

    public static List<BlockPos> cells(Vec3 base, float yaw, int size) {
        float radians = yaw * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(radians), 0.0, -Mth.sin(radians));
        double halfWidth = width(size) / 2.0;
        int baseY = Mth.floor(base.y + 1.0E-3);

        Set<BlockPos> cells = new LinkedHashSet<>();
        for (double u = -halfWidth + TRACE_STEP * 0.5; u < halfWidth; u += TRACE_STEP) {
            Vec3 point = base.add(right.scale(u));
            for (int row = 0; row < height(size); row++) {
                cells.add(new BlockPos(Mth.floor(point.x), baseY + row, Mth.floor(point.z)));
            }
        }

        List<BlockPos> ordered = new ArrayList<>(cells);
        BlockPos center = BlockPos.containing(base);
        ordered.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return ordered;
    }

    private WallGeometry() {}
}
