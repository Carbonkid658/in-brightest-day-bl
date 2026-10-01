package dev.amble.client.effects.glide;

import dev.amble.client.effects.BlastEffects;
import dev.amble.client.effects.ShieldEffects;
import dev.amble.client.effects.VoxelRenderer;
import dev.amble.core.glide.GlideManager;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.networking.payloads.s2c.GlideBoltS2CPayload;
import dev.amble.core.networking.payloads.s2c.GlideS2CPayload;
import dev.amble.core.ringpowers.CorpsColors;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class GlideEffects {
    private static final int FADE_IN_TICKS = 10;
    private static final int FADE_OUT_TICKS = 8;
    private static final int FLICKER_TICKS = 100;
    private static final int BOLT_TICKS = 8;
    private static final int TRAIL_STEPS = 6;
    private static final float TRAIL_SPACING = 0.05F;
    private static final float HALF_SPAN = 1.4F;
    private static final float NOSE = 0.5F;
    private static final float TAIL = -0.6F;
    private static final float NOTCH = 0.2F;
    private static final float DIHEDRAL = 0.18F;
    private static final float FLAP = 0.06F;
    private static final float STEP = 0.125F;
    private static final float BOLT_SIZE = 0.35F;
    private static final float BACK_ANCHOR = 0.62F;
    private static final float BACK_GAP = 0.12F;
    private static final float PLAYER_HEIGHT = 1.8F;
    private static final float PLAYER_WIDTH = 0.6F;

    private static final Map<Integer, ClientGlide> GLIDES = new HashMap<>();
    private static final List<Bolt> BOLTS = new ArrayList<>();

    private static final class ClientGlide {
        final @Nullable ClientLevel level;
        int casterId;
        int color;
        int remaining;
        int age;
        int fade = -1;

        ClientGlide(GlideS2CPayload payload, @Nullable ClientLevel level) {
            this.level = level;
            this.update(payload);
        }

        void update(GlideS2CPayload payload) {
            this.casterId = payload.casterId();
            this.color = ARGB.opaque(payload.color());
            this.remaining = payload.remaining();
        }
    }

    private record Frame(Vec3 anchor, Vec3 right, Vec3 length, Vec3 normal, float size) {}

    private static final class Bolt {
        final int casterId;
        final int targetId;
        final int color;
        final Vec3 start;
        final Vec3 look;
        final @Nullable ClientLevel level;
        int age;

        Bolt(GlideBoltS2CPayload payload, Vec3 start, Vec3 look, @Nullable ClientLevel level) {
            this.casterId = payload.casterId();
            this.targetId = payload.targetId();
            this.color = ARGB.opaque(payload.color());
            this.start = start;
            this.look = look;
            this.level = level;
        }
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(GlideS2CPayload.TYPE, (payload, context) -> receive(payload, context.client().level));
        ClientPlayNetworking.registerGlobalReceiver(GlideBoltS2CPayload.TYPE, (payload, context) -> launch(payload, context.client().level));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            GLIDES.clear();
            BOLTS.clear();
            GlideManager.clearClient();
        });
        ClientTickEvents.END_CLIENT_TICK.register(GlideEffects::tick);
        LevelRenderEvents.COLLECT_SUBMITS.register(GlideEffects::render);
    }

    public static void snapshot(Consumer<CustomPacketPayload> out) {
        GLIDES.forEach((id, glide) -> {
            if (glide.fade < 0) out.accept(new GlideS2CPayload(id, glide.casterId, glide.color, glide.remaining, true));
        });
    }

    private static void receive(GlideS2CPayload payload, @Nullable ClientLevel level) {
        GlideManager.setClient(payload.entityId(), payload.present());
        ClientGlide glide = GLIDES.get(payload.entityId());
        if (!payload.present()) {
            if (glide != null && glide.fade < 0) glide.fade = 0;
            return;
        }
        if (glide == null || glide.level != level) {
            GLIDES.put(payload.entityId(), new ClientGlide(payload, level));
            return;
        }
        glide.update(payload);
        if (glide.fade >= 0) {
            glide.fade = -1;
            glide.age = 0;
        }
    }

    private static void launch(GlideBoltS2CPayload payload, @Nullable ClientLevel level) {
        if (level == null) return;
        Entity caster = level.getEntity(payload.casterId());
        if (caster == null) return;
        Vec3 start = caster instanceof Player player ? BlastEffects.hand(player, 1.0F) : caster.getBoundingBox().getCenter();
        BOLTS.add(new Bolt(payload, start, caster.getViewVector(1.0F), level));
    }

    private static void tick(Minecraft client) {
        if (client.isPaused()) return;
        Iterator<Map.Entry<Integer, ClientGlide>> iterator = GLIDES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, ClientGlide> entry = iterator.next();
            ClientGlide glide = entry.getValue();
            if (glide.level != client.level) {
                iterator.remove();
                GlideManager.setClient(entry.getKey(), false);
                continue;
            }
            glide.age++;
            if (glide.remaining > 0) glide.remaining--;
            if (glide.fade < 0 && glide.remaining <= 0) {
                glide.fade = 0;
                GlideManager.setClient(entry.getKey(), false);
            }
            if (glide.fade >= 0 && ++glide.fade > FADE_OUT_TICKS) iterator.remove();
        }
        BOLTS.removeIf(bolt -> bolt.level != client.level || ++bolt.age >= BOLT_TICKS);
    }

    private static void render(LevelRenderContext context) {
        if (GLIDES.isEmpty() && BOLTS.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 camera = context.levelState().cameraRenderState.pos;
        boolean firstPerson = client.options.getCameraType().isFirstPerson();

        for (Map.Entry<Integer, ClientGlide> entry : GLIDES.entrySet()) {
            ClientGlide glide = entry.getValue();
            Entity entity = client.level.getEntity(entry.getKey());
            if (entity == null || firstPerson && entity == client.getCameraEntity()) continue;

            float time = glide.age + partialTicks;
            float unfold = ease(Mth.clamp(time / FADE_IN_TICKS, 0.0F, 1.0F));
            float alpha = unfold;
            if (glide.fade >= 0) alpha *= 1.0F - Mth.clamp((glide.fade + partialTicks) / FADE_OUT_TICKS, 0.0F, 1.0F);
            if (glide.fade < 0 && glide.remaining < FLICKER_TICKS) alpha *= flicker(time, glide.remaining);
            if (alpha <= 0.01F) continue;

            int color = liveColor(client, glide.casterId, glide.color);
            List<ShieldEffects.Voxel> voxels = wing(frame(entity, partialTicks), unfold, time, color);
            ShieldEffects.submit(context, camera, voxels, alpha);
        }

        for (Bolt bolt : BOLTS) {
            Entity target = client.level.getEntity(bolt.targetId);
            if (target == null) continue;
            int color = liveColor(client, bolt.casterId, bolt.color);
            ShieldEffects.submit(context, camera, bolt(bolt, target, partialTicks, color), 1.0F);
        }
    }

    private static int liveColor(Minecraft client, int casterId, int fallback) {
        if (client.level != null && client.level.getEntity(casterId) instanceof Player caster && PowerRingItem.getWornCorps(caster).isPresent()) {
            return ARGB.opaque(CorpsColors.of(caster));
        }
        return fallback;
    }

    private static float flicker(float time, int remaining) {
        float urgency = 1.0F - remaining / (float) FLICKER_TICKS;
        float wave = Mth.sin(time * (0.35F + urgency * 1.4F));
        return wave > 0.3F - urgency ? 1.0F : 0.25F;
    }

    private static float ease(float t) {
        float u = 1.0F - t;
        return 1.0F - u * u * u;
    }

    private static Frame frame(Entity entity, float partialTicks) {
        Vec3 position = entity.getPosition(partialTicks);
        float yaw = (entity instanceof LivingEntity living ? Mth.rotLerp(partialTicks, living.yBodyRotO, living.yBodyRot) : entity.getViewYRot(partialTicks)) * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        Vec3 up = new Vec3(0.0, 1.0, 0.0);

        if (!(entity instanceof Player) && entity.getBbHeight() < entity.getBbWidth() * 1.5F) {
            float size = Mth.clamp(entity.getBbWidth() / PLAYER_WIDTH, 0.6F, 3.0F);
            Vec3 right = forward.cross(up).normalize();
            return new Frame(position.add(0.0, entity.getBbHeight() + BACK_GAP, 0.0), right, forward, up, size);
        }

        float scale = entity instanceof LivingEntity living ? living.getScale() : 1.0F;
        float size = entity instanceof Player ? scale : Mth.clamp(entity.getBbWidth() / PLAYER_WIDTH, 0.6F, 3.0F);
        float height = entity instanceof Player ? PLAYER_HEIGHT * scale : entity.getBbHeight();
        Vec3 back = forward.scale(-1.0);
        Vec3 length = up;

        if (entity instanceof LivingEntity living && living.isFallFlying()) {
            float ticks = living.getFallFlyingTicks() + partialTicks;
            float blend = Mth.clamp(ticks * ticks / 100.0F, 0.0F, 1.0F);
            Vec3 look = entity.getViewVector(partialTicks);
            Vec3 lift = up.subtract(look.scale(look.y));
            Vec3 glideBack = lift.lengthSqr() < 1.0E-4 ? back : lift.normalize();
            length = up.lerp(look, blend).normalize();
            back = back.lerp(glideBack, blend).normalize();
        }

        Vec3 right = length.cross(back).normalize();
        back = right.cross(length).normalize();
        Vec3 anchor = position.add(length.scale(height * BACK_ANCHOR)).add(back.scale(PLAYER_WIDTH * 0.5F * size + BACK_GAP));
        return new Frame(anchor, right, length, back, size);
    }

    private static List<ShieldEffects.Voxel> wing(Frame frame, float unfold, float time, int color) {
        float size = frame.size();
        float step = STEP * size;
        float halfSpan = HALF_SPAN * size;
        float nose = NOSE * size;
        float tail = TAIL * size;
        float edgeHalf = VoxelRenderer.snapSize(step * 0.45F);
        float fillHalf = VoxelRenderer.snapSize(step * 0.3F);
        int columns = Mth.ceil(halfSpan / step);
        int rows = Mth.ceil((nose - tail) / step);

        List<ShieldEffects.Voxel> voxels = new ArrayList<>();
        for (int column = -columns; column <= columns; column++) {
            for (int row = 0; row <= rows; row++) {
                float s = column * step;
                float a = tail + row * step;
                if (!inside(s, a, halfSpan, nose, tail, size)) continue;

                boolean edge = !inside(s + step, a, halfSpan, nose, tail, size) || !inside(s - step, a, halfSpan, nose, tail, size)
                        || !inside(s, a + step, halfSpan, nose, tail, size) || !inside(s, a - step, halfSpan, nose, tail, size);
                boolean spar = column == 0;
                float reach = Math.abs(s) / halfSpan;
                float lift = DIHEDRAL * size * reach + FLAP * size * reach * reach * Mth.sin(time * 0.12F);
                Vec3 point = frame.anchor()
                        .add(frame.right().scale(s * unfold))
                        .add(frame.length().scale(a))
                        .add(frame.normal().scale(lift));

                int tint = edge || spar
                        ? VoxelRenderer.toWhite(color, 0.35F + 0.1F * Mth.sin(time * 0.3F + column))
                        : VoxelRenderer.toWhite(color, 0.1F + 0.1F * Mth.sin(time * 0.25F + column * 0.7F + row));
                voxels.add(new ShieldEffects.Voxel(VoxelRenderer.snap(point), edge || spar ? edgeHalf : fillHalf, tint));
            }
        }
        return voxels;
    }

    private static boolean inside(float s, float a, float halfSpan, float nose, float tail, float size) {
        if (a > nose) return false;
        float reach = Math.abs(s) / halfSpan;
        if (reach > 1.0F) return false;
        float trailing = tail + NOTCH * size * (1.0F - reach * reach);
        if (a < trailing) return false;
        return Math.abs(s) <= halfSpan * (nose - a) / (nose - tail);
    }

    private static List<ShieldEffects.Voxel> bolt(Bolt bolt, Entity target, float partialTicks, int color) {
        Vec3 end = target.getPosition(partialTicks).add(0.0, target.getBbHeight() * 0.6, 0.0);
        Vec3 control = bolt.start.add(bolt.look.scale(bolt.start.distanceTo(end) * 0.5));
        float progress = Mth.clamp((bolt.age + partialTicks) / BOLT_TICKS, 0.0F, 1.0F);
        float time = bolt.age + partialTicks;

        List<ShieldEffects.Voxel> voxels = new ArrayList<>();
        for (int i = TRAIL_STEPS; i >= 1; i--) {
            float t = progress - i * TRAIL_SPACING;
            if (t <= 0.0F) continue;
            float fade = 1.0F - i / (float) (TRAIL_STEPS + 1);
            voxels.add(new ShieldEffects.Voxel(VoxelRenderer.snap(curve(bolt.start, control, end, t)), VoxelRenderer.snapSize(0.05F * fade + 0.02F), VoxelRenderer.toWhite(color, 0.3F * fade)));
        }

        Vec3 head = curve(bolt.start, control, end, progress);
        Vec3 direction = tangent(bolt.start, control, end, progress);
        direction = direction.lengthSqr() < 1.0E-6 ? bolt.look : direction.normalize();
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 right = direction.cross(up);
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
        Vec3 normal = right.cross(direction).normalize();
        voxels.addAll(wing(new Frame(head, right, direction, normal, BOLT_SIZE), 1.0F, time, color));
        return voxels;
    }

    private static Vec3 curve(Vec3 start, Vec3 control, Vec3 end, float t) {
        float u = 1.0F - t;
        return start.scale(u * u).add(control.scale(2.0F * u * t)).add(end.scale(t * t));
    }

    private static Vec3 tangent(Vec3 start, Vec3 control, Vec3 end, float t) {
        return control.subtract(start).scale(2.0F * (1.0F - t)).add(end.subtract(control).scale(2.0F * t));
    }

    private GlideEffects() {}
}
