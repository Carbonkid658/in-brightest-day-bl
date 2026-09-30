package dev.amble.client.effects;

import dev.amble.core.ringpowers.CorpsColors;
import dev.amble.core.ringpowers.RingBenefits;
import dev.amble.core.shields.ShieldManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public final class ElementAura {
    private static final float FADE_SPEED = 0.15F;
    private static final float RADIUS_SCALE = 0.95F;
    private static final float WATER_ALPHA = 0.3F;
    private static final float LAVA_ALPHA = 0.45F;

    private static final Map<Player, float[]> AURAS = new WeakHashMap<>();

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(ElementAura::tick);
        LevelRenderEvents.COLLECT_SUBMITS.register(ElementAura::render);
    }

    private static void tick(Minecraft client) {
        if (client.level == null || client.isPaused()) return;

        for (AbstractClientPlayer player : client.level.players()) {
            boolean inLava = isSubmerged(player, FluidTags.LAVA);
            boolean submerged = (inLava || isSubmerged(player, FluidTags.WATER)) && RingBenefits.isActive(player);
            float[] aura = AURAS.get(player);
            if (aura == null) {
                if (!submerged) continue;
                aura = new float[3];
                AURAS.put(player, aura);
            }
            aura[1] = aura[0];
            aura[0] += ((submerged ? 1.0F : 0.0F) - aura[0]) * FADE_SPEED;
            if (submerged) aura[2] = inLava ? LAVA_ALPHA : WATER_ALPHA;
            if (!submerged && aura[0] < 0.005F) AURAS.remove(player);
        }
    }

    /** True once the fluid reaches the top of the player's hitbox, not merely their feet or eyes. */
    private static boolean isSubmerged(Player player, TagKey<Fluid> fluid) {
        return player.getFluidHeight(fluid) >= player.getBbHeight();
    }

    private static void render(LevelRenderContext context) {
        if (AURAS.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        for (Map.Entry<Player, float[]> entry : AURAS.entrySet()) {
            Player player = entry.getKey();
            float[] aura = entry.getValue();
            float amount = Mth.lerp(partialTicks, aura[1], aura[0]);
            if (amount < 0.005F) continue;

            Vec3 center = player.getPosition(partialTicks).add(0.0, player.getBbHeight() * 0.5, 0.0);
            float radius = ShieldManager.entityShieldRadius(player) * RADIUS_SCALE;
            float time = player.tickCount + partialTicks;
            int color = ARGB.opaque(CorpsColors.of(player));

            ShieldEffects.submit(context, camera, ShieldEffects.sphere(center, radius * (0.8F + 0.2F * amount), radius, time, color), aura[2] * amount);
        }
    }

    private ElementAura() {}
}
