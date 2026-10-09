package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.drill.DrillMode;
import dev.amble.core.drill.DrillModes;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DrillModeC2SPayload(int mode) implements CustomPacketPayload {

    public static final Type<DrillModeC2SPayload> TYPE =
            new Type<>(BrightestDay.id("drill_mode"));

    public static final StreamCodec<ByteBuf, DrillModeC2SPayload> CODEC =
            ByteBufCodecs.VAR_INT.map(DrillModeC2SPayload::new, DrillModeC2SPayload::mode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        DrillModes.set(context.player(), DrillMode.byId(this.mode));
    }
}
