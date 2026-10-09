package dev.amble.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.amble.client.effects.ArmedPose;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public class SlottedRingLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public SlottedRingLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        SlottedRingRenderer.submit(this.getParentModel(), state, state.mainArm, poseStack, submitNodeCollector, lightCoords, state.outlineColor);
        ArmedPose.submitRingGlow(this.getParentModel(), state, state.mainArm, poseStack, submitNodeCollector);
    }
}
