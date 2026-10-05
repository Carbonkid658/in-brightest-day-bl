package dev.amble.core.team;

import dev.amble.BrightestDay;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;

public final class RingDamage {
    public static final ResourceKey<DamageType> RING_CONSTRUCT = ResourceKey.create(Registries.DAMAGE_TYPE, BrightestDay.id("ring_construct"));

    public static DamageSource source(ServerLevel level, Player player) {
        return level.damageSources().source(RING_CONSTRUCT, player);
    }

    private RingDamage() {}
}
