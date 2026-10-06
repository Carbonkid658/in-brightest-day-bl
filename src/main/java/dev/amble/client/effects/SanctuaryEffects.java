package dev.amble.client.effects;

import dev.amble.core.networking.payloads.s2c.SanctuaryS2CPayload;
import dev.amble.core.ringpowers.LanternCorps;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class SanctuaryEffects {
    private static final String[] SYMBOL = {
            ".....#.....",
            ".....#.....",
            "....###....",
            "..#######..",
            ".##.....##.",
            "##.......##",
            "#....#....#",
            "#...###...#",
            "#....#....#",
            "##.......##",
            ".##.....##.",
            "..#######..",
            "....#.#....",
            "...##.##...",
            ".###...###."
    };
    private static final float VOXEL = 0.2F;
    private static final double HEIGHT = 3.2;
    private static final double BOB = 0.15;
    private static final float SPIN = 0.02F;
    private static final float ALPHA = 0.9F;
    private static final double RENDER_DISTANCE = 128.0;
    private static final List<int[]> CELLS = new ArrayList<>();

    static {
        for (int row = 0; row < SYMBOL.length; row++) {
            for (int column = 0; column < SYMBOL[row].length(); column++) {
                if (SYMBOL[row].charAt(column) == '#') CELLS.add(new int[]{column, row});
            }
        }
    }

    private static @Nullable BlockPos center;

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(SanctuaryS2CPayload.TYPE, (payload, context) -> center = payload.center().orElse(null));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> center = null);
        LevelRenderEvents.COLLECT_SUBMITS.register(SanctuaryEffects::render);
    }

    private static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (center == null || client.level == null || client.level.dimension() != Level.OVERWORLD) return;

        Vec3 camera = context.levelState().cameraRenderState.pos;
        float time = client.level.getGameTime() + client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 origin = Vec3.atBottomCenterOf(center).add(0.0, HEIGHT + Math.sin(time * 0.05) * BOB, 0.0);
        if (origin.distanceTo(camera) > RENDER_DISTANCE) return;

        float angle = time * SPIN;
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        float width = SYMBOL[0].length();
        float height = SYMBOL.length;
        int color = ARGB.opaque(LanternCorps.BLUE.color());

        List<ShieldEffects.Voxel> voxels = new ArrayList<>(CELLS.size());
        for (int[] cell : CELLS) {
            float x = (cell[0] - (width - 1) / 2.0F) * VOXEL;
            float y = ((height - 1) / 2.0F - cell[1]) * VOXEL;
            float shimmer = 0.15F + 0.25F * (0.5F + 0.5F * Mth.sin(time * 0.15F + cell[0] * 0.7F + cell[1] * 0.4F));
            voxels.add(new ShieldEffects.Voxel(origin.add(x * cos, y, x * sin), VOXEL * 0.5F, VoxelRenderer.toWhite(color, shimmer)));
        }
        ShieldEffects.submit(context, camera, voxels, ALPHA);
    }

    private SanctuaryEffects() {}
}
