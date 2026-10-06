package dev.amble.client.flight;

import dev.amble.core.ringpowers.CorpsColors;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranimcore.animation.layered.modifier.AdjustmentModifier;
import com.zigythebird.playeranimcore.math.Vec3f;
import dev.amble.core.ringpowers.impl.FlightRingPower;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public final class FlightAnimator {

    private static final float MAX_HOVER_LEAN = 30.0F;
    private static final float HOVER_LEAN_PER_SPEED = 30.0F;
    private static final float MAX_HEAD_PITCH = 50.0F;
    private static final float MAX_HEAD_YAW = 70.0F;
    public static final int TRANSITION_TICKS = 16;
    private static final float FLIGHT_BODY_ANGLE = 87.5F;
    private static final float LIMB_SWING_FORWARD = 25.0F;
    private static final float LIMB_SWING_SIDE = 30.0F;
    private static final float LIMB_SWING_TURN = 1.5F;
    private static final float LIMB_SWING_PITCH = 3.0F;
    private static final float MAX_LIMB_SWING = 35.0F;
    private static final float ARM_SWING_SCALE = 0.6F;
    private static final float MAX_ROLL = 55.0F;
    private static final float CAMERA_ROLL_SCALE = 0.6F;
    private static final float FIRST_PERSON_FOV_BOOST = 0.4F;
    private static final float THIRD_PERSON_FOV_BOOST = 0.9F;
    public static final float MAX_FOV_MODIFIER = 2.0F;
    public static final float MAX_FOV = 120.0F;
    private static final double SONIC_BOOM_SPEED = 3.0;
    private static final float DIVE_PIVOT = 0.75F;
    private static final float DIVE_MODEL_CENTER = 0.94F;
    private static final float DIVE_BOX_CENTER = 0.3F;
    private static final double REMOTE_VELOCITY_SMOOTHING = 0.35;
    private static final int LANDING_GRACE_TICKS = 4;
    private static final int MIN_PHASE_TICKS = 8;
    public static final int ROLL_TICKS = 10;
    private static final float ROLL_CAMERA_TILT = 25.0F;
    private static final float HOVER_CAPE_LEAN = 70.0F;
    private static final float FLIGHT_CAPE_LEAN = 22.0F;
    private static final float HOVER_CAPE_FLAP = 20.0F;
    private static final float FLIGHT_CAPE_FLAP = 4.0F;
    private static final float CAPE_SIDE_SCALE = 0.5F;
    private static final float CAPE_FLUTTER = 3.0F;
    public static final float WIND_VERTICAL_SPEED = 0.25F;

    public static final RenderStateDataKey<Vec3> DIVE_OFFSET = RenderStateDataKey.create(() -> "brightestday:dive_offset");

    private static final Map<Player, Motion> MOTIONS = new WeakHashMap<>();
    private static @Nullable FlightWindSoundInstance windSound;

    private enum Phase { NONE, HOVER, FLIGHT }

    private static final class Motion {
        float flight, oFlight;
        float pitch, oPitch;
        float roll, oRoll;
        float speed, oSpeed;
        double forward;
        float bodyPitch, oBodyPitch;
        float flightBlend, oFlightBlend;
        float limbX, oLimbX;
        float limbZ, oLimbZ;
        Phase phase = Phase.NONE;
        Vec3 velocity = Vec3.ZERO;
        int landingTicks;
        int phaseTicks;
        float spin, oSpin;
        int rollTicks;
        int rollDirection;
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
        boolean airborne = FlightRingPower.isFlying(player);
        Motion motion = MOTIONS.get(player);
        if (motion == null) {
            if (!airborne) return;
            motion = new Motion();
            MOTIONS.put(player, motion);
        }

        motion.landingTicks = airborne ? 0 : motion.landingTicks + 1;
        boolean flying = airborne || FlightRingPower.canFly(player) && motion.phase != Phase.NONE && motion.landingTicks < LANDING_GRACE_TICKS;

        Vec3 rawVelocity = player.position().subtract(player.xo, player.yo, player.zo);
        motion.velocity = player == client.player ? rawVelocity : motion.velocity.lerp(rawVelocity, REMOTE_VELOCITY_SMOOTHING);
        Vec3 velocity = motion.velocity;
        double speed = velocity.length();
        float bodyYaw = player.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 facing = new Vec3(-Mth.sin(bodyYaw), 0.0, Mth.cos(bodyYaw));
        Vec3 right = new Vec3(-Mth.cos(bodyYaw), 0.0, -Mth.sin(bodyYaw));
        double forward = velocity.dot(facing);
        double side = velocity.dot(right);
        float turn = Mth.wrapDegrees(player.yBodyRot - player.yBodyRotO);

        motion.oFlight = motion.flight;
        motion.oPitch = motion.pitch;
        motion.oRoll = motion.roll;
        motion.oSpeed = motion.speed;
        motion.oBodyPitch = motion.bodyPitch;
        motion.oFlightBlend = motion.flightBlend;
        motion.oLimbX = motion.limbX;
        motion.oLimbZ = motion.limbZ;
        motion.oSpin = motion.spin;
        tickRoll(motion);

        motion.speed = (float) speed;
        motion.forward = forward;
        motion.flight += ((flying ? 1.0F : 0.0F) - motion.flight) * 0.25F;

        float targetPitch = speed > 0.05
                ? (float) Math.toDegrees(Math.atan2(-velocity.y, Math.max(forward, 0.0) + 0.05))
                : 0.0F;
        motion.pitch += (targetPitch - motion.pitch) * 0.25F;

        float targetRoll = flying
                ? Mth.clamp((float) (side * 20.0 + turn * 2.0 * Math.min(speed, 2.0)), -MAX_ROLL, MAX_ROLL)
                : 0.0F;
        motion.roll += (targetRoll - motion.roll) * 0.2F;

        motion.flightBlend = Mth.approach(motion.flightBlend, motion.phase == Phase.FLIGHT ? 1.0F : 0.0F, 1.0F / TRANSITION_TICKS);
        float blend = ease(motion.flightBlend);

        float hoverLean = Mth.clamp((float) forward * HOVER_LEAN_PER_SPEED, -MAX_HOVER_LEAN, MAX_HOVER_LEAN);
        float targetBodyPitch = motion.phase == Phase.NONE ? 0.0F : Mth.lerp(blend, hoverLean, motion.pitch);
        motion.bodyPitch += (targetBodyPitch - motion.bodyPitch) * 0.2F;

        float pitchRate = motion.pitch - motion.oPitch;
        float targetLimbX = flying ? Mth.lerp(blend, (float) forward * LIMB_SWING_FORWARD, pitchRate * LIMB_SWING_PITCH) : 0.0F;
        float targetLimbZ = flying ? -((float) side * LIMB_SWING_SIDE + turn * LIMB_SWING_TURN) : 0.0F;
        motion.limbX += (Mth.clamp(targetLimbX, -MAX_LIMB_SWING, MAX_LIMB_SWING) - motion.limbX) * 0.12F;
        motion.limbZ += (Mth.clamp(targetLimbZ, -MAX_LIMB_SWING, MAX_LIMB_SWING) - motion.limbZ) * 0.12F;

        updateAnimation(player, motion, flying);

        if (!flying && motion.flight < 0.001F) {
            MOTIONS.remove(player);
            return;
        }

        if (flying) {
            int color = CorpsColors.of(player);
            if (speed > SONIC_BOOM_SPEED && motion.oSpeed <= SONIC_BOOM_SPEED) sonicBoom(client.level, player, velocity, color);
        }

        if (player == client.player && flying && (motion.phase == Phase.FLIGHT || Math.abs(velocity.y) > WIND_VERTICAL_SPEED) && (windSound == null || windSound.isStopped())) {
            windSound = new FlightWindSoundInstance(client.player);
            client.getSoundManager().play(windSound);
        }
    }

    private static void tickRoll(Motion motion) {
        if (motion.rollTicks <= 0) {
            motion.spin = motion.oSpin = 0.0F;
            return;
        }
        motion.rollTicks--;
        float progress = 1.0F - motion.rollTicks / (float) ROLL_TICKS;
        motion.spin = ease(progress) * 360.0F * motion.rollDirection;
    }

    public static boolean aileronRoll(Player player, boolean right) {
        Motion motion = MOTIONS.get(player);
        if (motion == null || motion.rollTicks > 0) return false;
        motion.rollTicks = ROLL_TICKS;
        motion.rollDirection = right ? 1 : -1;
        return true;
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

    private static void updateAnimation(Player player, Motion motion, boolean flying) {
        PlayerAnimationController controller = FlightAnimations.controller(player);
        if (controller == null) return;

        if (!flying) {
            if (motion.phase != Phase.NONE) FlightAnimations.stop(controller);
            motion.phase = Phase.NONE;
            return;
        }

        if (motion.phaseTicks > 0) motion.phaseTicks--;
        switch (motion.phase) {
            case NONE -> enterPhase(controller, motion, Phase.HOVER, FlightAnimations.HOVER);
            case HOVER -> {
                if (motion.phaseTicks == 0 && motion.forward > FlightRingPower.DIVE_ENTER_SPEED) {
                    enterPhase(controller, motion, Phase.FLIGHT, FlightAnimations.FLIGHT);
                }
            }
            case FLIGHT -> {
                if (motion.phaseTicks == 0 && motion.forward < FlightRingPower.DIVE_EXIT_SPEED) {
                    enterPhase(controller, motion, Phase.HOVER, FlightAnimations.HOVER);
                }
            }
        }

        float intensity = Mth.clamp(motion.speed / (float) FlightRingPower.BOOST_SPEED, 0.0F, 1.0F);
        float hoverSpeed = 1.0F + Math.min(motion.speed, 1.2F) * 1.2F;
        float flightSpeed = 0.7F + intensity * 1.3F;
        FlightAnimations.setSpeed(controller, Mth.lerp(ease(motion.flightBlend), hoverSpeed, flightSpeed));
    }

    private static void enterPhase(PlayerAnimationController controller, Motion motion, Phase phase, Identifier animation) {
        FlightAnimations.loop(controller, animation);
        motion.phase = phase;
        motion.phaseTicks = MIN_PHASE_TICKS;
    }

    public static Optional<AdjustmentModifier.PartModifier> adjustment(Avatar avatar, String bone) {
        if (!(avatar instanceof Player player)) return Optional.empty();

        Motion motion = MOTIONS.get(player);
        if (motion == null) return Optional.empty();

        float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float flight = Mth.lerp(partialTicks, motion.oFlight, motion.flight);
        float blend = ease(Mth.lerp(partialTicks, motion.oFlightBlend, motion.flightBlend)) * flight;
        float limbX = Mth.lerp(partialTicks, motion.oLimbX, motion.limbX) * flight;
        float limbZ = Mth.lerp(partialTicks, motion.oLimbZ, motion.limbZ) * flight;

        return switch (bone) {
            case "body" -> rotation(
                    Mth.lerp(partialTicks, motion.oBodyPitch, motion.bodyPitch) * flight,
                    0.0F,
                    (Mth.lerp(partialTicks, motion.oRoll, motion.roll) + Mth.lerp(partialTicks, motion.oSpin, motion.spin)) * flight);
            case "head" -> {
                float bodyPitch = Mth.lerp(partialTicks, motion.oBodyPitch, motion.bodyPitch);
                float bodyYaw = Mth.rotLerp(partialTicks, player.yBodyRotO, player.yBodyRot);
                float lookPitch = Mth.clamp(player.getViewXRot(partialTicks) - bodyPitch, -MAX_HEAD_PITCH, MAX_HEAD_PITCH);
                float lookYaw = Mth.clamp(Mth.wrapDegrees(player.getViewYRot(partialTicks) - bodyYaw), -MAX_HEAD_YAW, MAX_HEAD_YAW);
                yield rotation(lookPitch * blend, lookYaw * blend, 0.0F);
            }
            case "right_arm", "left_arm" -> rotation(limbX * ARM_SWING_SCALE, 0.0F, limbZ * ARM_SWING_SCALE);
            case "right_leg", "left_leg" -> rotation(limbX, 0.0F, limbZ);
            default -> Optional.empty();
        };
    }

    private static float ease(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    private static Optional<AdjustmentModifier.PartModifier> rotation(float x, float y, float z) {
        return Optional.of(new AdjustmentModifier.PartModifier(
                new Vec3f(x * Mth.DEG_TO_RAD, y * Mth.DEG_TO_RAD, z * Mth.DEG_TO_RAD),
                Vec3f.ZERO
        ));
    }

    public static float bodyPitch(Player player, float partialTicks) {
        Motion motion = MOTIONS.get(player);
        if (motion == null) return 0.0F;

        float flight = Mth.lerp(partialTicks, motion.oFlight, motion.flight);
        float blend = ease(Mth.lerp(partialTicks, motion.oFlightBlend, motion.flightBlend));
        return (blend * FLIGHT_BODY_ANGLE + Mth.lerp(partialTicks, motion.oBodyPitch, motion.bodyPitch)) * flight;
    }

    public static void extractDive(Avatar entity, AvatarRenderState state, float partialTicks) {
        ((FabricRenderState) state).setData(DIVE_OFFSET, entity instanceof Player player ? diveOffset(player, partialTicks) : Vec3.ZERO);
    }

    public static void adjustCape(Avatar entity, AvatarRenderState state, float partialTicks) {
        if (!(entity instanceof Player player)) return;
        Motion motion = MOTIONS.get(player);
        if (motion == null) return;

        float flight = Mth.lerp(partialTicks, motion.oFlight, motion.flight);
        if (flight <= 0.0F) return;

        float blend = ease(Mth.lerp(partialTicks, motion.oFlightBlend, motion.flightBlend));
        float speed = Mth.lerp(partialTicks, motion.oSpeed, motion.speed);
        float maxLean = Mth.lerp(blend, HOVER_CAPE_LEAN, FLIGHT_CAPE_LEAN);
        float lean = maxLean * (1.0F - (float) Math.exp(-state.capeLean / maxLean));
        float time = player.tickCount + partialTicks;
        float flutter = Mth.sin(time * (0.8F + Math.min(speed, 3.0F) * 0.5F)) * Math.min(speed, 2.0F) * CAPE_FLUTTER;
        float flap = Mth.clamp(state.capeFlap, -6.0F, Mth.lerp(blend, HOVER_CAPE_FLAP, FLIGHT_CAPE_FLAP));

        state.capeLean = Mth.lerp(flight, state.capeLean, Math.max(lean + flutter, 0.0F));
        state.capeFlap = Mth.lerp(flight, state.capeFlap, flap);
        state.capeLean2 = Mth.lerp(flight, state.capeLean2, state.capeLean2 * CAPE_SIDE_SCALE);
    }

    public static Vec3 diveOffset(Player player, float partialTicks) {
        Motion motion = MOTIONS.get(player);
        if (motion == null) return Vec3.ZERO;

        float flight = Mth.lerp(partialTicks, motion.oFlight, motion.flight);
        float dive = ease(Mth.lerp(partialTicks, motion.oFlightBlend, motion.flightBlend)) * flight;
        float pitch = bodyPitch(player, partialTicks) * Mth.DEG_TO_RAD;
        float arm = DIVE_MODEL_CENTER - DIVE_PIVOT;
        return new Vec3(0.0, DIVE_BOX_CENTER - DIVE_PIVOT - arm * Mth.cos(pitch), arm * Mth.sin(pitch)).scale(dive);
    }

    public static Vec3 worldDiveOffset(Player player, float partialTicks) {
        Vec3 local = diveOffset(player, partialTicks);
        float yaw = Mth.rotLerp(partialTicks, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 facing = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        return new Vec3(0.0, local.y, 0.0).add(facing.scale(-local.z));
    }

    public static boolean isAnimating(Player player) {
        return MOTIONS.containsKey(player);
    }

    public static float flightBlend(Player player) {
        Motion motion = MOTIONS.get(player);
        return motion == null ? 0.0F : ease(motion.flightBlend);
    }

    public static float verticalSpeed(Player player) {
        Motion motion = MOTIONS.get(player);
        return motion == null ? 0.0F : (float) Math.abs(motion.velocity.y);
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
        float spin = Mth.lerp(partialTicks, motion.oSpin, motion.spin) / 360.0F;
        float tilt = Mth.sin(Math.abs(spin) * Mth.PI) * ROLL_CAMERA_TILT * Math.signum(spin);
        return (Mth.lerp(partialTicks, motion.oRoll, motion.roll) * CAMERA_ROLL_SCALE + tilt)
                * Mth.lerp(partialTicks, motion.oFlight, motion.flight);
    }

    private FlightAnimator() {}
}
