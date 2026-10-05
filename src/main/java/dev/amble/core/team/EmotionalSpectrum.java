package dev.amble.core.team;

import dev.amble.core.ringpowers.LanternCorps;

import java.util.List;

public final class EmotionalSpectrum {
    public static final List<LanternCorps> VISIBLE = List.of(
            LanternCorps.RED, LanternCorps.ORANGE, LanternCorps.YELLOW, LanternCorps.GREEN,
            LanternCorps.BLUE, LanternCorps.INDIGO, LanternCorps.STAR_SAPPHIRE);
    public static final int OPPOSED = VISIBLE.size();

    public enum Affinity {
        SAME("same"),
        KINDRED("kindred"),
        ALIGNED("aligned"),
        DISTANT("distant"),
        OPPOSED("opposed");

        private final String key;

        Affinity(String key) {
            this.key = key;
        }

        public String translationKey() {
            return "gui.brightestday.team.affinity." + this.key;
        }
    }

    public static int position(LanternCorps corps) {
        return VISIBLE.indexOf(corps);
    }

    public static int distance(LanternCorps from, LanternCorps to) {
        if (from == to) return 0;
        if (from == LanternCorps.BLACK || to == LanternCorps.BLACK) return OPPOSED;
        if (from == LanternCorps.WHITE || to == LanternCorps.WHITE) return 1;
        return Math.abs(position(from) - position(to));
    }

    public static Affinity affinity(LanternCorps from, LanternCorps to) {
        int distance = distance(from, to);
        if (distance == 0) return Affinity.SAME;
        if (distance == 1) return Affinity.KINDRED;
        if (distance <= 3) return Affinity.ALIGNED;
        if (distance < OPPOSED) return Affinity.DISTANT;
        return Affinity.OPPOSED;
    }

    private EmotionalSpectrum() {}
}
