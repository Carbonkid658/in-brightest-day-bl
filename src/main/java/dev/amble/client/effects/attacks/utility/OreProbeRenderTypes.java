package dev.amble.client.effects.attacks.utility;

import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.amble.BrightestDay;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.List;

public final class OreProbeRenderTypes {

    private static final RenderPipeline XRAY_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(BrightestDay.id("pipeline/ore_probe_xray"))
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                    .withCull(false)
                    .build()
    );

    public static final RenderType XRAY = RenderType.create(
            "brightestday_ore_probe_xray",
            RenderSetup.builder(XRAY_PIPELINE).sortOnUpload().createRenderSetup()
    );

    public static List<RenderPipeline> pipelines() {
        return List.of(XRAY_PIPELINE);
    }

    public static void init() {}

    private OreProbeRenderTypes() {}
}
