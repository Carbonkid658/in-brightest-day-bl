package dev.amble.core.progression;

import dev.amble.core.ringpowers.LanternCorps;

import java.util.Locale;

public enum RankTask {
    GREEN_LOW_WIN(LanternCorps.GREEN, 2, 1),
    GREEN_OUTCLASS(LanternCorps.GREEN, 3, 1),
    GREEN_BOSS(LanternCorps.GREEN, 4, 1),
    YELLOW_FEAR(LanternCorps.YELLOW, 2, 25),
    YELLOW_FEARED_KILLS(LanternCorps.YELLOW, 3, 5),
    YELLOW_FEARED_DEATHS(LanternCorps.YELLOW, 4, 3),
    RED_BURST(LanternCorps.RED, 2, 1),
    RED_DUELS(LanternCorps.RED, 3, 3),
    RED_PLASMA(LanternCorps.RED, 4, 1),
    ORANGE_HOARD(LanternCorps.ORANGE, 2, 1),
    ORANGE_STEAL(LanternCorps.ORANGE, 3, 2000),
    ORANGE_RING_KILL(LanternCorps.ORANGE, 4, 1),
    BLUE_HEAL(LanternCorps.BLUE, 2, 200),
    BLUE_HOPE(LanternCorps.BLUE, 3, 3),
    BLUE_SHIELD(LanternCorps.BLUE, 4, 300),
    INDIGO_MIMIC(LanternCorps.INDIGO, 2, 4),
    INDIGO_SAVE(LanternCorps.INDIGO, 3, 5),
    INDIGO_CONVERT(LanternCorps.INDIGO, 4, 1),
    SAPPHIRE_ENCASE(LanternCorps.STAR_SAPPHIRE, 2, 10),
    SAPPHIRE_BOND(LanternCorps.STAR_SAPPHIRE, 3, 600),
    SAPPHIRE_GUARD(LanternCorps.STAR_SAPPHIRE, 4, 1);

    private final LanternCorps corps;
    private final int rank;
    private final int goal;

    RankTask(LanternCorps corps, int rank, int goal) {
        this.corps = corps;
        this.rank = rank;
        this.goal = goal;
    }

    public LanternCorps corps() {
        return this.corps;
    }

    public int rank() {
        return this.rank;
    }

    public int goal() {
        return this.goal;
    }

    public String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
