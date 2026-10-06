package dev.amble.client.effects;

import dev.amble.BrightestDay;
import dev.amble.core.networking.payloads.c2s.CommsC2SPayload;
import dev.amble.core.networking.payloads.s2c.CommsIncomingS2CPayload;
import dev.amble.core.networking.payloads.s2c.CommsTalkingS2CPayload;
import dev.amble.core.networking.payloads.s2c.CommsTargetS2CPayload;
import dev.amble.core.ringpowers.CorpsColors;
import dev.amble.core.ringpowers.RingPowerRegistry;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class CommsClient {
    private static final float RAISE_SPEED = 0.3F;
    private static final int HUD_BOTTOM_OFFSET = 72;
    private static final int LINE_GAP = 11;
    private static final int INCOMING_COLOR = 0xFFB8E0FF;

    private static final Set<Integer> TALKING = new HashSet<>();
    private static final Map<Player, float[]> AMOUNTS = new WeakHashMap<>();
    private static String dialed = "";
    private static String incoming = "";
    private static boolean transmitting;

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(CommsTargetS2CPayload.TYPE, (payload, context) -> dialed = payload.name());
        ClientPlayNetworking.registerGlobalReceiver(CommsIncomingS2CPayload.TYPE, (payload, context) -> incoming = payload.active() ? payload.name() : "");
        ClientPlayNetworking.registerGlobalReceiver(CommsTalkingS2CPayload.TYPE, (payload, context) -> {
            if (payload.talking()) TALKING.add(payload.playerId());
            else TALKING.remove(payload.playerId());
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            TALKING.clear();
            dialed = "";
            incoming = "";
            transmitting = false;
        });
        ClientTickEvents.END_CLIENT_TICK.register(CommsClient::tick);
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, BrightestDay.id("comms"), CommsClient::extractHud);
    }

    private static boolean selected(LocalPlayer player) {
        return ArmedRingPower.isArmed(player) && ArmedRingPower.activeAbility(player).orElse(null) == RingPowerRegistry.COMMS;
    }

    public static boolean onScroll(int wheel) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || wheel == 0 || !selected(player)) return false;
        ClientPlayNetworking.send(new CommsC2SPayload(wheel > 0 ? CommsC2SPayload.Action.PREVIOUS : CommsC2SPayload.Action.NEXT));
        return true;
    }

    private static void tick(Minecraft client) {
        LocalPlayer local = client.player;
        if (local == null || client.level == null) return;

        boolean holding = AbilityClient.holding(local, RingPowerRegistry.COMMS);
        if (holding != transmitting) {
            transmitting = holding;
            ClientPlayNetworking.send(new CommsC2SPayload(holding ? CommsC2SPayload.Action.START : CommsC2SPayload.Action.STOP));
        }

        if (client.isPaused()) return;
        for (AbstractClientPlayer player : client.level.players()) {
            boolean talking = TALKING.contains(player.getId());
            float[] amount = AMOUNTS.get(player);
            if (amount == null) {
                if (!talking) continue;
                amount = new float[2];
                AMOUNTS.put(player, amount);
            }
            amount[1] = amount[0];
            amount[0] += ((talking ? 1.0F : 0.0F) - amount[0]) * RAISE_SPEED;
            if (!talking && amount[0] < 0.001F) AMOUNTS.remove(player);
        }
    }

    public static float talking(Player player, float partialTicks) {
        float[] amount = AMOUNTS.get(player);
        return amount == null ? 0.0F : Mth.lerp(partialTicks, amount[1], amount[0]);
    }

    private static void extractHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        Font font = client.font;
        int y = graphics.guiHeight() - HUD_BOTTOM_OFFSET;
        if (!incoming.isEmpty()) {
            line(graphics, font, Component.translatable("hud.brightestday.comms.incoming", incoming), y, INCOMING_COLOR);
            y -= LINE_GAP;
        }
        if (!selected(player)) return;

        int color = ARGB.opaque(CorpsColors.of(player));
        Component status = dialed.isEmpty()
                ? Component.translatable("hud.brightestday.comms.none")
                : Component.translatable(transmitting ? "hud.brightestday.comms.transmitting" : "hud.brightestday.comms.dialed", dialed);
        line(graphics, font, status, y, color);
    }

    private static void line(GuiGraphicsExtractor graphics, Font font, Component text, int y, int color) {
        graphics.text(font, text, graphics.guiWidth() / 2 - font.width(text) / 2, y, color, true);
    }

    private CommsClient() {}
}
