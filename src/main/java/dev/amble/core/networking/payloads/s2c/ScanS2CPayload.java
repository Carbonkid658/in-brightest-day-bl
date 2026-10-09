package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record ScanS2CPayload(int entityId, BlockPos pos, Component title, List<Component> lines, int color) implements CustomPacketPayload {

    public static final int NO_ENTITY = -1;

    public static final Type<ScanS2CPayload> TYPE =
            new Type<>(BrightestDay.id("scan"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScanS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, ScanS2CPayload::entityId,
                    BlockPos.STREAM_CODEC, ScanS2CPayload::pos,
                    ComponentSerialization.STREAM_CODEC, ScanS2CPayload::title,
                    ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list()), ScanS2CPayload::lines,
                    ByteBufCodecs.INT, ScanS2CPayload::color,
                    ScanS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
