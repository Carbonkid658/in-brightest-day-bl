package dev.amble.client.effects;

import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class ArmedAimModifier extends AbstractModifier {
    private final Avatar avatar;
    private float headX;
    private float headY;

    public ArmedAimModifier(Avatar avatar) {
        this.avatar = avatar;
    }

    @Override
    public void get3DTransform(@NotNull PlayerAnimBone bone) {
        super.get3DTransform(bone);
        if (!(this.avatar instanceof Player player)) return;

        String name = bone.getName();
        if ("head".equals(name)) {
            this.headX = bone.rotation.x;
            this.headY = bone.rotation.y;
            return;
        }

        float amount = ArmedPose.amount(player);
        if (amount <= 0.001F) return;

        boolean right = player.getMainArm() == HumanoidArm.RIGHT;
        if (name.equals(right ? "right_arm" : "left_arm")) {
            bone.rotation.set(
                    Mth.lerp(amount, bone.rotation.x, this.headX - Mth.HALF_PI),
                    Mth.lerp(amount, bone.rotation.y, this.headY + (right ? -ArmedPose.AIM_INWARD : ArmedPose.AIM_INWARD)),
                    Mth.lerp(amount, bone.rotation.z, 0.0F));
        }
    }
}
