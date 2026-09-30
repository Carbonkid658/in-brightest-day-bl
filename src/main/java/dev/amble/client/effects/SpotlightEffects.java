package dev.amble.client.effects;

import dev.amble.config.BrightestDayConfig;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.CorpsColors;
import dev.amble.core.ringpowers.impl.LightRingPower;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class SpotlightEffects {
    private static final double SPACING = 0.6;
    private static final double MAX_SPREAD = 1.2;
    private static final double SPREAD_PER_BLOCK = 0.05;
    private static final int RING_VOXELS = 4;
    private static final float VOXEL_SIZE = 1.5F * VoxelRenderer.PIXEL;
    private static final float CONE_ALPHA = 0.2F;
    private static final float SPOT_SIZE = 5.0F * VoxelRenderer.PIXEL;

    public static void init() {
        LevelRenderEvents.COLLECT_SUBMITS.register(SpotlightEffects::render);
    }

    private static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 camera = context.levelState().cameraRenderState.pos;
        double range = BrightestDayConfig.get().spotlightRange;

        for (AbstractClientPlayer player : client.level.players()) {
            if (!LightRingPower.isEmitting(player) || !PowerRingItem.hasCharge(player) || player.isInvisible()) continue;

            HitResult hit = player.pick(range, partialTicks, false);
            Vec3 from = BlastEffects.hand(player, partialTicks);
            Vec3 to = hit.getLocation();
            Vec3 path = to.subtract(from);
            double length = path.length();
            if (length < 1.0E-3) continue;

            Vec3 direction = path.scale(1.0 / length);
            Vec3 reference = Math.abs(direction.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
            Vec3 side = direction.cross(reference).normalize();
            Vec3 up = direction.cross(side);
            int color = ARGB.opaque(CorpsColors.of(player));
            float time = player.tickCount + partialTicks;
            double spread = Math.min(length * SPREAD_PER_BLOCK, MAX_SPREAD);
            float half = VoxelRenderer.snapSize(VOXEL_SIZE * 0.5F);

            List<ShieldEffects.Voxel> voxels = new ArrayList<>();
            int count = Math.max(Mth.floor(length / SPACING), 1);
            for (int i = 1; i <= count; i++) {
                double t = (double) i / count;
                Vec3 center = from.add(path.scale(t));
                double radius = spread * t;
                for (int k = 0; k < RING_VOXELS; k++) {
                    double angle = (double) k / RING_VOXELS * Mth.TWO_PI + time * 0.05 + i * 0.7;
                    Vec3 point = center.add(side.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
                    voxels.add(new ShieldEffects.Voxel(VoxelRenderer.snap(point), half, color));
                }
            }
            if (hit.getType() == HitResult.Type.BLOCK) {
                voxels.add(new ShieldEffects.Voxel(VoxelRenderer.snap(to), VoxelRenderer.snapSize(SPOT_SIZE * 0.5F), VoxelRenderer.toWhite(color, 0.6F)));
            }
            ShieldEffects.submit(context, camera, voxels, CONE_ALPHA);
        }
    }

    private SpotlightEffects() {}
}
