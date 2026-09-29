package dev.amble.client.flight;

import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.ringpowers.impl.FlightRingPower;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

public final class FlightAnimator {

    public static final RenderStateDataKey<FlightPose> POSE = RenderStateDataKey.create(() -> "brightestday:flight_pose");

    private static final float HOVER_LEAN = 15.0F;
    private static final float MAX_ROLL = 55.0F;
    private static final float CAMERA_ROLL_SCALE = 0.6F;
    private static final float FIRST_PERSON_FOV_BOOST = 0.4F;
    private static final float THIRD_PERSON_FOV_BOOST = 0.9F;
    public static final float MAX_FOV_MODIFIER = 2.0F;
    public static final float MAX_FOV = 120.0F;
    private static final double TRAIL_SPEED = 0.6;
    private static final double SONIC_BOOM_SPEED = 3.0;

    private static final Map<Player, Motion> MOTIONS = new WeakHashMap<>();
    private static @Nullable FlightWindSoundInstance windSound;

    private static final class Motion {
        float flight, oFlight;
        float tilt, oTilt;
        float pitch, oPitch;
        float roll, oRoll;
        float speed, oSpeed;
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(FlightAnimator::tick);
    }

    private static void tick(Minecraft client) {
        if (client.level == null || client.isPaused()) return;

        for (AbstractClientPlayer player : client.level.players()) {
            update(client, player);
        }
    }

    private static void update(Minecraft client, AbstractClientPlayer player) {
        boolean flying = FlightRingPower.isFlying(player);
        Motion motion = MOTIONS.get(player);
        if (motion == null) {
            if (!flying) return;
            motion = new Motion();
            MOTIONS.put(player, motion);
        }

        Vec3 velocity = player.position().subtract(player.xo, player.yo, player.zo);
        double speed = velocity.length();
        float bodyYaw = player.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 facing = new Vec3(-Mth.sin(bodyYaw), 0.0, Mth.cos(bodyYaw));
        Vec3 right = new Vec3(-Mth.cos(bodyYaw), 0.0, -Mth.sin(bodyYaw));
        double forward = velocity.dot(facing);
        double side = velocity.dot(right);
        float turn = Mth.wrapDegrees(player.yBodyRot - player.yBodyRotO);

        motion.oFlight = motion.flight;
        motion.oTilt = motion.tilt;
        motion.oPitch = motion.pitch;
        motion.oRoll = motion.roll;
        motion.oSpeed = motion.speed;

        motion.speed = (float) speed;
        motion.flight += ((flying ? 1.0F : 0.0F) - motion.flight) * 0.25F;

        float targetTilt = flying ? Mth.clamp((float) (speed - 0.15) / 1.2F, 0.0F, 1.0F) : 0.0F;
        motion.tilt += (targetTilt - motion.tilt) * 0.2F;

        float targetPitch = speed > 0.05
                ? (float) Math.toDegrees(Math.atan2(-velocity.y, Math.max(forward, 0.0) + 0.05))
                : 0.0F;
        motion.pitch += (targetPitch - motion.pitch) * 0.25F;

        float targetRoll = flying
                ? Mth.clamp((float) (side * 20.0 + turn * 2.0 * Math.min(speed, 2.0)), -MAX_ROLL, MAX_ROLL)
                : 0.0F;
        motion.roll += (targetRoll - motion.roll) * 0.2F;

        if (!flying && motion.flight < 0.001F) {
            MOTIONS.remove(player);
            return;
        }

        if (flying) {
            int color = PowerRingItem.getWornCorps(player).orElse(LanternCorps.GREEN).color();
            if (speed > TRAIL_SPEED) spawnTrail(client.level, player, velocity, speed, color);
            if (speed > SONIC_BOOM_SPEED && motion.oSpeed <= SONIC_BOOM_SPEED) sonicBoom(client.level, player, velocity, color);
        }

        if (player == client.player && flying && (windSound == null || windSound.isStopped())) {
            windSound = new FlightWindSoundInstance(client.player);
            client.getSoundManager().play(windSound);
        }
    }

