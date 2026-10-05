package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.config.BrightestDayConfig;
import dev.amble.core.networking.payloads.s2c.AileronRollS2CPayload;
import dev.amble.core.ringpowers.impl.FlightRingPower;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.WeakHashMap;

public record AileronRollC2SPayload(boolean right) implements CustomPacketPayload {
    private static final int COOLDOWN_TICKS = 10;
    private static final Map<ServerPlayer, Integer> LAST_ROLL = new WeakHashMap<>();

    public static final Type<AileronRollC2SPayload> TYPE =
            new Type<>(BrightestDay.id("aileron_roll"));

    public static final StreamCodec<ByteBuf, AileronRollC2SPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, AileronRollC2SPayload::right,
                    AileronRollC2SPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        if (!BrightestDayConfig.get().aileronRolls || !FlightRingPower.isFlying(player)) return;

        int now = player.level().getServer().getTickCount();
        Integer last = LAST_ROLL.get(player);
        if (last != null && now - last < COOLDOWN_TICKS) return;
        LAST_ROLL.put(player, now);

        AileronRollS2CPayload payload = new AileronRollS2CPayload(player.getId(), this.right);
        for (ServerPlayer watcher : PlayerLookup.tracking(player)) {
            if (watcher != player) ServerPlayNetworking.send(watcher, payload);
        }
    }
}
