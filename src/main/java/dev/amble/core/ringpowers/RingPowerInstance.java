package dev.amble.core.ringpowers;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public record RingPowerInstance<D>(RingPower<D> power, D data) {

    public static final Codec<RingPowerInstance<?>> CODEC =
            RingPower.CODEC.<RingPowerInstance<?>>dispatch("type", RingPowerInstance::power, RingPower::instanceCodec);

    public static final StreamCodec<RegistryFriendlyByteBuf, List<RingPowerInstance<?>>> LIST_STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.listOf());

    public boolean is(RingPower<?> power) {
        return this.power == power;
    }

    public RingPowerInstance<D> withData(D data) {
        return new RingPowerInstance<>(this.power, data);
    }

    public boolean run(ServerPlayer player) {
        return this.power.run(player, this.data);
    }

    public void tick(ServerPlayer player) {
        this.power.tick(player, this.data);
    }
}
