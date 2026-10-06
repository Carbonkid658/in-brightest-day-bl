package dev.amble.client.effects;

import dev.amble.BrightestDay;
import dev.amble.core.networking.payloads.s2c.BloodHuntS2CPayload;
import dev.amble.core.ringpowers.LanternCorps;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class BloodHuntClient {
    public static final int GLOW_COLOR = LanternCorps.RED.color();
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
    private static final int TOP_MARGIN = 6;

    private static int preyId = BloodHuntS2CPayload.NO_PREY;
    private static Vec3 lastKnown = Vec3.ZERO;

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(BloodHuntS2CPayload.TYPE, (payload, context) -> {
            preyId = payload.preyId();
            lastKnown = new Vec3(payload.x(), payload.y(), payload.z());
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> preyId = BloodHuntS2CPayload.NO_PREY);
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, BrightestDay.id("blood_hunt"), BloodHuntClient::extractCompass);
    }

    public static boolean isPrey(Entity entity) {
        return preyId != BloodHuntS2CPayload.NO_PREY && entity.getId() == preyId;
    }

    private static void extractCompass(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null || preyId == BloodHuntS2CPayload.NO_PREY) return;

        Entity prey = client.level.getEntity(preyId);
        Vec3 target = prey != null ? prey.getBoundingBox().getCenter() : lastKnown;
        Vec3 offset = target.subtract(player.getEyePosition());
        float bearing = (float) Math.toDegrees(Mth.atan2(-offset.x, offset.z));
        float relative = Mth.wrapDegrees(bearing - player.getYRot());
        int sector = Math.floorMod(Math.round(relative / 45.0F), ARROWS.length);
        int distance = Mth.floor(offset.length());

        Font font = client.font;
        Component text = Component.translatable("hud.brightestday.blood_hunt", ARROWS[sector], distance);
        int x = graphics.guiWidth() / 2 - font.width(text) / 2;
        graphics.text(font, text, x, TOP_MARGIN, ARGB.opaque(GLOW_COLOR), true);
    }

    private BloodHuntClient() {}
}
