package dev.amble.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.amble.BrightestDay;
import dev.amble.client.render.models.HologramPadModel;
import dev.amble.core.mannequin.Hologram;
import dev.amble.core.mannequin.Mannequins;
import dev.amble.core.ringpowers.LanternCorps;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Holograms implements ResourceManagerReloadListener {
    public static final int IDLE_COLOR = 0x4FE3FF;
    public static final RenderStateDataKey<Integer> TINT = RenderStateDataKey.create(() -> "brightestday:hologram_tint");

    private static final Identifier PAD = BrightestDay.id("textures/block/hologram_pad.png");
    private static final Identifier PAD_EMISSION = BrightestDay.id("textures/block/hologram_pad_emission.png");
    private static final Identifier PAD_EMISSION_TINTABLE = BrightestDay.id("dynamic/hologram_pad_emission");
    private static final double RENDER_DISTANCE = 96.0;
    private static final int ALPHA = 150;
    private static final float WHITEN = 0.3F;

    private static @Nullable HologramPadModel model;
    private static volatile boolean stale = true;
    private static boolean tintable;

    public static void init() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(BrightestDay.id("hologram_textures"), new Holograms());
        LevelRenderEvents.COLLECT_SUBMITS.register(Holograms::renderPads);
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        stale = true;
    }

    public static List<Mannequin> all(ClientLevel level) {
        List<Mannequin> holograms = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof Mannequin mannequin && Mannequins.isHologram(mannequin)) holograms.add(mannequin);
        }
        return holograms;
    }

    public static boolean is(Entity entity) {
        return Mannequins.isHologram(entity);
    }

    public static Hologram of(Entity entity) {
        return Mannequins.hologram(entity);
    }

    public static Optional<LanternCorps> corps(Entity entity) {
        return is(entity) ? of(entity).corps() : Optional.empty();
    }

    public static boolean lit(Entity entity) {
        if (!is(entity)) return false;
        return of(entity).corps().isPresent();
    }

    public static int color(Entity entity) {
        return corps(entity).map(LanternCorps::color).orElse(IDLE_COLOR);
    }

    public static void extract(Entity entity, AvatarRenderState state) {
        FabricRenderState data = (FabricRenderState) state;
        if (!is(entity) || of(entity).settings().hardLight()) {
            data.setData(TINT, null);
            return;
        }
        data.setData(TINT, ARGB.color(ALPHA, ARGB.srgbLerp(WHITEN, ARGB.opaque(color(entity)), 0xFFFFFFFF)));
    }

    public static @Nullable Integer tint(Object state) {
        return state instanceof FabricRenderState fabric ? fabric.getData(TINT) : null;
    }

    public static RenderType type(Identifier texture) {
        return RenderTypes.entityTranslucentEmissive(texture);
    }

    private static boolean ready() {
        if (stale) {
            tintable = BatteryTextures.load(Minecraft.getInstance().getResourceManager(), PAD_EMISSION, PAD_EMISSION_TINTABLE);
            stale = false;
        }
        return true;
    }

    private static void renderPads(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || !ready()) return;
        if (model == null) model = new HologramPadModel();

        Vec3 camera = context.levelState().cameraRenderState.pos;
        float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float time = client.level.getGameTime() + partialTicks;
        PoseStack poseStack = context.poseStack();
        for (Mannequin mannequin : all(client.level)) {
            Vec3 feet = mannequin.getPosition(partialTicks);
            if (feet.distanceTo(camera) > RENDER_DISTANCE || mannequin.isInvisible() || !of(mannequin).settings().pad()) continue;
            BlockPos sample = BlockPos.containing(feet);
            int light = LightCoordsUtil.pack(client.level.getBrightness(LightLayer.BLOCK, sample), client.level.getBrightness(LightLayer.SKY, sample));
            float pulse = 0.85F + 0.15F * Mth.sin(time * 0.1F + mannequin.getId());
            boolean tinted = corps(mannequin).isPresent() && tintable;
            Identifier emission = tinted ? PAD_EMISSION_TINTABLE : PAD_EMISSION;
            int glow = tinted ? ARGB.scaleRGB(ARGB.opaque(color(mannequin)), pulse) : ARGB.white(pulse);

            poseStack.pushPose();
            poseStack.translate(feet.x - camera.x, feet.y - Mannequins.PAD_HEIGHT - camera.y, feet.z - camera.z);
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(0.0F, -HologramPadModel.PIVOT_Y / 16.0F, 0.0F);
            context.submitNodeCollector().submitModelPart(model.body(), poseStack, RenderTypes.entityTranslucent(PAD), light, OverlayTexture.NO_OVERLAY, null, -1);
            context.submitNodeCollector().submitModelPart(model.body(), poseStack, RenderTypes.eyes(emission), LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null, glow);
            poseStack.popPose();
        }
    }

    private Holograms() {}
}
