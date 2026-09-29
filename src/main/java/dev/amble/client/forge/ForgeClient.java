package dev.amble.client.forge;

import dev.amble.BrightestDay;
import dev.amble.client.effects.BlastEffects;
import dev.amble.client.effects.ConstructClient;
import dev.amble.client.effects.ShieldEffects;
import dev.amble.client.effects.VoxelRenderer;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.networking.payloads.c2s.ForgeC2SPayload;
import dev.amble.core.ringpowers.CorpsColors;
import dev.amble.core.ringpowers.constructs.ConstructRingPower;
import dev.amble.core.ringpowers.constructs.ConstructTool;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ForgeClient {
    private static final float DRAW_DISTANCE = 2.5F;
    private static final float SAMPLE_DEGREES = 0.5F;
    private static final float MIN_STROKE_DEGREES = 12.0F;
    private static final float VOXEL_SPACING = 1.5F * VoxelRenderer.PIXEL;
    private static final float VOXEL_SIZE = 1.5F * VoxelRenderer.PIXEL;
    private static final int RELEASE_TICKS = 8;
    private static final int LEGEND_MARGIN = 6;
    private static final int LEGEND_PADDING = 5;
    private static final int LEGEND_LINE = 10;

    private static final List<float[]> STROKE = new ArrayList<>();
    private static final List<Vec3> DIRECTIONS = new ArrayList<>();
    private static boolean drawing;
    private static boolean succeeded;
    private static int releaseAge = -1;
    private static float yaw0;
    private static float pitch0;
    private static long seed;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(ForgeClient::tick);
        LevelRenderEvents.COLLECT_SUBMITS.register(ForgeClient::render);
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, BrightestDay.id("forge_legend"), ForgeClient::extractLegend);
    }

    public static boolean wantsToDraw(LocalPlayer player) {
        return player.getMainHandItem().isEmpty()
                && ArmedRingPower.isArmed(player)
                && PowerRingItem.hasCharge(player)
                && ArmedRingPower.selectedConstruct(player).map(ConstructRingPower::usesGesture).orElse(false)
                && !ConstructClient.isLookingAtLantern();
    }

    public static float drawAmount() {
        return drawing ? 1.0F : 0.0F;
    }

    private static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (releaseAge >= 0 && ++releaseAge > RELEASE_TICKS) {
            releaseAge = -1;
            STROKE.clear();
            DIRECTIONS.clear();
        }
        if (player == null || client.isPaused()) return;

        boolean down = client.gui.screen() == null && client.options.keyUse.isDown() && wantsToDraw(player);
        if (down && !drawing) {
            drawing = true;
            releaseAge = -1;
            STROKE.clear();
            DIRECTIONS.clear();
            yaw0 = player.getYRot();
            pitch0 = player.getXRot();
            play(client, SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F);
        } else if (!down && drawing) {
            finish(client);
        }
    }

    private static void finish(Minecraft client) {
        drawing = false;
        releaseAge = 0;
        seed = RandomSource.create().nextLong();

        Optional<ConstructTool> tool = GestureRecognizer.length(STROKE) >= MIN_STROKE_DEGREES
                ? GestureRecognizer.recognize(STROKE)
                : Optional.empty();
        succeeded = tool.isPresent();
        if (tool.isPresent()) {
            ClientPlayNetworking.send(new ForgeC2SPayload(tool.get().ordinal()));
        } else {
            client.gui.hud.setOverlayMessage(Component.translatable("message.brightestday.unknown_pattern"), false);
            play(client, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2F);
        }
    }

    private static void play(Minecraft client, SoundEvent sound, float pitch) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) return;
        client.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), sound, SoundSource.PLAYERS, 0.6F, pitch, false);
    }

    private static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || (!drawing && releaseAge < 0)) return;

        float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        if (drawing) sample(player, partialTicks);
        if (DIRECTIONS.isEmpty()) return;

        Vec3 eye = player.getEyePosition(partialTicks);
        int color = ARGB.opaque(CorpsColors.of(player));
        List<Vec3> path = new ArrayList<>(DIRECTIONS.size());
        for (Vec3 direction : DIRECTIONS) path.add(eye.add(direction.scale(DRAW_DISTANCE)));

        float release = releaseAge < 0 ? 0.0F : Mth.clamp((releaseAge + partialTicks) / RELEASE_TICKS, 0.0F, 1.0F);
        Vec3 hand = BlastEffects.hand(player, partialTicks);
        RandomSource random = RandomSource.create(seed);
        int tint = VoxelRenderer.toWhite(color, 0.2F + 0.6F * release);
        float half = VoxelRenderer.snapSize(VOXEL_SIZE * (1.0F - 0.6F * release) * 0.5F);

        List<ShieldEffects.Voxel> voxels = new ArrayList<>();
        for (int i = 1; i < path.size(); i++) {
            Vec3 from = path.get(i - 1);
            Vec3 to = path.get(i);
            int steps = Math.max(Mth.ceil(from.distanceTo(to) / VOXEL_SPACING), 1);
            for (int step = 0; step < steps; step++) {
                Vec3 point = from.lerp(to, (double) step / steps);
                if (release > 0.0F) {
                    Vec3 scatter = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).scale(0.6);
                    point = succeeded ? point.lerp(hand, release * release) : point.add(scatter.scale(release));
                }
                voxels.add(new ShieldEffects.Voxel(VoxelRenderer.snap(point), half, tint));
            }
        }
        ShieldEffects.submit(context, context.levelState().cameraRenderState.pos, voxels, 1.0F - (succeeded ? 0.0F : release));
    }

    private static void sample(LocalPlayer player, float partialTicks) {
        float yaw = player.getViewYRot(partialTicks);
        float pitch = player.getViewXRot(partialTicks);
        float[] point = {Mth.wrapDegrees(yaw - yaw0), -(pitch - pitch0)};
        if (!STROKE.isEmpty()) {
            float[] last = STROKE.getLast();
            if (Math.abs(point[0] - last[0]) + Math.abs(point[1] - last[1]) < SAMPLE_DEGREES) return;
        }
        STROKE.add(point);
        DIRECTIONS.add(Vec3.directionFromRotation(pitch, yaw));
    }

    private static void extractLegend(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || !ArmedRingPower.isArmed(player) || !PowerRingItem.hasCharge(player)) return;
        if (!ArmedRingPower.selectedConstruct(player).map(ConstructRingPower::usesGesture).orElse(false)) return;

        Font font = client.font;
        int color = ARGB.opaque(CorpsColors.of(player));
        ConstructTool[] tools = ConstructTool.values();
        int glyphWidth = 0;
        int nameWidth = 0;
        for (ConstructTool tool : tools) {
            glyphWidth = Math.max(glyphWidth, font.width(tool.glyph()));
            nameWidth = Math.max(nameWidth, font.width(Component.translatable(tool.translationKey())));
        }

        int width = LEGEND_PADDING * 3 + glyphWidth + nameWidth;
        int height = LEGEND_PADDING * 2 + tools.length * LEGEND_LINE;
        int left = graphics.guiWidth() - width - LEGEND_MARGIN;
        int top = graphics.guiHeight() / 2 - height / 2;

        graphics.fill(left, top, left + width, top + height, 0x900A140C);
        graphics.fill(left, top, left + 1, top + height, color);
        int y = top + LEGEND_PADDING;
        for (ConstructTool tool : tools) {
            graphics.text(font, tool.glyph(), left + LEGEND_PADDING, y, VoxelRenderer.toWhite(color, 0.3F), true);
            graphics.text(font, Component.translatable(tool.translationKey()), left + LEGEND_PADDING * 2 + glyphWidth, y, 0xFFE8F4EA, true);
            y += LEGEND_LINE;
        }
    }

    private ForgeClient() {}
}
