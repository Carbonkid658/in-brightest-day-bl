package dev.amble.core.ringpowers.impl;

import com.mojang.serialization.MapCodec;
import dev.amble.BrightestDay;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.progression.IndigoOne;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.ringpowers.RingPower;
import dev.amble.core.ringpowers.RingPowerCategory;
import dev.amble.core.ringpowers.RingPowerRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class ConversionRingPower extends RingPower<Unit> {
    private static final double RANGE = 5.0;

    public ConversionRingPower() {
        super(BrightestDay.id("conversion"), EnumSet.of(LanternCorps.INDIGO), MapCodec.unitCodec(Unit.INSTANCE));
    }

    @Override
    public RingPowerCategory category() {
        return RingPowerCategory.UTILITY;
    }

    @Override
    public Unit createData() {
        return Unit.INSTANCE;
    }

    public static void fire(ServerPlayer player) {
        if (player.isSpectator() || !BrightestDayAttachments.has(player, RingPowerRegistry.CONVERSION)) return;

        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(RANGE));
        BlockHitResult blockHit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 reach = blockHit.getLocation();
        AABB area = player.getBoundingBox().expandTowards(reach.subtract(eye)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player, eye, reach, area,
                entity -> entity instanceof ServerPlayer target && !target.isSpectator() && !BrightestDayAttachments.getRing(target).isEmpty(), 0.3F);

        if (hit == null || !(hit.getEntity() instanceof ServerPlayer target) || !IndigoOne.begin(player, target)) {
            player.sendOverlayMessage(Component.translatable("message.brightestday.indigo.no_target").withColor(LanternCorps.INDIGO.color()));
            return;
        }
        ArmedRingPower.raise(player);
    }
}
