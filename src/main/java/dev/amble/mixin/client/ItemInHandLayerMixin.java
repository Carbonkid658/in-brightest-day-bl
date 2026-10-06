package dev.amble.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zigythebird.playeranim.accessors.IAvatarAnimationState;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import dev.amble.client.effects.ArmedPose;
import dev.amble.client.effects.LanternChargeAnimations;
import dev.amble.core.items.LanternBlockItem;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemInHandLayer.class, priority = 1500)
public abstract class ItemInHandLayerMixin {
    @Unique
    private static final float EPSILON = 1.0E-4F;
    @Unique
    private static final float PIXEL = 1.0F / 16.0F;
    @Unique
    private static final float PIVOT_X = 1.0F;
    @Unique
    private static final float PIVOT_Y = 10.0F;
    @Unique
    private static final float PIVOT_Z = -2.0F;
    @Unique
    private static final float HANG_X = 0.093F;
    @Unique
    private static final float HANG_Y = 10.836F;
    @Unique
    private static final float HANG_Z = -0.063F;
    @Unique
    private static final float HELD_SCALE = 0.8F;

    @Unique
    private final PlayerAnimBone brightestday$rightItem = new PlayerAnimBone("right_item");
    @Unique
    private final PlayerAnimBone brightestday$leftItem = new PlayerAnimBone("left_item");
    @Unique
    private final Matrix4f brightestday$armPose = new Matrix4f();
    @Unique
    private final Matrix3f brightestday$armNormal = new Matrix3f();

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ArmedModel;translateToHand(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V", shift = At.Shift.AFTER))
    private void brightestday$captureArm(ArmedEntityRenderState renderState, ItemStackRenderState itemStackRenderState, ItemStack itemStack, HumanoidArm arm,
                                        PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        this.brightestday$armPose.set(poseStack.last().pose());
        this.brightestday$armNormal.set(poseStack.last().normal());
    }

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void brightestday$adjustAnimatedItem(ArmedEntityRenderState renderState, ItemStackRenderState itemStackRenderState, ItemStack itemStack, HumanoidArm arm,
                                                PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        if (!(renderState instanceof IAvatarAnimationState state)) return;
        AvatarAnimManager manager = state.playerAnimLib$getAnimManager();
        if (manager == null || !manager.isActive()) return;

        PlayerAnimBone bone = arm == HumanoidArm.LEFT ? this.brightestday$leftItem : this.brightestday$rightItem;
        bone.setToInitialPose();
        manager.get3DTransform(bone);

        boolean charging = ((FabricRenderState) renderState).getDataOrDefault(ArmedPose.CHARGE_ANIMATING, false);
        if (charging && itemStack.getItem() instanceof LanternBlockItem) {
            this.brightestday$hang(poseStack, bone, arm);
            LanternChargeAnimations.setBareTransform(true);
            return;
        }
        this.brightestday$fixRotationOrder(poseStack, bone);
    }

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V", shift = At.Shift.AFTER))
    private void brightestday$restoreTransform(ArmedEntityRenderState renderState, ItemStackRenderState itemStackRenderState, ItemStack itemStack, HumanoidArm arm,
                                              PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        LanternChargeAnimations.setBareTransform(false);
    }

    @Unique
    private void brightestday$hang(PoseStack poseStack, PlayerAnimBone bone, HumanoidArm arm) {
        float side = arm == HumanoidArm.LEFT ? 1.0F : -1.0F;
        poseStack.last().pose().set(this.brightestday$armPose);
        poseStack.last().normal().set(this.brightestday$armNormal);

        poseStack.translate((side * PIVOT_X + bone.position.x) * PIXEL, (PIVOT_Y - bone.position.y) * PIXEL, (PIVOT_Z + bone.position.z) * PIXEL);
        poseStack.rotate(new Quaternionf().rotationZYX(bone.rotation.z, bone.rotation.y, bone.rotation.x));
        poseStack.scale(bone.scale.x, bone.scale.y, bone.scale.z);

        poseStack.translate(side * HANG_X * PIXEL, HANG_Y * PIXEL, HANG_Z * PIXEL);
        poseStack.rotate(new Quaternionf().rotationZ((float) Math.PI));
        poseStack.scale(HELD_SCALE, HELD_SCALE, HELD_SCALE);
        poseStack.translate(-0.5F, 0.0F, -0.5F);
    }

    @Unique
    private void brightestday$fixRotationOrder(PoseStack poseStack, PlayerAnimBone bone) {
        float x = bone.rotation.x;
        float y = bone.rotation.y;
        float z = bone.rotation.z;
        if (Math.abs(y) < EPSILON || Math.abs(z) < EPSILON) return;
        if (Math.abs(bone.scale.x) < EPSILON || Math.abs(bone.scale.y) < EPSILON || Math.abs(bone.scale.z) < EPSILON) return;

        Quaternionf applied = new Quaternionf().rotateZ(-y).rotateY(-z).rotateX(-x);
        Quaternionf intended = new Quaternionf().rotateY(-z).rotateZ(-y).rotateX(-x);
        poseStack.scale(1.0F / bone.scale.x, 1.0F / bone.scale.y, 1.0F / bone.scale.z);
        poseStack.rotate(applied.conjugate().mul(intended));
        poseStack.scale(bone.scale.x, bone.scale.y, bone.scale.z);
    }
}
