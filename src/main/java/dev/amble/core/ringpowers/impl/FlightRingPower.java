package dev.amble.core.ringpowers.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.amble.BrightestDay;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.ringpowers.RingPower;
import dev.amble.core.ringpowers.RingPowerRegistry;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class FlightRingPower extends RingPower<FlightRingPower.Data> {

    public static final double CRUISE_SPEED = 1.2;
    public static final double BOOST_SPEED = 3.5;
    private static final double CRUISE_RESPONSE = 0.15;
    private static final double BOOST_RESPONSE = 0.06;
    private static final double BRAKE_RESPONSE = 0.12;

    public record Data(boolean enabled) {
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("enabled", false).forGetter(Data::enabled)
        ).apply(instance, Data::new));
    }

    public FlightRingPower() {
        super(BrightestDay.id("flight"), EnumSet.allOf(LanternCorps.class), Data.CODEC);
    }

    @Override
    public Data createData() {
        return new Data(false);
    }

    @Override
    public boolean run(ServerPlayer player, Data data) {
        setEnabled(player, !data.enabled());
        return true;
    }

    @Override
    public void tick(ServerPlayer player, Data data) {
        player.resetFallDistance();
    }

    public static boolean hasFlight(Player player) {
        return BrightestDayAttachments.has(player, RingPowerRegistry.FLIGHT);
    }

    public static boolean canFly(Player player) {
        return BrightestDayAttachments.get(player, RingPowerRegistry.FLIGHT)
                .map(instance -> instance.data().enabled())
                .orElse(false);
    }

    public static void setEnabled(Player player, boolean enabled) {
        if (!hasFlight(player)) return;

        BrightestDayAttachments.setData(player, RingPowerRegistry.FLIGHT, new Data(enabled));
        if (enabled && player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    public static boolean isFlying(Player player) {
        return canFly(player)
                && !player.onGround()
                && !player.isInWater()
                && !player.isPassenger()
                && !player.isSpectator()
                && !player.isSleeping()
                && !player.isFallFlying()
                && !player.getAbilities().flying;
    }

    public static void travel(Player player, Vec3 input, boolean ascending) {
        boolean boosting = player.isSprinting();
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 left = new Vec3(Mth.cos(yaw), 0.0, Mth.sin(yaw));
        double vertical = (ascending ? 1.0 : 0.0) - (player.isShiftKeyDown() ? 1.0 : 0.0);

        Vec3 wish = player.getLookAngle().scale(input.z)
                .add(left.scale(input.x))
                .add(0.0, vertical, 0.0);

        Vec3 target;
        double response;
        if (wish.lengthSqr() < 1.0E-4) {
            target = Vec3.ZERO;
            response = BRAKE_RESPONSE;
        } else {
            target = wish.normalize().scale(boosting ? BOOST_SPEED : CRUISE_SPEED);
            response = boosting ? BOOST_RESPONSE : CRUISE_RESPONSE;
        }

        Vec3 velocity = player.getDeltaMovement();
        velocity = velocity.add(target.subtract(velocity).scale(response));

        player.setDeltaMovement(velocity);
        player.move(MoverType.SELF, velocity);
    }

    public static void registerEvents() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(entity instanceof Player player && source.is(DamageTypeTags.IS_FALL) && hasFlight(player)));
    }
}