    private static void spawnTrail(ClientLevel level, Player player, Vec3 velocity, double speed, int color) {
        RandomSource random = level.getRandom();
        Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        int count = Math.min(6, Mth.ceil(speed * 1.5));
        DustParticleOptions dust = new DustParticleOptions(color, Math.min(1.0F + (float) speed * 0.35F, 2.5F));

        for (int i = 0; i < count; i++) {
            double back = 1.0 + (i + random.nextDouble()) / count;
            Vec3 pos = center.subtract(velocity.scale(back));
            level.addParticle(dust,
                    pos.x + (random.nextDouble() - 0.5) * 0.3,
                    pos.y + (random.nextDouble() - 0.5) * 0.3,
                    pos.z + (random.nextDouble() - 0.5) * 0.3,
                    0.0, 0.0, 0.0);
        }
    }

    private static void sonicBoom(ClientLevel level, Player player, Vec3 velocity, int color) {
        Vec3 direction = velocity.normalize();
        Vec3 reference = Math.abs(direction.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 a = direction.cross(reference).normalize();
        Vec3 b = direction.cross(a);
        Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0).subtract(direction.scale(1.5));
        DustParticleOptions dust = new DustParticleOptions(color, 2.0F);

        int points = 32;
        for (int i = 0; i < points; i++) {
            float angle = i * Mth.TWO_PI / points;
            Vec3 offset = a.scale(Mth.cos(angle)).add(b.scale(Mth.sin(angle)));
            Vec3 pos = center.add(offset.scale(1.4));
            Vec3 push = offset.scale(0.25);
            level.addParticle(dust, pos.x, pos.y, pos.z, push.x, push.y, push.z);
        }

        level.playLocalSound(center.x, center.y, center.z, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS, 1.0F, 0.6F, false);
    }

    public static @Nullable FlightPose pose(Player player, float partialTicks) {
        Motion motion = MOTIONS.get(player);
        if (motion == null) return null;

        float flight = Mth.lerp(partialTicks, motion.oFlight, motion.flight);
        if (flight < 0.001F) return null;

        float tilt = Mth.lerp(partialTicks, motion.oTilt, motion.tilt);
        float pitch = Mth.lerp(partialTicks, motion.oPitch, motion.pitch);
        float roll = Mth.lerp(partialTicks, motion.oRoll, motion.roll);
        float bodyAngle = -flight * Mth.lerp(tilt, HOVER_LEAN, 90.0F + pitch);

        HumanoidArm leadArm = PowerRingItem.getCorps(player.getMainHandItem()).isPresent()
                ? player.getMainArm()
                : player.getMainArm().getOpposite();

        return new FlightPose(flight, tilt, bodyAngle, roll * flight, leadArm);
    }

    public static boolean isAnimating(Player player) {
        return MOTIONS.containsKey(player);
    }

    public static float speed(Player player) {
        Motion motion = MOTIONS.get(player);
        return motion == null ? 0.0F : motion.speed;
    }

    public static float fovBoost(Player player, boolean firstPerson) {
        Motion motion = MOTIONS.get(player);
        if (motion == null) return 0.0F;
        float speedFactor = Mth.clamp((motion.speed - 0.3F) / (float) (FlightRingPower.BOOST_SPEED - 0.3), 0.0F, 1.0F);
        return motion.flight * speedFactor * (firstPerson ? FIRST_PERSON_FOV_BOOST : THIRD_PERSON_FOV_BOOST);
    }

    public static float cameraRoll(Player player, float partialTicks) {
        Motion motion = MOTIONS.get(player);
        if (motion == null) return 0.0F;
        return Mth.lerp(partialTicks, motion.oRoll, motion.roll)
                * Mth.lerp(partialTicks, motion.oFlight, motion.flight)
                * CAMERA_ROLL_SCALE;
    }

    private FlightAnimator() {}
}
