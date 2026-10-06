package dev.amble.client.render;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.amble.BrightestDay;
import dev.amble.client.effects.VoxelRenderer;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.CorpsColors;
import dev.amble.core.ringpowers.ColorTweak;
import dev.amble.core.ringpowers.impl.FlightRingPower;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public final class GlowAura {
    private static final float WHITEN = 0.15F;
    private static final float BOOST = 1.35F;
    private static final float FADE_TICKS = 10.0F;
    private static final float MIN_VISIBLE = 0.02F;
    private static final Map<Player, float[]> FADES = new WeakHashMap<>();

    public static final RenderPipeline ADDITIVE_OUTLINE_BLIT = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                    .withLocation(BrightestDay.id("pipeline/additive_entity_outline_blit"))
                    .withVertexShader("core/screenquad")
                    .withFragmentShader("core/blit_screen")
                    .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
                    .withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.LIGHTNING), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_COLOR))
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .build()
    );

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(GlowAura::tick);
    }

    private static boolean wanted(Player player) {
        ColorTweak tweak = BrightestDayAttachments.getColorTweak(player);
        if (!tweak.aura() || PowerRingItem.getWornCorps(player).isEmpty() || !PowerRingItem.hasCharge(player)) return false;
        return !tweak.auraFlightOnly() || FlightRingPower.isFlying(player);
    }

    private static void tick(Minecraft client) {
        if (client.level == null || client.isPaused()) return;
        for (Player player : client.level.players()) {
            float[] fade = FADES.get(player);
            boolean wanted = wanted(player);
            if (fade == null) {
                if (!wanted) continue;
                fade = new float[2];
                FADES.put(player, fade);
            }
            fade[0] = fade[1];
            fade[1] = Mth.approach(fade[1], wanted ? 1.0F : 0.0F, 1.0F / FADE_TICKS);
            if (!wanted && fade[0] <= 0.0F) FADES.remove(player);
        }
    }

    public static void extract(Avatar entity, AvatarRenderState state) {
        if (!(entity instanceof Player player)) return;
        float[] fade = FADES.get(player);
        if (fade == null) return;
        float amount = Mth.lerp(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false), fade[0], fade[1]);
        if (amount < MIN_VISIBLE) return;

        int color = ARGB.scaleRGB(VoxelRenderer.toWhite(CorpsColors.of(player), WHITEN), BOOST * amount);
        state.outlineColor = ARGB.opaque(color);
    }

    private GlowAura() {}
}
