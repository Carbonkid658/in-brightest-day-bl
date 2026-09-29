package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.menus.LanternMenu;
import dev.amble.core.menus.RingSlotContainer;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public record OpenLanternC2SPayload() implements CustomPacketPayload {

    public static final OpenLanternC2SPayload INSTANCE = new OpenLanternC2SPayload();

    public static final Type<OpenLanternC2SPayload> TYPE =
            new Type<>(BrightestDay.id("open_lantern"));

    public static final StreamCodec<ByteBuf, OpenLanternC2SPayload> CODEC = StreamCodec.unit(INSTANCE);

    public static final Component TITLE = Component.translatable("container.brightestday.lantern");

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) -> new LanternMenu(containerId, inventory, new RingSlotContainer(p)),
                TITLE
        ));
    }
}
