package dev.amble.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.amble.BrightestDay;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.networking.payloads.c2s.AcidC2SPayload;
import dev.amble.core.networking.payloads.c2s.ConcussiveC2SPayload;
import dev.amble.core.networking.payloads.c2s.DismissConstructC2SPayload;
import dev.amble.core.networking.payloads.c2s.ToggleLightC2SPayload;
import dev.amble.core.networking.payloads.c2s.UsePowerC2SPayload;
import dev.amble.core.ringpowers.RingPowerRegistry;
import dev.amble.core.ringpowers.impl.LightRingPower;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

public final class BrightestDayKeybinds {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(BrightestDay.id("main"));

    public static final KeyMapping POWER_1 = register("power_1", InputConstants.KEY_Z);
    public static final KeyMapping POWER_2 = register("power_2", InputConstants.KEY_X);
    public static final KeyMapping POWER_3 = register("power_3", InputConstants.KEY_C);
    public static final KeyMapping POWER_4 = register("power_4", InputConstants.UNKNOWN.getValue());

    public static final KeyMapping CYCLE_CONSTRUCT = register("cycle_construct", InputConstants.KEY_R);
    public static final KeyMapping SCAN = register("scan", InputConstants.KEY_V);
    public static final KeyMapping DISMISS_CONSTRUCT = register("dismiss_construct", InputConstants.KEY_G);
    public static final KeyMapping CONCUSSIVE_BLAST = register("concussive_blast", InputConstants.KEY_B);
    public static final KeyMapping ACID_VOMIT = register("acid_vomit", InputConstants.KEY_N);

    private static boolean spewing;

    private static final KeyMapping[] POWER_KEYS = {POWER_1, POWER_2, POWER_3, POWER_4};

    public static final @Nullable KeyMapping TOGGLE_LIGHT = FabricLoader.getInstance().isModLoaded(LightRingPower.LAMB_DYNAMIC_LIGHTS)
            ? register("toggle_light", InputConstants.KEY_LALT)
            : null;

    private static KeyMapping register(String name, int key) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.brightestday." + name,
                InputConstants.Type.KEYBOARD,
                key,
                CATEGORY
        ));
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(BrightestDayKeybinds::tick);
    }

    private static void tick(Minecraft client) {
        if (client.player == null) return;

        for (int i = 0; i < POWER_KEYS.length; i++) {
            while (POWER_KEYS[i].consumeClick()) {
                ClientPlayNetworking.send(new UsePowerC2SPayload(i));
            }
        }

        while (DISMISS_CONSTRUCT.consumeClick()) {
            ClientPlayNetworking.send(DismissConstructC2SPayload.INSTANCE);
        }

        while (CONCUSSIVE_BLAST.consumeClick()) {
            if (BrightestDayAttachments.get(client.player, RingPowerRegistry.CONCUSSIVE).isPresent()) ClientPlayNetworking.send(ConcussiveC2SPayload.INSTANCE);
        }

        boolean spew = ACID_VOMIT.isDown() && client.gui.screen() == null && BrightestDayAttachments.get(client.player, RingPowerRegistry.ACID).isPresent();
        if (spew != spewing) {
            spewing = spew;
            ClientPlayNetworking.send(new AcidC2SPayload(spew));
        }

        if (TOGGLE_LIGHT != null) {
            while (TOGGLE_LIGHT.consumeClick()) {
                ClientPlayNetworking.send(ToggleLightC2SPayload.INSTANCE);
            }
        }
    }

    private BrightestDayKeybinds() {}
}