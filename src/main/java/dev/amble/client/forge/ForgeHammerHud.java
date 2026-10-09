package dev.amble.client.forge;

import dev.amble.BrightestDay;
import dev.amble.core.forge.ForgeHammer;
import dev.amble.core.forge.SpectrumForgeBlock;
import dev.amble.core.networking.payloads.s2c.ForgeBeatS2CPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;

public final class ForgeHammerHud {
    private static final float TARGET_RADIUS = 10.0F;
    private static final float START_RADIUS = 46.0F;
    private static final int SEGMENTS = 64;
    private static final int PIP_SIZE = 5;
    private static final int PIP_GAP = 3;
    private static final int PIP_OFFSET = 26;
    private static final int FEEDBACK_TICKS = 10;
    private static final int HIT_COLOR = 0xFF7DFF9A;
    private static final int MISS_COLOR = 0xFFFF5A5A;
    private static final int PENDING_COLOR = 0xFF505050;

    private static boolean active;
    private static long beatAt;
    private static int strike;
    private static int hits;
    private static int misses;
    private static int color;
    private static int feedback;
    private static int feedbackTicks;

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(ForgeBeatS2CPayload.TYPE, (payload, context) -> {
            active = payload.active();
            beatAt = payload.beatAt();
            strike = payload.strike();
            hits = payload.hits();
            misses = payload.misses();
            color = ARGB.opaque(payload.color());
            if (payload.feedback() != ForgeBeatS2CPayload.NONE_FEEDBACK) {
                feedback = payload.feedback();
                feedbackTicks = FEEDBACK_TICKS;
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> active = false);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (feedbackTicks > 0) feedbackTicks--;
        });
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (!level.isClientSide() || !active || !(level.getBlockState(pos).getBlock() instanceof SpectrumForgeBlock)) return InteractionResult.PASS;
            return InteractionResult.SUCCESS;
        });
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, BrightestDay.id("forge_hammer"), ForgeHammerHud::extract);
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || (!active && feedbackTicks <= 0)) return;

        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2;
        if (active) {
            float now = client.level.getGameTime() + deltaTracker.getGameTimeDeltaPartialTick(false);
            float remaining = Mth.clamp((beatAt - now) / ForgeHammer.BEAT_TICKS, 0.0F, 1.0F);
            float radius = TARGET_RADIUS + (START_RADIUS - TARGET_RADIUS) * remaining;
            float closeness = 1.0F - Math.abs(radius - TARGET_RADIUS) / (START_RADIUS - TARGET_RADIUS);
            circle(graphics, cx, cy, TARGET_RADIUS, ARGB.color(0xC0, color), 1);
            circle(graphics, cx, cy, radius, ARGB.srgbLerp(closeness, ARGB.color(0xA0, 0xFFFFFF), color), 2);
        }

        int total = ForgeHammer.STRIKES * PIP_SIZE + (ForgeHammer.STRIKES - 1) * PIP_GAP;
        int x = cx - total / 2;
        int y = cy + PIP_OFFSET;
        int hitCount = hits;
        int missCount = misses;
        for (int i = 0; i < ForgeHammer.STRIKES; i++) {
            int pip = i < strike ? (hitCount-- > 0 ? HIT_COLOR : missCount-- > 0 ? MISS_COLOR : PENDING_COLOR) : PENDING_COLOR;
            graphics.fill(x, y, x + PIP_SIZE, y + PIP_SIZE, pip);
            x += PIP_SIZE + PIP_GAP;
        }

        if (feedbackTicks > 0) {
            Font font = client.font;
            Component text = Component.translatable(feedback == ForgeBeatS2CPayload.HIT ? "hud.brightestday.forge.hit" : "hud.brightestday.forge.miss");
            int alpha = Math.round(255 * feedbackTicks / (float) FEEDBACK_TICKS);
            graphics.text(font, text, cx - font.width(text) / 2, cy - PIP_OFFSET - font.lineHeight, ARGB.color(alpha, feedback == ForgeBeatS2CPayload.HIT ? HIT_COLOR : MISS_COLOR), true);
        }
    }

    private static void circle(GuiGraphicsExtractor graphics, int cx, int cy, float radius, int color, int thickness) {
        for (int i = 0; i < SEGMENTS; i++) {
            float angle = i * Mth.TWO_PI / SEGMENTS;
            int x = Math.round(cx + Mth.cos(angle) * radius);
            int y = Math.round(cy + Mth.sin(angle) * radius);
            graphics.fill(x, y, x + thickness, y + thickness, color);
        }
    }

    private ForgeHammerHud() {}
}
