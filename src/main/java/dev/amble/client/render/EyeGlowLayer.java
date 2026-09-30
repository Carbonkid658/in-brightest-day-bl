package dev.amble.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.amble.client.effects.ArmedPose;
import dev.amble.client.flight.FlightRenderTypes;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.ringpowers.EyePaint;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.ringpowers.RingBenefits;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;

public class EyeGlowLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final RenderStateDataKey<EyePaint> EYES = RenderStateDataKey.create(() -> "brightestday:eyes");
    public static final RenderStateDataKey<Boolean> GLOWING = RenderStateDataKey.create(() -> "brightestday:eyes_glowing");

    private static final float PIXEL = 1.0F / 16.0F;
    private static final float FACE_Z = -4.52F * PIXEL;
    private static final int ALPHA = 0xC0;

    public EyeGlowLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    public static void extract(Avatar entity, AvatarRenderState state) {
        FabricRenderState data = (FabricRenderState) state;
        if (entity instanceof Player player) {
            data.setData(EYES, BrightestDayAttachments.getEyes(player));
            data.setData(GLOWING, RingBenefits.isActive(player));
        } else {
            data.setData(GLOWING, false);
        }
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        EyePaint eyes = state.getDataOrDefault(EYES, EyePaint.EMPTY);
        if (eyes.isEmpty() || state.isInvisible || !state.getDataOrDefault(GLOWING, false)) return;

        int corps = ARGB.opaque(state.getDataOrDefault(ArmedPose.COLOR, LanternCorps.GREEN.color()));
        poseStack.pushPose();
        this.getParentModel().head.translateAndRotate(poseStack);
        submitNodeCollector.submitCustomGeometry(poseStack, FlightRenderTypes.GLOW, (pose, buffer) -> pixels(pose, buffer, eyes, corps));
        poseStack.popPose();
    }

    private static void pixels(PoseStack.Pose pose, VertexConsumer buffer, EyePaint eyes, int corps) {
        for (int row = 0; row < EyePaint.SIZE; row++) {
            for (int column = 0; column < EyePaint.SIZE; column++) {
                int kind = eyes.get(column, row);
                if (kind == EyePaint.NONE) continue;

                int color = ARGB.color(ALPHA, kind == EyePaint.WHITE ? 0xFFFFFF : corps);
                float x0 = (3 - column) * PIXEL;
                float x1 = (4 - column) * PIXEL;
                float y0 = (row - 8) * PIXEL;
                float y1 = (row - 7) * PIXEL;
                buffer.addVertex(pose, x1, y0, FACE_Z).setColor(color);
                buffer.addVertex(pose, x0, y0, FACE_Z).setColor(color);
                buffer.addVertex(pose, x0, y1, FACE_Z).setColor(color);
                buffer.addVertex(pose, x1, y1, FACE_Z).setColor(color);
            }
        }
    }
}
