package dev.amble.client.effects;

import dev.amble.core.items.PowerRingItem;
import dev.amble.core.networking.payloads.s2c.LightOrbS2CPayload;
import dev.amble.core.ringpowers.CorpsColors;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class LightOrbEffects {
    private static final int POP_TICKS = 8;
    private static final int FADE_TICKS = 6;
    private static final float BASE_RADIUS = 0.25F;
    private static final float RADIUS_PER_SIZE = 0.08F;
    private static final float CORE_SCALE = 0.45F;

    private static final Map<Integer, ClientOrb> ORBS = new HashMap<>();

    private static final class ClientOrb {
        final Vec3 center;
        final int size;
        final int color;
        final int casterId;
        final @Nullable ClientLevel level;
        int age;
        int fade = -1;

        ClientOrb(LightOrbS2CPayload payload, @Nullable ClientLevel level) {
            this.center = payload.center();
            this.size = payload.size();
            this.color = ARGB.opaque(payload.color());
            this.casterId = payload.casterId();
            this.level = level;
        }
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(LightOrbS2CPayload.TYPE, (payload, context) -> {
            if (payload.present()) {
                ORBS.put(payload.id(), new ClientOrb(payload, context.client().level));
                return;
            }
            ClientOrb orb = ORBS.get(payload.id());
            if (orb != null && orb.fade < 0) orb.fade = 0;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ORBS.clear());
        ClientTickEvents.END_CLIENT_TICK.register(LightOrbEffects::tick);
        LevelRenderEvents.COLLECT_SUBMITS.register(LightOrbEffects::render);
    }

    private static void tick(Minecraft client) {
        if (client.isPaused()) return;
        Iterator<ClientOrb> iterator = ORBS.values().iterator();
        while (iterator.hasNext()) {
            ClientOrb orb = iterator.next();
            if (orb.level != client.level) {
                iterator.remove();
                continue;
            }
            orb.age++;
            if (orb.fade >= 0 && ++orb.fade > FADE_TICKS) iterator.remove();
        }
    }

    private static void render(LevelRenderContext context) {
        if (ORBS.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 camera = context.levelState().cameraRenderState.pos;

        for (ClientOrb orb : ORBS.values()) {
            float time = orb.age + partialTicks;
            float scale = Mth.clamp(time / POP_TICKS, 0.0F, 1.0F);
            if (orb.fade >= 0) scale *= 1.0F - Mth.clamp((orb.fade + partialTicks) / FADE_TICKS, 0.0F, 1.0F);
            if (scale <= 0.01F) continue;

            int color = orb.color;
            if (client.level.getEntity(orb.casterId) instanceof Player caster && PowerRingItem.getWornCorps(caster).isPresent()) {
                color = ARGB.opaque(CorpsColors.of(caster));
            }
            float radius = BASE_RADIUS + RADIUS_PER_SIZE * orb.size;
            float pulse = 1.0F + 0.06F * Mth.sin(time * 0.15F);
            List<ShieldEffects.Voxel> voxels = new ArrayList<>(ShieldEffects.sphere(orb.center, radius * scale * pulse, radius, time, color));
            voxels.add(new ShieldEffects.Voxel(orb.center, VoxelRenderer.snapSize(radius * CORE_SCALE * scale), VoxelRenderer.toWhite(color, 0.7F)));
            ShieldEffects.submit(context, camera, voxels, 1.0F);
        }
    }

    private LightOrbEffects() {}
}
