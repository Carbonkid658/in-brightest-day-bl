package dev.amble.client.flight;

import dev.amble.config.BrightestDayConfig;
import dev.amble.core.networking.payloads.c2s.AileronRollC2SPayload;
import dev.amble.core.networking.payloads.s2c.AileronRollS2CPayload;
import dev.amble.core.ringpowers.impl.FlightRingPower;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class AileronRolls {
    private static final int DOUBLE_TAP_TICKS = 7;
    private static final int COOLDOWN_TICKS = FlightAnimator.ROLL_TICKS + 4;

    private static boolean leftWasDown;
    private static boolean rightWasDown;
    private static int lastLeftTap = -100;
    private static int lastRightTap = -100;
    private static int cooldown;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(AileronRolls::tick);
        ClientPlayNetworking.registerGlobalReceiver(AileronRollS2CPayload.TYPE, (payload, context) -> {
            if (context.client().level != null && context.client().level.getEntity(payload.playerId()) instanceof Player player
                    && FlightAnimator.aileronRoll(player, payload.right())) {
                whoosh(player);
            }
        });
    }

    private static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.isPaused()) return;
        if (cooldown > 0) cooldown--;

        boolean leftDown = client.options.keyLeft.isDown();
        boolean rightDown = client.options.keyRight.isDown();
        boolean leftTapped = leftDown && !leftWasDown;
        boolean rightTapped = rightDown && !rightWasDown;
        leftWasDown = leftDown;
        rightWasDown = rightDown;

        if (client.gui.screen() != null || !BrightestDayConfig.get().aileronRolls || !FlightRingPower.isFlying(player)) return;

        int now = player.tickCount;
        if (leftTapped) {
            if (now - lastLeftTap <= DOUBLE_TAP_TICKS) roll(player, false);
            lastLeftTap = now;
        }
        if (rightTapped) {
            if (now - lastRightTap <= DOUBLE_TAP_TICKS) roll(player, true);
            lastRightTap = now;
        }
    }

    private static void roll(LocalPlayer player, boolean right) {
        if (cooldown > 0 || !FlightAnimator.aileronRoll(player, right)) return;
        cooldown = COOLDOWN_TICKS;
        lastLeftTap = lastRightTap = -100;

        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 side = new Vec3(-Mth.cos(yaw), 0.0, -Mth.sin(yaw)).scale(right ? 1.0 : -1.0);
        player.setDeltaMovement(player.getDeltaMovement().add(side.scale(BrightestDayConfig.get().aileronRollDodge)));

        whoosh(player);
        if (ClientPlayNetworking.canSend(AileronRollC2SPayload.TYPE)) ClientPlayNetworking.send(new AileronRollC2SPayload(right));
    }

    private static void whoosh(Player player) {
        player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.PHANTOM_SWOOP,
                SoundSource.PLAYERS, 0.8F, 1.4F + player.getRandom().nextFloat() * 0.2F, false);
    }

    private AileronRolls() {}
}
