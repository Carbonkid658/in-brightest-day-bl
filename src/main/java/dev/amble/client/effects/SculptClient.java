package dev.amble.client.effects;

import dev.amble.BrightestDay;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.networking.payloads.c2s.SculptShapeC2SPayload;
import dev.amble.core.ringpowers.CorpsColors;
import dev.amble.core.ringpowers.RingPowerRegistry;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import dev.amble.core.sculpt.SculptGeometry;
import dev.amble.core.sculpt.SculptShape;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SculptClient {
    private static final float PREVIEW_ALPHA = 0.3F;
    private static final float DOME_PREVIEW_ALPHA = 0.18F;
    private static final float RING_HALF = 2.0F * VoxelRenderer.PIXEL;
    private static final int LEGEND_MARGIN = 6;
    private static final int LEGEND_PADDING = 5;
    private static final int LEGEND_LINE = 10;

    private static final List<Vec3> RING = new ArrayList<>();
    private static SculptShape shape = SculptShape.FREEFORM;
    private static double depth = Double.NaN;
    private static @Nullable Vec3 tubeStart;
    private static @Nullable Vec3 tubeEnd;

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> shape = SculptShape.FREEFORM);
        ClientTickEvents.END_CLIENT_TICK.register(SculptClient::tick);
        LevelRenderEvents.COLLECT_SUBMITS.register(SculptClient::render);
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, BrightestDay.id("sculpt_legend"), SculptClient::extractLegend);
    }

    public static boolean isSelected(Player player) {
        return ArmedRingPower.selectedConstruct(player).orElse(null) == RingPowerRegistry.SCULPT;
    }

    private static boolean isReady(LocalPlayer player) {
        return player.getMainHandItem().isEmpty() && ArmedRingPower.isArmed(player) && PowerRingItem.hasCharge(player) && isSelected(player);
    }

    /** Sneak + scroll cycles the shape; plain scroll is left to the width control. */
    public static boolean onScroll(int wheel) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || !player.isShiftKeyDown() || !isReady(player) || wheel == 0) return false;

        shape = shape.cycle(-wheel);
        ClientPlayNetworking.send(new SculptShapeC2SPayload(shape.ordinal()));
        client.gui.hud.setOverlayMessage(Component.translatable("message.brightestday.sculpt_shape",
                Component.translatable(RingPowerRegistry.SCULPT.getTranslationKey()), Component.translatable(shape.translationKey())), false);
        return true;
    }

    /** Mirrors the server's trace while a cage or tube is held, so its outline can be previewed before release. */
    private static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        boolean tracing = player != null && client.level != null && (shape == SculptShape.CAGE || shape == SculptShape.TUBE)
                && BlastEffects.isSustaining() && isSelected(player);
        if (!tracing) {
            RING.clear();
            depth = Double.NaN;
            tubeStart = null;
            tubeEnd = null;
            return;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 point = SculptGeometry.trace(client.level, player, eye, player.getLookAngle(), depth);
        if (point == null) return;
        if (Double.isNaN(depth)) depth = SculptGeometry.lockDepth(eye, point);

        if (shape == SculptShape.TUBE) {
            if (tubeStart == null) tubeStart = point;
            tubeEnd = point;
        } else if (RING.isEmpty() || RING.getLast().distanceToSqr(point) > SculptGeometry.STEP * SculptGeometry.STEP) {
            RING.add(point);
        }
    }

    private static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null || !isReady(player) || ConstructClient.isLookingAtLantern()) return;

        Vec3 camera = context.levelState().cameraRenderState.pos;
        int color = CorpsColors.of(player);
        int width = ConstructClient.size(RingPowerRegistry.SCULPT);
        switch (shape) {
            case CAGE -> renderCage(context, camera, color);
            case TUBE -> {
                if (tubeStart != null && tubeEnd != null) {
                    Set<BlockPos> shell = new LinkedHashSet<>();
                    SculptGeometry.tube(tubeStart, tubeEnd, width, shell, new HashSet<>());
                    WallEffects.submitPreview(context, camera, shell, color, PREVIEW_ALPHA);
                } else if (!BlastEffects.isSustaining()) {
                    Vec3 point = crosshair(client, player);
                    if (point != null) WallEffects.submitPreview(context, camera, List.of(BlockPos.containing(point)), color, PREVIEW_ALPHA);
                }
            }
            default -> {
                if (BlastEffects.isSustaining()) return;
                Vec3 point = crosshair(client, player);
                if (point == null) return;

                float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
                Set<BlockPos> cells = new LinkedHashSet<>();
                SculptGeometry.ribbon(point, Mth.floor(point.y), SculptGeometry.flatDirection(player.getViewYRot(partialTicks)), width, cells);
                WallEffects.submitPreview(context, camera, cells, color, PREVIEW_ALPHA);
            }
        }
    }

    private static @Nullable Vec3 crosshair(Minecraft client, LocalPlayer player) {
        float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        return SculptGeometry.trace(client.level, player, player.getEyePosition(partialTicks), player.getViewVector(partialTicks), Double.NaN);
    }

    private static void renderCage(LevelRenderContext context, Vec3 camera, int color) {
        if (RING.isEmpty()) return;

        int tint = VoxelRenderer.toWhite(ARGB.opaque(color), 0.4F);
        List<ShieldEffects.Voxel> ring = new ArrayList<>(RING.size());
        for (Vec3 point : RING) ring.add(new ShieldEffects.Voxel(VoxelRenderer.snap(point), RING_HALF, tint));
        ShieldEffects.submit(context, camera, ring, 0.9F);

        SculptGeometry.Cage cage = SculptGeometry.cage(RING);
        if (cage != null) WallEffects.submitPreview(context, camera, SculptGeometry.dome(cage), color, DOME_PREVIEW_ALPHA);
    }

    private static void extractLegend(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || !ArmedRingPower.isArmed(player) || !PowerRingItem.hasCharge(player) || !isSelected(player)) return;

        Font font = client.font;
        int color = ARGB.opaque(CorpsColors.of(player));
        SculptShape[] shapes = SculptShape.values();
        Component widthLine = Component.translatable("hud.brightestday.sculpt_width", ConstructClient.size(RingPowerRegistry.SCULPT));
        Component hint = Component.translatable("hud.brightestday.sculpt_hint");

        int glyphWidth = 0;
        int nameWidth = Math.max(font.width(widthLine), font.width(hint));
        for (SculptShape entry : shapes) {
            glyphWidth = Math.max(glyphWidth, font.width(entry.glyph()));
            nameWidth = Math.max(nameWidth, font.width(Component.translatable(entry.translationKey())));
        }

        int width = LEGEND_PADDING * 3 + glyphWidth + nameWidth;
        int height = LEGEND_PADDING * 2 + (shapes.length + 2) * LEGEND_LINE + LEGEND_PADDING;
        int left = graphics.guiWidth() - width - LEGEND_MARGIN;
        int top = graphics.guiHeight() / 2 - height / 2;

        graphics.fill(left, top, left + width, top + height, 0x900A140C);
        graphics.fill(left, top, left + 1, top + height, color);
        int y = top + LEGEND_PADDING;
        for (SculptShape entry : shapes) {
            boolean selected = entry == shape;
            if (selected) graphics.fill(left + 1, y - 2, left + width, y + LEGEND_LINE - 2, ARGB.color(0x50, color));
            graphics.text(font, entry.glyph(), left + LEGEND_PADDING, y, VoxelRenderer.toWhite(color, selected ? 0.6F : 0.3F), true);
            graphics.text(font, Component.translatable(entry.translationKey()), left + LEGEND_PADDING * 2 + glyphWidth, y, selected ? 0xFFFFFFFF : 0xFF9AA89C, true);
            y += LEGEND_LINE;
        }

        y += LEGEND_PADDING;
        graphics.text(font, widthLine, left + LEGEND_PADDING, y, 0xFFE8F4EA, true);
        graphics.text(font, hint, left + LEGEND_PADDING, y + LEGEND_LINE, 0xFF7A887C, true);
    }

    private SculptClient() {}
}
