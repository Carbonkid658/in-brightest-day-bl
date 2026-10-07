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
import net.minecraft.world.entity.player.Player;

public final class AileronRolls {
    private static final int TAP_GAP_TICKS = 6;
    private static final int TAPS = 3;
    private static final int COOLDOWN_TICKS = FlightAnimator.ROLL_TICKS + 4;

    private static boolean leftWasDown;
    private static boolean rightWasDown;
    private static int lastTap = -100;
    private static int tapDirection;
    private static int taps;
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

        if (leftTapped != rightTapped) tap(player, rightTapped);
    }

    private static void tap(LocalPlayer player, boolean right) {
        int now = player.tickCount;
        int direction = right ? 1 : -1;
        taps = direction == tapDirection && now - lastTap <= TAP_GAP_TICKS ? taps + 1 : 1;
        tapDirection = direction;
        lastTap = now;
        if (taps < TAPS) return;
        taps = 0;
        roll(player, right);
    }

    private static void roll(LocalPlayer player, boolean right) {
        if (cooldown > 0 || !FlightAnimator.aileronRoll(player, right)) return;
        cooldown = COOLDOWN_TICKS;
        FlightRingPower.startDodge(player, right);

        whoosh(player);
        if (ClientPlayNetworking.canSend(AileronRollC2SPayload.TYPE)) ClientPlayNetworking.send(new AileronRollC2SPayload(right));
    }

    private static void whoosh(Player player) {
        player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.PHANTOM_SWOOP,
                SoundSource.PLAYERS, 0.8F, 1.4F + player.getRandom().nextFloat() * 0.2F, false);
    }

    private AileronRolls() {}
}
