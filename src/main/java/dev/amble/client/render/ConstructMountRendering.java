package dev.amble.client.render;

import dev.amble.core.mounts.ConstructMounts;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public final class ConstructMountRendering {
    public static final RenderStateDataKey<Integer> MOUNT = RenderStateDataKey.create(() -> "brightestday:construct_mount");
    public static final Identifier HORSE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/horse/horse_white.png");

    private static final int ALPHA = 175;

    public static void mark(Entity entity, EntityRenderState state) {
        ((FabricRenderState) state).setData(MOUNT, entity.getAttached(ConstructMounts.COLOR));
    }

    public static @Nullable Integer color(Object state) {
        return state instanceof FabricRenderState fabric ? fabric.getData(MOUNT) : null;
    }

    public static int tint(int color) {
        return ARGB.color(ALPHA, ARGB.srgbLerp(0.25F, color, 0xFFFFFFFF));
    }

    public static RenderType type(Identifier texture) {
        return RenderTypes.entityTranslucentEmissive(texture);
    }

    private ConstructMountRendering() {}
}
