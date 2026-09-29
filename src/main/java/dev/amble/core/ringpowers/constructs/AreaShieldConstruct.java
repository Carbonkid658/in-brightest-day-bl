package dev.amble.core.ringpowers.constructs;

import dev.amble.BrightestDay;
import dev.amble.core.shields.ShieldManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class AreaShieldConstruct extends ConstructRingPower {
    public static final int MIN_RADIUS = 2;
    public static final int MAX_RADIUS = 10;
    public static final int DEFAULT_RADIUS = 4;
    public static final double RANGE = 24.0;
    private static final int BASE_COST = 100;
    private static final int COST_PER_BLOCK = 30;

    public AreaShieldConstruct() {
        super(BrightestDay.id("area_shield"));
    }

    @Override
    public int cost(int radius) {
        return BASE_COST + COST_PER_BLOCK * clampRadius(radius);
    }

    @Override
    public boolean usesRadius() {
        return true;
    }

    public static int clampRadius(int radius) {
        return Mth.clamp(radius, MIN_RADIUS, MAX_RADIUS);
    }

    @Override
    public void fire(ServerPlayer player, int radius, int color) {
        Vec3 center = aim(player, RANGE).end();
        ShieldManager.shieldArea(player.level(), center, clampRadius(radius), color, player);
        player.level().playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5F, 0.8F);
        player.level().playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.4F);
    }
}
