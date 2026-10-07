package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.LanternCorps;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record TeamRosterS2CPayload(List<Member> members) implements CustomPacketPayload {
    public record Member(UUID id, String name, int corps) {
        public static final StreamCodec<ByteBuf, Member> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Member::id,
                ByteBufCodecs.STRING_UTF8, Member::name,
                ByteBufCodecs.VAR_INT, Member::corps,
                Member::new
        );

        public Optional<LanternCorps> wornCorps() {
            LanternCorps[] values = LanternCorps.values();
            return this.corps >= 0 && this.corps < values.length ? Optional.of(values[this.corps]) : Optional.empty();
        }
    }

    public static final Type<TeamRosterS2CPayload> TYPE =
            new Type<>(BrightestDay.id("team_roster"));

    public static final StreamCodec<ByteBuf, TeamRosterS2CPayload> CODEC =
            Member.CODEC.apply(ByteBufCodecs.list()).map(TeamRosterS2CPayload::new, TeamRosterS2CPayload::members);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
