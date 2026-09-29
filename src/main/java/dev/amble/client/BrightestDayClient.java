package dev.amble.client;

import dev.amble.client.effects.ArmedPose;
import dev.amble.client.effects.BlastEffects;
import dev.amble.client.effects.ConstructClient;
import dev.amble.client.effects.ElementAura;
import dev.amble.client.effects.ScanEffects;
import dev.amble.client.effects.ShieldEffects;
import dev.amble.client.effects.TractorEffects;
import dev.amble.client.forge.ForgeClient;
import dev.amble.client.flight.FlightAnimations;
import dev.amble.client.flight.FlightAnimator;
import dev.amble.client.flight.FlightTrail;
import dev.amble.client.hud.RingChargeHud;
import dev.amble.client.render.LanternBlockEntityRenderer;
import dev.amble.client.render.SlottedRingLayer;
import dev.amble.client.screens.LanternButtons;
import dev.amble.client.screens.LanternScreen;
import dev.amble.core.BrightestDayBlockEntityTypes;
import dev.amble.core.BrightestDayMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;

public class BrightestDayClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BrightestDayKeybinds.init();
        FlightAnimator.init();
        FlightAnimations.init();
        FlightTrail.init();
        BlastEffects.init();
        ShieldEffects.init();
        ArmedPose.init();
        ElementAura.init();
        TractorEffects.init();
        ScanEffects.init();
        ForgeClient.init();
        ConstructClient.init();
        LanternButtons.init();
        RingChargeHud.init();
        MenuScreens.register(BrightestDayMenus.LANTERN, LanternScreen::new);
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, entityRenderer, helper, context) -> {
            if (entityRenderer instanceof AvatarRenderer<?> avatarRenderer) {
                helper.register(new SlottedRingLayer(avatarRenderer));
            }
        });
        registerBlockEntityRenderers();
    }

    private void registerBlockEntityRenderers() {
        BlockEntityRendererRegistry.register(BrightestDayBlockEntityTypes.LANTERN_BLOCK_ENTITY_TYPE, LanternBlockEntityRenderer::new);
    }
}
