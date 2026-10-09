package dev.amble.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import dev.amble.core.ringpowers.constructs.ConstructTools;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConstructToolRendering {
    public static final RenderStateDataKey<Boolean> CONSTRUCT = RenderStateDataKey.create(() -> "brightestday:construct_tool");

    private static final int ALPHA = 170;

    public static void mark(ItemStackRenderState state, ItemStack stack) {
        ((FabricRenderState) state).setData(CONSTRUCT, ConstructTools.isConstruct(stack) ? Boolean.TRUE : null);
    }

    public static boolean isConstruct(ItemStackRenderState state) {
        return Boolean.TRUE.equals(((FabricRenderState) state).getData(CONSTRUCT));
    }

    public static void submit(PoseStack poseStack, SubmitNodeCollector collector, int overlayCoords, ItemQuads quads,
                              @Nullable IntList tintLayers, boolean foil) {
        Map<RenderType, List<BakedQuad>> byRenderType = new LinkedHashMap<>();
        for (BakedQuad quad : quads.all()) {
            byRenderType.computeIfAbsent(translucentType(quad, foil), type -> new ArrayList<>()).add(quad);
        }
        byRenderType.forEach((renderType, group) -> submitGroup(poseStack, collector, overlayCoords, renderType, group, tintLayers));
    }

    private static RenderType translucentType(BakedQuad quad, boolean foil) {
        boolean blockAtlas = quad.materialInfo().sprite().atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS);
        if (blockAtlas) return foil ? Sheets.translucentBlockItemGlintSheet() : Sheets.translucentBlockItemSheet();
        return foil ? Sheets.translucentItemGlintSheet() : Sheets.translucentItemSheet();
    }

    private static void submitGroup(PoseStack poseStack, SubmitNodeCollector collector, int overlayCoords, RenderType renderType,
                                    List<BakedQuad> group, @Nullable IntList tintLayers) {
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            QuadInstance instance = new QuadInstance();
            instance.setLightCoords(LightCoordsUtil.FULL_BRIGHT);
            instance.setOverlayCoords(overlayCoords);
            for (BakedQuad quad : group) {
                BakedQuad.MaterialInfo material = quad.materialInfo();
                int tint = -1;
                if (material.isTinted() && tintLayers != null && material.tintIndex() < tintLayers.size()) {
                    tint = tintLayers.getInt(material.tintIndex());
                }
                instance.setColor(ARGB.color(ALPHA, tint));
                buffer.putBakedQuad(pose, quad, instance);
            }
        });
    }

    private ConstructToolRendering() {}
}
