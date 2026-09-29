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
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class GlowAura {
    private static final float WHITEN = 0.15F;
    private static final float BOOST = 1.35F;

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

    public static void init() {}

    public static void extract(Avatar entity, AvatarRenderState state) {
        if (!(entity instanceof Player player)) return;
        if (!BrightestDayAttachments.getColorTweak(player).aura()) return;
        if (PowerRingItem.getWornCorps(player).isEmpty() || !PowerRingItem.hasCharge(player)) return;

        int color = ARGB.scaleRGB(VoxelRenderer.toWhite(CorpsColors.of(player), WHITEN), BOOST);
        state.outlineColor = ARGB.opaque(color);
    }

    private GlowAura() {}
}
