package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.team.LanternTeams;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ByIdMap;

import java.util.UUID;
import java.util.function.IntFunction;

public record TeamActionC2SPayload(Action action, UUID target) implements CustomPacketPayload {

    public enum Action {
        INVITE, CANCEL, ACCEPT, DECLINE, LEAVE;

        private static final IntFunction<Action> BY_ID = ByIdMap.continuous(Action::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        private static final StreamCodec<ByteBuf, Action> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Action::ordinal);
    }

    public static final Type<TeamActionC2SPayload> TYPE =
            new Type<>(BrightestDay.id("team_action"));

    public static final StreamCodec<ByteBuf, TeamActionC2SPayload> CODEC =
            StreamCodec.composite(
                    Action.STREAM_CODEC, TeamActionC2SPayload::action,
                    UUIDUtil.STREAM_CODEC, TeamActionC2SPayload::target,
                    TeamActionC2SPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        switch (this.action) {
            case INVITE -> {
                ServerPlayer target = player.level().getServer().getPlayerList().getPlayer(this.target);
                if (target != null) LanternTeams.invite(player, target);
            }
            case CANCEL -> LanternTeams.cancel(player, this.target);
            case ACCEPT -> LanternTeams.accept(player, this.target);
            case DECLINE -> LanternTeams.decline(player, this.target);
            case LEAVE -> LanternTeams.leave(player, true);
        }
    }
}
