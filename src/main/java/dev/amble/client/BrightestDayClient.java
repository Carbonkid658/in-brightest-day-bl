package dev.amble.client;

import dev.amble.client.render.GreenLanternBlockEntityRenderer;
import dev.amble.core.BrightestDayBlockEntityTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

public class BrightestDayClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        registerBlockEntityRenderers();
    }

    private void registerBlockEntityRenderers() {
        BlockEntityRendererRegistry.register(BrightestDayBlockEntityTypes.GREEN_LANTERN_BLOCK_ENTITY_TYPE, GreenLanternBlockEntityRenderer::new);
    }
}
