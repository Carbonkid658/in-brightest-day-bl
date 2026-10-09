package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.mannequin.HologramSettings;
import dev.amble.core.mannequin.Mannequins;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.decoration.Mannequin;

public record MannequinEditC2SPayload(int entityId, HologramSettings settings) implements CustomPacketPayload {

    public static final Type<MannequinEditC2SPayload> TYPE =
            new Type<>(BrightestDay.id("mannequin_edit"));

    public static final StreamCodec<ByteBuf, MannequinEditC2SPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MannequinEditC2SPayload::entityId,
            ByteBufCodecs.fromCodec(HologramSettings.CODEC), MannequinEditC2SPayload::settings,
            MannequinEditC2SPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        if (context.player().level().getEntity(this.entityId) instanceof Mannequin mannequin) {
            Mannequins.edit(context.player(), mannequin, this.settings);
        }
    }
}
