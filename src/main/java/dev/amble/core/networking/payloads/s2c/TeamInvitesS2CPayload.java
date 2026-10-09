package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.UUID;

public record TeamInvitesS2CPayload(List<UUID> incoming, List<UUID> outgoing) implements CustomPacketPayload {

    public static final Type<TeamInvitesS2CPayload> TYPE =
            new Type<>(BrightestDay.id("team_invites"));

    public static final StreamCodec<ByteBuf, TeamInvitesS2CPayload> CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), TeamInvitesS2CPayload::incoming,
                    UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), TeamInvitesS2CPayload::outgoing,
                    TeamInvitesS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
