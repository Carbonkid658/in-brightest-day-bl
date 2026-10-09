package dev.amble.client.effects;

import dev.amble.client.forge.ForgeClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class RingInput {
    private static boolean interacted;

    public static void init() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!client.options.keyUse.isDown()) interacted = false;
        });
    }

    public static void markInteracted() {
        interacted = true;
    }

    public static boolean blocked() {
        return interacted;
    }

    public static boolean wantsUse(LocalPlayer player) {
        return BlastEffects.wantsToCharge(player) || ForgeClient.wantsToDraw(player) || AbilityClient.wantsUse(player);
    }

    public static boolean useHeld(Minecraft client) {
        return client.gui.screen() == null && client.options.keyUse.isDown() && !interacted;
    }

    private RingInput() {}
}
