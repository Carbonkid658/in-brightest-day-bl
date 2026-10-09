package dev.amble.client.effects.attacks.weapon;

import dev.amble.client.effects.ShieldEffects;
import dev.amble.client.effects.VoxelRenderer;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.networking.payloads.s2c.TurretBoltS2CPayload;
import dev.amble.core.networking.payloads.s2c.TurretS2CPayload;
import dev.amble.core.ringpowers.CorpsColors;
import dev.amble.core.team.RingTargets;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class TurretEffects {
    private static final float CELL = 2.0F * VoxelRenderer.PIXEL;
    private static final int POP_TICKS = 8;
    private static final int FADE_TICKS = 6;
    private static final int EXPIRE_GRACE = 20;
    private static final int WARN_TICKS = 40;
    private static final float BOB_AMPLITUDE = 0.08F;
    private static final float BOB_SPEED = 0.12F;
    private static final float IDLE_SPIN = 0.05F;
    private static final float AIM_LERP = 0.35F;
    private static final int BARREL_START = 2;
    private static final int BARREL_END = 6;
    private static final int FINS = 3;
    private static final float FIN_RADIUS = 4.5F;
    private static final float FIN_SPEED = 0.15F;
    private static final int FLASH_TICKS = 3;
    private static final int BOLT_LIFETIME = 40;
    private static final float BOLT_RADIUS = 0.3F;
    private static final float BOLT_SIZE = 3.0F * VoxelRenderer.PIXEL;
    private static final int BOLT_TAIL = 5;
    private static final float BOLT_TAIL_SPACING = 0.18F;
    private static final int SPARK_TICKS = 5;
    private static final int SPARK_VOXELS = 8;
    private static final float SPARK_RADIUS = 0.5F;

    private static final List<int[]> SHAPE = buildShape();
    private static final Map<Integer, ClientTurret> TURRETS = new HashMap<>();
    private static final List<Bolt> BOLTS = new ArrayList<>();
    private static final List<Spark> SPARKS = new ArrayList<>();

    private static final class ClientTurret {
        final Vec3 center;
        final int ownerId;
        final int color;
        final int remaining;
        final @Nullable ClientLevel level;
        Vec3 aim = new Vec3(0.0, 0.0, 1.0);
        Vec3 prevAim = this.aim;
        int targetId = -1;
        int flash;
        int age;
        int fade = -1;

        ClientTurret(TurretS2CPayload payload, @Nullable ClientLevel level) {
            this.center = payload.center();
            this.ownerId = payload.ownerId();
            this.color = ARGB.opaque(payload.color());
            this.remaining = payload.remaining();
            this.level = level;
        }
    }

    private static final class Bolt {
        final int turretId;
        final int targetId;
        final int color;
        final float speed;
        final @Nullable ClientLevel level;
        Vec3 pos;
        Vec3 prevPos;
        int age;

        Bolt(TurretBoltS2CPayload payload, @Nullable ClientLevel level) {
            this.turretId = payload.turretId();
            this.targetId = payload.targetId();
            this.color = ARGB.opaque(payload.color());
            this.speed = payload.speed();
            this.level = level;
            this.pos = payload.from();
            this.prevPos = payload.from();
        }
    }

    private static final class Spark {
        final Vec3 pos;
        final int color;
        int age;

        Spark(Vec3 pos, int color) {
            this.pos = pos;
            this.color = color;
        }
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(TurretS2CPayload.TYPE, (payload, context) -> {
            if (payload.present()) {
                TURRETS.put(payload.id(), new ClientTurret(payload, context.client().level));
                return;
            }
            ClientTurret turret = TURRETS.get(payload.id());
            if (turret != null && turret.fade < 0) turret.fade = 0;
        });
        ClientPlayNetworking.registerGlobalReceiver(TurretBoltS2CPayload.TYPE, (payload, context) -> {
            BOLTS.add(new Bolt(payload, context.client().level));
            ClientTurret turret = TURRETS.get(payload.turretId());
            if (turret == null) return;
            turret.targetId = payload.targetId();
            turret.flash = FLASH_TICKS;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            TURRETS.clear();
            BOLTS.clear();
            SPARKS.clear();
        });
        ClientTickEvents.END_CLIENT_TICK.register(TurretEffects::tick);
        LevelRenderEvents.COLLECT_SUBMITS.register(TurretEffects::render);
    }

    public static void snapshot(Consumer<CustomPacketPayload> out) {
        TURRETS.forEach((id, turret) -> {
            if (turret.fade < 0) out.accept(new TurretS2CPayload(id, turret.ownerId, turret.center, turret.color, Math.max(turret.remaining - turret.age, 0), true));
        });
    }

    private static void tick(Minecraft client) {
        if (client.isPaused()) return;

        Iterator<ClientTurret> turrets = TURRETS.values().iterator();
        while (turrets.hasNext()) {
            ClientTurret turret = turrets.next();
            if (turret.level != client.level) {
                turrets.remove();
                continue;
            }
            turret.age++;
            if (turret.flash > 0) turret.flash--;
            if (turret.fade < 0 && turret.age > turret.remaining + EXPIRE_GRACE) turret.fade = 0;
            if (turret.fade >= 0 && ++turret.fade > FADE_TICKS) {
                turrets.remove();
                continue;
            }
            turret.prevAim = turret.aim;
            turret.aim = turret.aim.lerp(desiredAim(client, turret), AIM_LERP).normalize();
        }

        Iterator<Bolt> bolts = BOLTS.iterator();
        while (bolts.hasNext()) {
            Bolt bolt = bolts.next();
            bolt.prevPos = bolt.pos;
            Entity tracked = client.level == null ? null : client.level.getEntity(bolt.targetId);
            Entity target = tracked == null ? null : RingTargets.nearestPart(tracked, bolt.pos);
            if (bolt.level != client.level || ++bolt.age > BOLT_LIFETIME || target == null || !target.isAlive()) {
                bolts.remove();
                continue;
            }

            Vec3 to = target.getBoundingBox().getCenter().subtract(bolt.pos);
            double distance = to.length();
            if (distance <= bolt.speed + BOLT_RADIUS + target.getBbWidth() * 0.5F) {
                SPARKS.add(new Spark(target.getBoundingBox().getCenter(), boltColor(client, bolt)));
                bolts.remove();
                continue;
            }
            bolt.pos = bolt.pos.add(to.scale(bolt.speed / distance));
        }

        SPARKS.removeIf(spark -> ++spark.age > SPARK_TICKS);
    }

    private static Vec3 desiredAim(Minecraft client, ClientTurret turret) {
        Entity tracked = client.level != null && turret.targetId >= 0 ? client.level.getEntity(turret.targetId) : null;
        if (tracked != null && tracked.isAlive()) {
            Vec3 to = RingTargets.nearestPart(tracked, turret.center).getBoundingBox().getCenter().subtract(turret.center);
            if (to.lengthSqr() > 1.0E-4) return to.normalize();
        }
        turret.targetId = -1;
        float yaw = turret.age * IDLE_SPIN;
        return new Vec3(Mth.sin(yaw), 0.0, Mth.cos(yaw));
    }

    private static void render(LevelRenderContext context) {
        if (TURRETS.isEmpty() && BOLTS.isEmpty() && SPARKS.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 camera = context.levelState().cameraRenderState.pos;

        for (ClientTurret turret : TURRETS.values()) {
            float time = turret.age + partialTicks;
            float scale = Mth.clamp(time / POP_TICKS, 0.0F, 1.0F);
            if (turret.fade >= 0) scale *= 1.0F - Mth.clamp((turret.fade + partialTicks) / FADE_TICKS, 0.0F, 1.0F);
            if (scale <= 0.01F) continue;

            float alpha = 1.0F;
            int left = turret.remaining - turret.age;
            if (turret.fade < 0 && left < WARN_TICKS) alpha = 0.55F + 0.45F * Math.abs(Mth.cos(time * 0.6F));
            ShieldEffects.submit(context, camera, turret(client, turret, time, partialTicks, scale), alpha);
        }

        if (!BOLTS.isEmpty()) {
            List<ShieldEffects.Voxel> voxels = new ArrayList<>();
            for (Bolt bolt : BOLTS) {
                int color = boltColor(client, bolt);
                Vec3 pos = bolt.prevPos.lerp(bolt.pos, partialTicks);
                Vec3 back = bolt.pos.subtract(bolt.prevPos);
                back = back.lengthSqr() < 1.0E-6 ? Vec3.ZERO : back.normalize().reverse();
                voxels.add(new ShieldEffects.Voxel(VoxelRenderer.snap(pos), VoxelRenderer.snapSize(BOLT_SIZE * 0.5F), VoxelRenderer.toWhite(color, 0.85F)));
                for (int i = 1; i <= BOLT_TAIL; i++) {
                    float t = (float) i / (BOLT_TAIL + 1);
                    float half = VoxelRenderer.snapSize(BOLT_SIZE * 0.5F * (1.0F - t * 0.7F));
                    voxels.add(new ShieldEffects.Voxel(VoxelRenderer.snap(pos.add(back.scale(i * BOLT_TAIL_SPACING))), half, VoxelRenderer.toWhite(color, 0.5F * (1.0F - t))));
                }
            }
            ShieldEffects.submit(context, camera, voxels, 1.0F);
        }

        for (Spark spark : SPARKS) {
            float t = (spark.age + partialTicks) / SPARK_TICKS;
            if (t >= 1.0F) continue;
            float radius = SPARK_RADIUS * (1.0F - (1.0F - t) * (1.0F - t));
            float half = VoxelRenderer.snapSize(BOLT_SIZE * 0.5F * (1.0F - 0.6F * t));
            List<ShieldEffects.Voxel> voxels = new ArrayList<>(SPARK_VOXELS);
            for (int i = 0; i < SPARK_VOXELS; i++) {
                float yaw = (float) i / SPARK_VOXELS * Mth.TWO_PI;
                float rise = (i & 1) == 0 ? 0.5F : -0.5F;
                Vec3 offset = new Vec3(Mth.cos(yaw), rise, Mth.sin(yaw)).normalize().scale(radius);
                voxels.add(new ShieldEffects.Voxel(VoxelRenderer.snap(spark.pos.add(offset)), half, VoxelRenderer.toWhite(spark.color, 0.7F * (1.0F - t))));
            }
            ShieldEffects.submit(context, camera, voxels, 1.0F - t * t);
        }
    }

    private static List<ShieldEffects.Voxel> turret(Minecraft client, ClientTurret turret, float time, float partialTicks, float scale) {
        int color = turretColor(client, turret);
        Vec3 center = turret.center.add(0.0, BOB_AMPLITUDE * Mth.sin(time * BOB_SPEED), 0.0);
        float step = CELL * scale;
        float half = VoxelRenderer.snapSize(CELL * 0.45F * scale);

        List<ShieldEffects.Voxel> voxels = new ArrayList<>(SHAPE.size() + FINS + BARREL_END + 2);
        for (int[] cell : SHAPE) {
            Vec3 point = center.add(cell[0] * step, cell[1] * step, cell[2] * step);
            float shine = cell[3] == 1 ? 0.35F : 0.1F + 0.08F * Mth.sin(time * 0.3F + cell[0] + cell[2]);
            voxels.add(new ShieldEffects.Voxel(VoxelRenderer.snap(point), half, VoxelRenderer.toWhite(color, shine)));
        }

        for (int i = 0; i < FINS; i++) {
            float angle = time * FIN_SPEED + i * Mth.TWO_PI / FINS;
            Vec3 point = center.add(Mth.cos(angle) * FIN_RADIUS * step, -3.0F * step, Mth.sin(angle) * FIN_RADIUS * step);
            voxels.add(new ShieldEffects.Voxel(VoxelRenderer.snap(point), half, VoxelRenderer.toWhite(color, 0.45F)));
        }

        Vec3 aim = turret.prevAim.lerp(turret.aim, partialTicks);
        aim = aim.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : aim.normalize();
        float flash = Mth.clamp((turret.flash - partialTicks) / FLASH_TICKS, 0.0F, 1.0F);
        for (int i = BARREL_START; i <= BARREL_END; i++) {
            Vec3 point = center.add(aim.scale(i * step));
            voxels.add(new ShieldEffects.Voxel(VoxelRenderer.snap(point), half, VoxelRenderer.toWhite(color, 0.2F + 0.4F * flash)));
        }
        Vec3 muzzle = center.add(aim.scale((BARREL_END + 1) * step));
        voxels.add(new ShieldEffects.Voxel(VoxelRenderer.snap(muzzle), VoxelRenderer.snapSize(CELL * (0.6F + 0.6F * flash) * scale), VoxelRenderer.toWhite(color, 0.6F + 0.4F * flash)));

        float pulse = 1.0F + 0.12F * Mth.sin(time * 0.25F);
        voxels.add(new ShieldEffects.Voxel(VoxelRenderer.snap(center), VoxelRenderer.snapSize(CELL * 0.9F * scale * pulse), VoxelRenderer.toWhite(color, 0.75F)));
        return voxels;
    }

    private static int turretColor(Minecraft client, ClientTurret turret) {
        if (client.level != null && client.level.getEntity(turret.ownerId) instanceof Player owner && PowerRingItem.getWornCorps(owner).isPresent()) {
            return ARGB.opaque(CorpsColors.of(owner));
        }
        return turret.color;
    }

    private static int boltColor(Minecraft client, Bolt bolt) {
        ClientTurret turret = TURRETS.get(bolt.turretId);
        return turret == null ? bolt.color : turretColor(client, turret);
    }

    private static List<int[]> buildShape() {
        List<int[]> cells = new ArrayList<>();
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                int r = x * x + z * z;
                if (r >= 4 && r <= 10) cells.add(new int[]{x, -3, z, 0});
                if (Math.abs(x) + Math.abs(z) <= 1) cells.add(new int[]{x, -4, z, 0});
            }
        }
        cells.add(new int[]{0, -5, 0, 1});
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    int r = x * x + y * y + z * z;
                    if (r >= 2 && r <= 5 && y >= -1) cells.add(new int[]{x, y, z, 0});
                }
            }
        }
        cells.add(new int[]{2, -2, 0, 1});
        cells.add(new int[]{-2, -2, 0, 1});
        cells.add(new int[]{0, -2, 2, 1});
        cells.add(new int[]{0, -2, -2, 1});
        return List.copyOf(cells);
    }

    private TurretEffects() {}
}
