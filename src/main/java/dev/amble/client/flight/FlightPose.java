package dev.amble.client.flight;

import net.minecraft.world.entity.HumanoidArm;

public record FlightPose(float flight, float tilt, float bodyAngle, float roll, HumanoidArm leadArm) {
}
