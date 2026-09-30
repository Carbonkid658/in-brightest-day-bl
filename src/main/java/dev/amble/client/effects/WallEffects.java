package dev.amble.client.effects;

import dev.amble.core.networking.payloads.s2c.WallRemoveS2CPayload;
import dev.amble.core.networking.payloads.s2c.WallSpawnS2CPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.CorpsColors;
import net.minecraft.core.BlockPos;
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

public final class WallEffects {
    private static final int POP_TICKS = 8;
    private static final int FADE_TICKS = 6;
    private static final float CELL_HALF = 0.49F;
    private static final float CELL_ALPHA = 0.85F;
    private static final float PREVIEW_HALF = 0.48F;

    private static final Map<Integer, ClientWall> WALLS = new HashMap<>();

    private static final class ClientWall {
        final List<BlockPos> cells;
        final int casterId;
        final int color;
        final int duration;
        final @Nullable ClientLevel level;
        int age;
        int fade = -1;

        ClientWall(WallSpawnS2CPayload payload, @Nullable ClientLevel level) {
            this.cells = payload.cells();
            this.casterId = payload.casterId();
            this.color = ARGB.opaque(payload.color());
            this.duration = payload.duration();
            this.age = payload.age();
            this.level = level;
        }
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(WallSpawnS2CPayload.TYPE, (payload, context) -> WALLS.put(payload.id(), new ClientWall(payload, context.client().level)));
        ClientPlayNetworking.registerGlobalReceiver(WallRemoveS2CPayload.TYPE, (payload, context) -> {
            ClientWall wall = WALLS.get(payload.id());
            if (wall != null && wall.fade < 0) wall.fade = 0;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WALLS.clear());
        ClientTickEvents.END_CLIENT_TICK.register(WallEffects::tick);
        LevelRenderEvents.COLLECT_SUBMITS.register(WallEffects::render);
    }

    private static void tick(Minecraft client) {
        if (client.isPaused()) return;
        Iterator<ClientWall> iterator = WALLS.values().iterator();
        while (iterator.hasNext()) {
            ClientWall wall = iterator.next();
            if (wall.level != client.level) {
                iterator.remove();
                continue;
            }
            wall.age++;
            if (wall.fade < 0 && wall.duration >= 0 && wall.age > wall.duration + 20) wall.fade = 0;
            if (wall.fade >= 0 && ++wall.fade > FADE_TICKS) iterator.remove();
        }
    }

    private static void render(LevelRenderContext context) {
        if (WALLS.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 camera = context.levelState().cameraRenderState.pos;

        for (ClientWall wall : WALLS.values()) {
            float time = wall.age + partialTicks;
            float fade = wall.fade < 0 ? 1.0F : 1.0F - Mth.clamp((wall.fade + partialTicks) / FADE_TICKS, 0.0F, 1.0F);
            if (fade <= 0.01F) continue;

            int color = liveColor(client.level, wall);
            List<ShieldEffects.Voxel> cells = new ArrayList<>(wall.cells.size());
            int count = wall.cells.size();
            for (int i = 0; i < count; i++) {
                float delay = (float) i / count * POP_TICKS;
                float pop = Mth.clamp((time - delay) / POP_TICKS * 2.0F, 0.0F, 1.0F);
                float scale = pop * fade;
                if (scale <= 0.01F) continue;

                int tint = VoxelRenderer.toWhite(color, 0.1F + 0.12F * Mth.sin(time * 0.2F + i * 0.9F));
                cells.add(new ShieldEffects.Voxel(Vec3.atCenterOf(wall.cells.get(i)), CELL_HALF * scale, tint));
            }
            ShieldEffects.submit(context, camera, cells, CELL_ALPHA);
        }
    }

    private static int liveColor(ClientLevel level, ClientWall wall) {
        if (level.getEntity(wall.casterId) instanceof Player caster && PowerRingItem.getWornCorps(caster).isPresent()) {
            return ARGB.opaque(CorpsColors.of(caster));
        }
        return wall.color;
    }

    public static void submitPreview(LevelRenderContext context, Vec3 camera, Iterable<BlockPos> cells, int color, float alpha) {
        int opaque = ARGB.opaque(color);
        List<ShieldEffects.Voxel> voxels = new ArrayList<>();
        for (BlockPos pos : cells) voxels.add(new ShieldEffects.Voxel(Vec3.atCenterOf(pos), PREVIEW_HALF, opaque));
        ShieldEffects.submit(context, camera, voxels, alpha);
    }

    private WallEffects() {}
}
