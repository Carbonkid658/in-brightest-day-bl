package dev.amble.client.effects.attacks.weapon;

import dev.amble.client.effects.BlastEffects;
import dev.amble.client.effects.ShieldEffects;
import dev.amble.client.effects.VoxelRenderer;
import dev.amble.core.attacks.weapon.WhipManager;
import dev.amble.core.networking.payloads.s2c.WhipS2CPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class WhipEffects {
    private static final int RETRACT_TICKS = 5;
    private static final float EXTEND_TICKS = 1.5F;
    private static final float LAG = 0.35F;
    private static final float SPACING = 0.16F;
    private static final float ROOT_SIZE = 4.0F * VoxelRenderer.PIXEL;
    private static final float END_SIZE = 2.0F * VoxelRenderer.PIXEL;
    private static final float TIP_SIZE = 5.0F * VoxelRenderer.PIXEL;
    private static final float RIPPLE = 0.22F;
    private static final float SAG = 0.35F;
    private static final int CRACK_VOXELS = 10;
    private static final int CRACK_TICKS = 4;
    private static final float CRACK_RADIUS = 0.8F;

    private static final List<Whip> WHIPS = new ArrayList<>();

    private static final class Whip {
        final int playerId;
        final Vec3 forward;
        final Vec3 side;
        final Vec3 up;
        final int color;
        final boolean lash;
        final float length;
        final float arc;
        final float sweepSide;
        final int ticks;
        final @Nullable ClientLevel level;
        int age;

        Whip(WhipS2CPayload payload, @Nullable ClientLevel level) {
            this.playerId = payload.playerId();
            this.forward = payload.direction().normalize();
            this.side = WhipManager.side(this.forward);
            this.up = this.side.cross(this.forward).normalize();
            this.color = ARGB.opaque(payload.color());
            this.lash = payload.lash();
            this.length = payload.length();
            this.arc = payload.arc();
            this.sweepSide = payload.side();
            this.ticks = Math.max(payload.ticks(), 1);
            this.level = level;
        }
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(WhipS2CPayload.TYPE, (payload, context) -> {
            if (context.client().level != null) WHIPS.add(new Whip(payload, context.client().level));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WHIPS.clear());
        ClientTickEvents.END_CLIENT_TICK.register(WhipEffects::tick);
        LevelRenderEvents.COLLECT_SUBMITS.register(WhipEffects::render);
    }

    private static void tick(Minecraft client) {
        if (client.isPaused()) return;
        Iterator<Whip> iterator = WHIPS.iterator();
        while (iterator.hasNext()) {
            Whip whip = iterator.next();
            if (whip.level != client.level || ++whip.age > whip.ticks + Math.max(RETRACT_TICKS, CRACK_TICKS)) iterator.remove();
        }
    }

    private static void render(LevelRenderContext context) {
        if (WHIPS.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 camera = context.levelState().cameraRenderState.pos;

        for (Whip whip : WHIPS) {
            if (!(client.level.getEntity(whip.playerId) instanceof Player player)) continue;

            float time = whip.age + partialTicks;
            float progress = time / whip.ticks;
            float retract = Mth.clamp((time - whip.ticks) / RETRACT_TICKS, 0.0F, 1.0F);
            float extend = whip.lash ? easeOut(Mth.clamp(progress, 0.0F, 1.0F)) : Mth.clamp(time / EXTEND_TICKS, 0.3F, 1.0F);
            float length = whip.length * extend * (1.0F - retract);
            Vec3 hand = BlastEffects.hand(player, partialTicks);

            List<ShieldEffects.Voxel> voxels = new ArrayList<>();
            Vec3 tip = hand;
            if (length > 0.05F) {
                int count = Math.max(Mth.ceil(length / SPACING), 2);
                for (int i = 0; i <= count; i++) {
                    float s = (float) i / count;
                    Vec3 point = point(whip, hand, s, length, progress, time);
                    float half = VoxelRenderer.snapSize(Mth.lerp(s, ROOT_SIZE, END_SIZE) * 0.5F);
                    float shine = 0.15F + 0.5F * s * s + 0.1F * Mth.sin(time * 1.3F - i * 0.6F);
                    voxels.add(new ShieldEffects.Voxel(VoxelRenderer.snap(point), half, VoxelRenderer.toWhite(whip.color, shine)));
                    tip = point;
                }
                voxels.add(new ShieldEffects.Voxel(VoxelRenderer.snap(tip), VoxelRenderer.snapSize(TIP_SIZE * 0.5F), VoxelRenderer.toWhite(whip.color, 0.8F)));
            }
            voxels.add(new ShieldEffects.Voxel(VoxelRenderer.snap(hand), VoxelRenderer.snapSize(ROOT_SIZE * 0.75F), VoxelRenderer.toWhite(whip.color, 0.2F)));

            float crack = (time - whip.ticks) / CRACK_TICKS;
            if (crack >= 0.0F && crack < 1.0F) {
                Vec3 end = point(whip, hand, 1.0F, whip.length, progress, time);
                float radius = CRACK_RADIUS * easeOut(crack);
                float half = VoxelRenderer.snapSize(TIP_SIZE * 0.5F * (1.0F - crack));
                for (int i = 0; i < CRACK_VOXELS; i++) {
                    float angle = (float) i / CRACK_VOXELS * Mth.TWO_PI;
                    Vec3 offset = whip.up.scale(Mth.cos(angle) * radius).add(whip.side.scale(Mth.sin(angle) * radius))
                            .add(whip.forward.scale(Mth.sin(angle * 2.0F) * radius * 0.3F));
                    voxels.add(new ShieldEffects.Voxel(VoxelRenderer.snap(end.add(offset)), half, VoxelRenderer.toWhite(whip.color, 0.9F)));
                }
            }

            ShieldEffects.submit(context, camera, voxels, 1.0F - retract * retract);
        }
    }

    private static Vec3 point(Whip whip, Vec3 hand, float s, float length, float progress, float time) {
        float wave = RIPPLE * s * (1.0F - s) * 4.0F * Mth.sin(s * 9.0F - time * 2.2F) * Math.max(1.0F - progress, 0.15F);
        if (whip.lash) {
            Vec3 along = whip.forward.scale(s * length);
            return hand.add(along).add(whip.up.scale(wave)).add(whip.side.scale(wave * 0.5F));
        }

        float angle = WhipManager.sweepAngle(progress - LAG * s, whip.arc, whip.sweepSide);
        float sag = SAG * Mth.sin(s * Mth.PI) * (1.0F - Mth.clamp(progress, 0.0F, 1.0F) * 0.5F);
        Vec3 direction = whip.forward.scale(Mth.cos(angle)).add(whip.side.scale(Mth.sin(angle)));
        return hand.add(direction.scale(s * length)).add(whip.up.scale(wave - sag));
    }

    private static float easeOut(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t);
    }

    private WhipEffects() {}
}
