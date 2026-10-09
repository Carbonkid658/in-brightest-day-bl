package dev.amble.core.progression;

import com.mojang.serialization.Codec;
import dev.amble.core.ringpowers.LanternCorps;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;

public enum Emotion implements StringRepresentable {
    RAGE("rage", LanternCorps.RED),
    AVARICE("avarice", LanternCorps.ORANGE),
    FEAR("fear", LanternCorps.YELLOW),
    WILL("will", LanternCorps.GREEN),
    HOPE("hope", LanternCorps.BLUE),
    COMPASSION("compassion", LanternCorps.INDIGO),
    LOVE("love", LanternCorps.STAR_SAPPHIRE);

    public static final Codec<Emotion> CODEC = StringRepresentable.fromEnum(Emotion::values);
    public static final int MAX = 1000;
    public static final int GATE = 250;

    private final String name;
    private final LanternCorps corps;

    Emotion(String name, LanternCorps corps) {
        this.name = name;
        this.corps = corps;
    }

    public LanternCorps corps() {
        return this.corps;
    }

    public static Optional<Emotion> of(LanternCorps corps) {
        for (Emotion emotion : values()) {
            if (emotion.corps == corps) return Optional.of(emotion);
        }
        return Optional.empty();
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
