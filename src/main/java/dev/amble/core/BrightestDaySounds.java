package dev.amble.core;

import dev.amble.BrightestDay;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class BrightestDaySounds {
    public static final SoundEvent RING_CHARGE_5_PERCENT = register("ring.charge_5_percent");

    private static SoundEvent register(String name) {
        Identifier id = BrightestDay.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void init() {}
}
