package dev.amble.core.ringpowers;

import java.util.EnumSet;
import java.util.Set;

public final class CorpsArsenal {
    public static final Set<LanternCorps> SHARED = EnumSet.of(
            LanternCorps.GREEN, LanternCorps.YELLOW, LanternCorps.ORANGE, LanternCorps.INDIGO, LanternCorps.STAR_SAPPHIRE, LanternCorps.WHITE);

    public static Set<LanternCorps> shared(LanternCorps... extra) {
        EnumSet<LanternCorps> corps = EnumSet.copyOf(SHARED);
        corps.addAll(Set.of(extra));
        return corps;
    }

    public static Set<LanternCorps> exclusive(LanternCorps... corps) {
        EnumSet<LanternCorps> set = EnumSet.of(LanternCorps.WHITE);
        set.addAll(Set.of(corps));
        return set;
    }

    private CorpsArsenal() {}
}
