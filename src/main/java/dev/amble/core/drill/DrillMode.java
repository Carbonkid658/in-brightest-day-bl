package dev.amble.core.drill;

import net.minecraft.util.Mth;

import java.util.Locale;

public enum DrillMode {
    HOLD,
    TUNNEL;

    private static final DrillMode[] VALUES = values();

    public String translationKey() {
        return "drill_mode.brightestday." + this.name().toLowerCase(Locale.ROOT);
    }

    public static DrillMode byId(int id) {
        return VALUES[Mth.clamp(id, 0, VALUES.length - 1)];
    }
}
