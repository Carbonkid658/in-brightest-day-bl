package dev.amble.core.ringpowers.constructs;

import com.mojang.serialization.MapCodec;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.ringpowers.RingPower;
import dev.amble.core.ringpowers.RingPowerCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

public abstract class ConstructRingPower extends RingPower<Unit> {

    protected ConstructRingPower(Identifier id) {
        super(id, EnumSet.allOf(LanternCorps.class), MapCodec.unitCodec(Unit.INSTANCE));
    }

    @Override
    public RingPowerCategory category() {
        return RingPowerCategory.CONSTRUCT;
    }

    @Override
    public Unit createData() {
        return Unit.INSTANCE;
    }

    public int cost(int radius) {
        return this.useCost();
    }

    public boolean usesSize() {
        return false;
    }

    public int minSize() {
        return 1;
    }

    public int maxSize() {
        return 1;
    }

    public int defaultSize() {
        return this.minSize();
    }

    public int clampSize(int size) {
        return Mth.clamp(size, this.minSize(), this.maxSize());
    }

    public Component describeSize(int size) {
        return Component.literal(String.valueOf(this.clampSize(size)));
    }

    public boolean usesGesture() {
        return false;
    }

    public abstract void fire(ServerPlayer player, int radius, int color);

    public record Aim(Vec3 eye, Vec3 look, Vec3 end, @Nullable Entity entity) {}

    protected static Aim aim(ServerPlayer player, double range) {
        ServerLevel level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));

        HitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getLocation();

        AABB searchArea = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, eye, end, searchArea,
                entity -> entity != player && !entity.isSpectator() && entity.isPickable(), 0.3F);
        if (entityHit != null) return new Aim(eye, look, entityHit.getLocation(), entityHit.getEntity());
        return new Aim(eye, look, end, null);
    }
}
