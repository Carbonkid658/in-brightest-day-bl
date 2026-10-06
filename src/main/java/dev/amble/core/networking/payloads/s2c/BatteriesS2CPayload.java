package dev.amble.core.networking.payloads.s2c;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record BatteriesS2CPayload(List<Entry> batteries) implements CustomPacketPayload {

    public record Entry(BlockPos pos, int color) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                Codec.INT.fieldOf("color").forGetter(Entry::color)
        ).apply(instance, Entry::new));
    }

    public static final Type<BatteriesS2CPayload> TYPE =
            new Type<>(BrightestDay.id("batteries"));

    public static final StreamCodec<ByteBuf, BatteriesS2CPayload> CODEC =
            ByteBufCodecs.fromCodec(Entry.CODEC.listOf()).map(BatteriesS2CPayload::new, BatteriesS2CPayload::batteries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
