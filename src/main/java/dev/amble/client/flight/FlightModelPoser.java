package dev.amble.client.flight;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public final class FlightModelPoser {

    public static void apply(PlayerModel model, AvatarRenderState state, FlightPose pose) {
        float w = pose.flight();
        float s = pose.tilt();
        float t = state.ageInTicks;

        model.head.xRot = Mth.clamp(model.head.xRot + pose.bodyAngle() * Mth.DEG_TO_RAD, -2.2F, 1.0F);

        boolean rightLeads = pose.leadArm() == HumanoidArm.RIGHT;
        float idle = Mth.sin(t * 0.09F) * 0.06F;
        float flutter = Mth.sin(t * 1.1F) * 0.06F * s;

        ModelPart lead = rightLeads ? model.rightArm : model.leftArm;
        ModelPart trail = rightLeads ? model.leftArm : model.rightArm;
        float leadOut = rightLeads ? 1.0F : -1.0F;

        pose(lead, w,
                Mth.lerp(s, -0.1F + idle, -Mth.PI + 0.12F),
                0.0F,
                Mth.lerp(s, 0.25F, -0.12F) * leadOut);
        pose(trail, w,
                Mth.lerp(s, -0.1F - idle, 0.2F),
                0.0F,
                Mth.lerp(s, 0.25F, 0.08F) * -leadOut);

        pose(model.rightLeg, w,
                Mth.lerp(s, 0.15F + idle, 0.05F + flutter),
                0.0F,
                Mth.lerp(s, 0.05F, 0.02F));
        pose(model.leftLeg, w,
                Mth.lerp(s, -0.05F - idle, 0.05F - flutter),
                0.0F,
                Mth.lerp(s, -0.05F, -0.02F));
    }

    private static void pose(ModelPart part, float weight, float xRot, float yRot, float zRot) {
        part.xRot = Mth.lerp(weight, part.xRot, xRot);
        part.yRot = Mth.lerp(weight, part.yRot, yRot);
        part.zRot = Mth.lerp(weight, part.zRot, zRot);
    }

    private FlightModelPoser() {}
}
