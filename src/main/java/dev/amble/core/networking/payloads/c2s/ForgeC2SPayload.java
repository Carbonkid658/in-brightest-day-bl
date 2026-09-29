package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.constructs.ConstructTool;
import dev.amble.core.ringpowers.constructs.ToolForgeConstruct;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ForgeC2SPayload(int tool) implements CustomPacketPayload {

    public static final Type<ForgeC2SPayload> TYPE =
            new Type<>(BrightestDay.id("forge"));

    public static final StreamCodec<ByteBuf, ForgeC2SPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ForgeC2SPayload::tool,
                    ForgeC2SPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        ConstructTool[] tools = ConstructTool.values();
        if (this.tool >= 0 && this.tool < tools.length) ToolForgeConstruct.forge(context.player(), tools[this.tool]);
    }
}
