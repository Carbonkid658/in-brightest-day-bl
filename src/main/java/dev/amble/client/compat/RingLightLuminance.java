package dev.amble.client.compat;

import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.impl.LightRingPower;
import dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class RingLightLuminance implements EntityLuminance {
    public static final RingLightLuminance INSTANCE = new RingLightLuminance();
    public static final Type TYPE = Type.registerSimple(BrightestDay.id("ring_light"), INSTANCE);

    private static final int LUMINANCE = 7;

    @Override
    public Type type() {
        return TYPE;
    }

    @Override
    public int getLuminance(ItemLightSourceManager itemLightSourceManager, Entity entity) {
        return entity instanceof Player player && LightRingPower.isEmitting(player) ? LUMINANCE : 0;
    }

    private RingLightLuminance() {}
}
