package dev.amble.client.effects.drill;

import dev.amble.client.effects.ShieldEffects;
import dev.amble.core.drill.DrillGeometry;
import dev.amble.core.networking.payloads.s2c.PlacedDrillS2CPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PlacedDrillEffects {
    private static final double SHAFT_LENGTH = 1.2;
    private static final double GLIDE = 0.35;

    private static final Map<Integer, PlacedDrill> DRILLS = new HashMap<>();
    private static @Nullable ClientLevel drillLevel;

    private static final class PlacedDrill {
        final DrillEffects.ClientDrill drill;
        final Direction direction;
        BlockPos head;
        Vec3 pos;
        Vec3 prevPos;
        @Nullable PlacedDrillSound sound;

        PlacedDrill(PlacedDrillS2CPayload payload) {
            this.drill = new DrillEffects.ClientDrill(ARGB.opaque(payload.color()), payload.size());
            this.direction = payload.direction();
            this.head = payload.head();
            this.pos = this.target();
            this.prevPos = this.pos;
        }

        Vec3 target() {
            return Vec3.atCenterOf(this.head).subtract(Vec3.atLowerCornerOf(this.direction.getUnitVec3i()).scale(0.5));
        }
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(PlacedDrillS2CPayload.TYPE, (payload, context) -> {
            if (!payload.present()) {
                DRILLS.remove(payload.id());
                return;
            }
            PlacedDrill drill = DRILLS.get(payload.id());
            if (drill == null) DRILLS.put(payload.id(), new PlacedDrill(payload));
            else drill.head = payload.head();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> DRILLS.clear());
        ClientTickEvents.END_CLIENT_TICK.register(PlacedDrillEffects::tick);
        LevelRenderEvents.COLLECT_SUBMITS.register(PlacedDrillEffects::render);
    }

    private static boolean isActive(PlacedDrill drill) {
        return DRILLS.containsValue(drill);
    }

    private static void tick(Minecraft client) {
        if (client.level != drillLevel) {
            DRILLS.clear();
            drillLevel = client.level;
        }
        if (client.level == null) return;

        for (PlacedDrill drill : DRILLS.values()) {
            drill.prevPos = drill.pos;
            drill.pos = drill.pos.add(drill.target().subtract(drill.pos).scale(GLIDE));
            if (drill.sound == null || drill.sound.isStopped()) {
                drill.sound = new PlacedDrillSound(drill);
                client.getSoundManager().play(drill.sound);
            }
        }
    }

    private static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || DRILLS.isEmpty()) return;
        float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 camera = context.levelState().cameraRenderState.pos;
        float time = client.level.getGameTime() + partialTicks;

        List<ShieldEffects.Voxel> voxels = new ArrayList<>();
        for (PlacedDrill drill : DRILLS.values()) {
            Vec3 forward = Vec3.atLowerCornerOf(drill.direction.getUnitVec3i());
            Vec3 face = drill.prevPos.lerp(drill.pos, partialTicks);
            Vec3 tip = face.add(forward.scale(DrillEffects.TIP_BITE));
            Vec3 start = face.subtract(forward.scale(SHAFT_LENGTH + drill.drill.size()));
            DrillEffects.drill(start, tip, time, drill.drill, voxels);

            if (DrillGeometry.drillable(client.level, drill.head, client.level.getBlockState(drill.head))) {
                DrillEffects.chips(client.level, new BlockHitResult(face, drill.direction.getOpposite(), drill.head, false), forward, time, drill.drill, voxels);
            }
        }
        ShieldEffects.submit(context, camera, voxels, 1.0F);
    }

    private static final class PlacedDrillSound extends AbstractTickableSoundInstance {
        private final PlacedDrill drill;

        PlacedDrillSound(PlacedDrill drill) {
            super(SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.drill = drill;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.8F;
            this.pitch = 2.0F;
            this.x = drill.pos.x;
            this.y = drill.pos.y;
            this.z = drill.pos.z;
        }

        @Override
        public void tick() {
            if (!isActive(this.drill)) {
                this.stop();
                return;
            }
            this.x = this.drill.pos.x;
            this.y = this.drill.pos.y;
            this.z = this.drill.pos.z;
        }
    }

    private PlacedDrillEffects() {}
}
