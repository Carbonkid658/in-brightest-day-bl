package dev.amble.client.effects.attacks.utility;

import dev.amble.client.effects.ShieldEffects;
import dev.amble.client.effects.VoxelRenderer;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.networking.payloads.s2c.LumberjackS2CPayload;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class LumberjackEffects {
    private static final float CELL = 2.0F * VoxelRenderer.PIXEL;
    private static final int POP_TICKS = 6;
    private static final int FADE_TICKS = 6;
    private static final int EXPIRE_GRACE = 20;
    private static final int WINDUP_TICKS = 8;
    private static final int CHOP_TICKS = 5;
    private static final float STRIKE_PORTION = 0.35F;
    private static final float REST_ANGLE = 0.2F;
    private static final float RAISED_ANGLE = -0.7F;
    private static final float STRIKE_ANGLE = 1.15F;

    private static final List<int[]> SHAPE = buildShape();
    private static final Map<Integer, ClientAxe> AXES = new HashMap<>();

    private static final class ClientAxe {
        final int ownerId;
        final Vec3 anchor;
        final float yaw;
        final int color;
        final int remaining;
        final @Nullable ClientLevel level;
        int age;
        int fade = -1;

        ClientAxe(LumberjackS2CPayload payload, @Nullable ClientLevel level) {
            this.ownerId = payload.ownerId();
            this.anchor = payload.anchor();
            this.yaw = payload.yaw();
            this.color = ARGB.opaque(payload.color());
            this.remaining = payload.remaining();
            this.level = level;
        }
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(LumberjackS2CPayload.TYPE, (payload, context) -> {
            if (payload.present()) {
                AXES.put(payload.id(), new ClientAxe(payload, context.client().level));
                return;
            }
            ClientAxe axe = AXES.get(payload.id());
            if (axe != null && axe.fade < 0) axe.fade = 0;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> AXES.clear());
        ClientTickEvents.END_CLIENT_TICK.register(LumberjackEffects::tick);
        LevelRenderEvents.COLLECT_SUBMITS.register(LumberjackEffects::render);
    }

    public static void snapshot(Consumer<CustomPacketPayload> out) {
        AXES.forEach((id, axe) -> {
            if (axe.fade < 0) out.accept(new LumberjackS2CPayload(id, axe.ownerId, axe.anchor, axe.yaw, axe.color, Math.max(axe.remaining - axe.age, 0), true));
        });
    }

    private static void tick(Minecraft client) {
        if (client.isPaused()) return;
        Iterator<ClientAxe> iterator = AXES.values().iterator();
        while (iterator.hasNext()) {
            ClientAxe axe = iterator.next();
            if (axe.level != client.level) {
                iterator.remove();
                continue;
            }
            axe.age++;
            if (axe.fade < 0 && axe.age > axe.remaining + EXPIRE_GRACE) axe.fade = 0;
            if (axe.fade >= 0 && ++axe.fade > FADE_TICKS) iterator.remove();
        }
    }

    private static void render(LevelRenderContext context) {
        if (AXES.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 camera = context.levelState().cameraRenderState.pos;

        for (ClientAxe axe : AXES.values()) {
            float time = axe.age + partialTicks;
            float scale = Mth.clamp(time / POP_TICKS, 0.0F, 1.0F);
            if (axe.fade >= 0) scale *= 1.0F - Mth.clamp((axe.fade + partialTicks) / FADE_TICKS, 0.0F, 1.0F);
            if (scale <= 0.01F) continue;
            ShieldEffects.submit(context, camera, axe(client, axe, time, scale), 1.0F);
        }
    }

    private static List<ShieldEffects.Voxel> axe(Minecraft client, ClientAxe axe, float time, float scale) {
        int color = axeColor(client, axe);
        float angle = swing(axe, time);
        Vec3 forward = new Vec3(Mth.sin(axe.yaw), 0.0, Mth.cos(axe.yaw));
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 handle = up.scale(Mth.cos(angle)).add(forward.scale(Mth.sin(angle)));
        Vec3 blade = forward.scale(Mth.cos(angle)).subtract(up.scale(Mth.sin(angle)));
        float step = CELL * scale;
        float half = VoxelRenderer.snapSize(CELL * 0.45F * scale);

        List<ShieldEffects.Voxel> voxels = new ArrayList<>(SHAPE.size());
        for (int[] cell : SHAPE) {
            Vec3 point = axe.anchor.add(handle.scale(cell[0] * step)).add(blade.scale(cell[1] * step));
            float shine = cell[2] == 1 ? 0.6F : 0.12F + 0.08F * Mth.sin(time * 0.3F + cell[0]);
            voxels.add(new ShieldEffects.Voxel(VoxelRenderer.snap(point), half, VoxelRenderer.toWhite(color, shine)));
        }
        return voxels;
    }

    private static float swing(ClientAxe axe, float time) {
        if (axe.fade >= 0) return REST_ANGLE;
        if (time < WINDUP_TICKS) return Mth.lerp(time / WINDUP_TICKS, REST_ANGLE, RAISED_ANGLE);
        float phase = ((time - WINDUP_TICKS) % CHOP_TICKS) / CHOP_TICKS;
        if (phase < STRIKE_PORTION) {
            float t = phase / STRIKE_PORTION;
            return Mth.lerp(t * t, RAISED_ANGLE, STRIKE_ANGLE);
        }
        float t = (phase - STRIKE_PORTION) / (1.0F - STRIKE_PORTION);
        return Mth.lerp(t, STRIKE_ANGLE, RAISED_ANGLE);
    }

    private static int axeColor(Minecraft client, ClientAxe axe) {
        if (client.level != null && client.level.getEntity(axe.ownerId) instanceof Player owner && PowerRingItem.getWornCorps(owner).isPresent()) {
            return ARGB.opaque(CorpsColors.of(owner));
        }
        return axe.color;
    }

    private static List<int[]> buildShape() {
        List<int[]> cells = new ArrayList<>();
        for (int a = -2; a <= 10; a++) cells.add(new int[]{a, 0, a == -2 ? 1 : 0});
        for (int a = 7; a <= 10; a++) cells.add(new int[]{a, -1, 0});
        for (int a = 7; a <= 10; a++) cells.add(new int[]{a, 1, 0});
        for (int a = 6; a <= 11; a++) cells.add(new int[]{a, 2, 0});
        for (int a = 5; a <= 12; a++) cells.add(new int[]{a, 3, 0});
        for (int a = 5; a <= 12; a++) cells.add(new int[]{a, 4, 1});
        cells.add(new int[]{11, 0, 0});
        return List.copyOf(cells);
    }

    private LumberjackEffects() {}
}
