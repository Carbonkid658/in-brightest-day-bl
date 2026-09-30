package dev.amble.core.acid;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class AcidStream {
    public static final double SPEED = 0.6;
    public static final double RISE = 0.08;
    public static final double GRAVITY = 0.05;

    public static Vec3 mouth(Entity entity, float partialTicks) {
        Vec3 look = entity.getViewVector(partialTicks);
        return entity.getEyePosition(partialTicks).add(0.0, -0.2, 0.0).add(look.scale(0.3));
    }

    public static Vec3 velocity(Vec3 look) {
        return look.scale(SPEED).add(0.0, RISE, 0.0);
    }

    private AcidStream() {}
}
