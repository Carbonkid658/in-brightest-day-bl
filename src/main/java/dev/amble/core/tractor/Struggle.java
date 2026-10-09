package dev.amble.core.tractor;

import dev.amble.config.BrightestDayConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class Struggle {
    private static final int BREAK_TICKS = 5;

    private @Nullable Vec3 last;
    private Vec3 pushed = Vec3.ZERO;
    private int strain;

    public boolean escaped(Entity target) {
        if (!(target instanceof ServerPlayer)) return false;
        Vec3 position = target.position();
        Vec3 previous = this.last;
        this.last = position;
        if (previous == null) return false;

        double deviation = position.subtract(previous).subtract(this.pushed).length();
        if (deviation > BrightestDayConfig.get().breakFreeSpeed) this.strain++;
        else this.strain = Math.max(0, this.strain - 1);
        return this.strain >= BREAK_TICKS;
    }

    public void push(Entity target, Vec3 velocity) {
        this.pushed = velocity;
        target.setDeltaMovement(velocity);
        target.needsSync = true;
        if (target instanceof ServerPlayer) target.syncVelocity = true;
        target.resetFallDistance();
    }
}
