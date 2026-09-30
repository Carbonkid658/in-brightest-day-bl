package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.ColorTweak;
import dev.amble.core.ringpowers.EyePaint;
import dev.amble.core.ringpowers.RingPowerInstance;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record PlayerStateS2CPayload(int playerId, List<RingPowerInstance<?>> powers, ItemStack ring, ColorTweak colorTweak, EyePaint eyes) implements CustomPacketPayload {

    public static final Type<PlayerStateS2CPayload> TYPE =
            new Type<>(BrightestDay.id("player_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerStateS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PlayerStateS2CPayload::playerId,
                    RingPowerInstance.LIST_STREAM_CODEC, PlayerStateS2CPayload::powers,
                    ItemStack.OPTIONAL_STREAM_CODEC, PlayerStateS2CPayload::ring,
                    ColorTweak.STREAM_CODEC, PlayerStateS2CPayload::colorTweak,
                    EyePaint.STREAM_CODEC, PlayerStateS2CPayload::eyes,
                    PlayerStateS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
