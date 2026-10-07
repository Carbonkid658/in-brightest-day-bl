package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.menus.LanternMenu;
import dev.amble.core.menus.RingSlotContainer;
import dev.amble.core.progression.CorpsCaps;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

public record OpenLanternC2SPayload(ItemStack creativeCarried) implements CustomPacketPayload {

    public static final OpenLanternC2SPayload INSTANCE = new OpenLanternC2SPayload(ItemStack.EMPTY);

    public static final Type<OpenLanternC2SPayload> TYPE =
            new Type<>(BrightestDay.id("open_lantern"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenLanternC2SPayload> CODEC =
            ItemStack.OPTIONAL_STREAM_CODEC.map(OpenLanternC2SPayload::new, OpenLanternC2SPayload::creativeCarried);

    public static final Component TITLE = Component.translatable("container.brightestday.lantern");

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        ItemStack carried = player.hasInfiniteMaterials() && !this.creativeCarried.isEmpty() ? this.creativeCarried.copy() : player.containerMenu.getCarried();
        player.containerMenu.setCarried(ItemStack.EMPTY);
        if (carried.getItem() instanceof PowerRingItem && BrightestDayAttachments.getRing(player).isEmpty() && CorpsCaps.check(player, carried)) {
            BrightestDayAttachments.setRing(player, carried);
            carried = ItemStack.EMPTY;
        }

        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) -> new LanternMenu(containerId, inventory, new RingSlotContainer(p)),
                TITLE
        ));

        if (carried.isEmpty()) return;
        if (player.containerMenu instanceof LanternMenu) {
            player.containerMenu.setCarried(carried);
            player.containerMenu.broadcastChanges();
        } else {
            player.getInventory().placeItemBackInInventory(carried, Prediction.SERVER_ONLY);
        }
    }
}
