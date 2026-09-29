package dev.amble.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.amble.client.render.renderstates.GreenLanternBlockEntityRenderState;
import dev.amble.core.blockentities.GreenLanternBlockEntity;
import dev.amble.core.blocks.GreenLanternBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class GreenLanternBlockEntityRenderer
        implements BlockEntityRenderer<GreenLanternBlockEntity, GreenLanternBlockEntityRenderState> {

    private final BlockModelResolver blockModelResolver;

    public GreenLanternBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockModelResolver = context.blockModelResolver();
    }

    @Override
    public GreenLanternBlockEntityRenderState createRenderState() {
        return new GreenLanternBlockEntityRenderState();
    }

    @Override
    public void extractRenderState(GreenLanternBlockEntity blockEntity, GreenLanternBlockEntityRenderState state,
                                   float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, breakProgress);

        state.yRot = RotationSegment.convertToDegrees(blockEntity.getBlockState().getValue(GreenLanternBlock.ROTATION));

        this.blockModelResolver.update(state.model, blockEntity.getBlockState(), BlockDisplayContext.create());
    }

    @Override
    public void submit(GreenLanternBlockEntityRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();

        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.rotateDegrees(Axis.YP, -state.yRot);
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        state.model.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
    }
}