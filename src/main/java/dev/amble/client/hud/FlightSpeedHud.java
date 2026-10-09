package dev.amble.client.hud;

import dev.amble.BrightestDay;
import dev.amble.client.config.BrightestDayClientConfig;
import dev.amble.client.flight.FlightControls;
import dev.amble.core.ringpowers.CorpsColors;
import dev.amble.core.ringpowers.impl.FlightRingPower;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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

public final class FlightSpeedHud {
    private static final int WIDTH = 100;
    private static final int HEIGHT = 25;
    private static final int MARGIN = 4;
    private static final int PADDING = 4;
    private static final int BAR_HEIGHT = 3;
    private static final int HOTBAR_HALF_WIDTH = 91;
    private static final int OFFHAND_SLOT_WIDTH = 29;
    private static final int STATUS_BARS_HEIGHT = 62;
    private static final float FADE_PER_TICK = 0.15F;
    private static final float SPEED_SMOOTHING = 0.5F;
    private static final float MIN_VISIBLE = 0.02F;
    private static final double TICKS_PER_SECOND = 20.0;
    private static final double SCALE_MAX = 5.0 * TICKS_PER_SECOND;

    private static float fade, oFade;
    private static double speed, oSpeed;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(FlightSpeedHud::tick);
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, BrightestDay.id("flight_speed"), FlightSpeedHud::extract);
    }

    private static void tick(Minecraft client) {
        if (client.isPaused()) return;
        LocalPlayer player = client.player;
        oFade = fade;
        oSpeed = speed;
        boolean flying = player != null && FlightRingPower.isFlying(player);
        fade = Mth.approach(fade, flying ? 1.0F : 0.0F, FADE_PER_TICK);
        double measured = player == null ? 0.0 : player.position().subtract(player.xo, player.yo, player.zo).length() * TICKS_PER_SECOND;
        speed += (measured - speed) * SPEED_SMOOTHING;
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || player.isSpectator() || !BrightestDayClientConfig.get().showFlightSpeedometer) return;

        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
        float alpha = Mth.lerp(partialTicks, oFade, fade);
        if (alpha < MIN_VISIBLE) return;
        alpha = alpha * alpha * (3.0F - 2.0F * alpha);

        double current = Mth.lerp(partialTicks, oSpeed, speed);
        double cruise = FlightRingPower.cruiseSpeed(player) * TICKS_PER_SECOND;
        double boost = FlightRingPower.boostSpeed(FlightRingPower.cruiseSpeed(player)) * TICKS_PER_SECOND;
        boolean boosting = FlightRingPower.isBoosting(player);

        Font font = client.font;
        int color = ARGB.opaque(CorpsColors.ui(player));
        int left = graphics.guiWidth() - MARGIN - WIDTH;
        int top = graphics.guiHeight() - MARGIN - HEIGHT;
        if (left < graphics.guiWidth() / 2 + HOTBAR_HALF_WIDTH + OFFHAND_SLOT_WIDTH) {
            top = graphics.guiHeight() - STATUS_BARS_HEIGHT - HEIGHT;
        }

        graphics.fill(left, top, left + WIDTH, top + HEIGHT, ARGB.color(0x90 * alpha / 255.0F, ARGB.scaleRGB(color, 0.25F)));
        graphics.fill(left, top, left + 1, top + HEIGHT, ARGB.color(alpha, color));

        int textY = top + PADDING;
        Component reading = Component.translatable("hud.brightestday.flight_speed", FlightControls.formatSpeed(current));
        graphics.text(font, reading, left + PADDING + 1, textY, ARGB.white(alpha), true);

        Component status;
        int statusColor;
        if (boosting) {
            status = Component.translatable("hud.brightestday.flight_boost");
            float pulse = 0.75F + 0.25F * Mth.sin((player.tickCount + partialTicks) * 0.5F);
            statusColor = ARGB.color(alpha, ARGB.scaleRGB(color, pulse));
        } else {
            status = Component.translatable("hud.brightestday.flight_cruise", FlightControls.formatSpeed(cruise));
            statusColor = ARGB.color(alpha, 0xB8C4BA);
        }
        graphics.text(font, status, left + WIDTH - PADDING - font.width(status), textY, statusColor, true);

        int barLeft = left + PADDING + 1;
        int barRight = left + WIDTH - PADDING;
        int barWidth = barRight - barLeft;
        int barTop = top + HEIGHT - PADDING - BAR_HEIGHT;
        int barBottom = barTop + BAR_HEIGHT;

        int cruiseX = barLeft + position(cruise, barWidth);
        int boostX = barLeft + position(boost, barWidth);
        int speedX = barLeft + position(current, barWidth);

        graphics.fill(barLeft, barTop, barRight, barBottom, ARGB.color(0x80 * alpha / 255.0F, 0x000000));
        graphics.fill(cruiseX, barTop, boostX, barBottom, ARGB.color((boosting ? 0x70 : 0x30) * alpha / 255.0F, color));
        graphics.fill(barLeft, barTop, speedX, barBottom, ARGB.color(alpha, color));
        graphics.fill(cruiseX, barTop - 2, cruiseX + 1, barBottom + 1, ARGB.white(alpha));
    }

    private static int position(double blocksPerSecond, int width) {
        return Math.round(width * (float) Math.sqrt(Mth.clamp(blocksPerSecond / SCALE_MAX, 0.0, 1.0)));
    }

    private FlightSpeedHud() {}
}
