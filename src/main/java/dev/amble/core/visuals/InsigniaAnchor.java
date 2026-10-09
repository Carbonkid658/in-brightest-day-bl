package dev.amble.core.visuals;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public record InsigniaAnchor(float x, float y, float distance, int echoes) {
    public static final float WIDTH = 8.0F;
    public static final float HEIGHT = 12.0F;
    public static final float STEP = 0.5F;
    public static final float MAX_DISTANCE = 0.8F;
    public static final float DISTANCE_STEP = 0.05F;
    public static final int MAX_ECHOES = 2;
    public static final InsigniaAnchor DEFAULT = new InsigniaAnchor(4.0F, 1.5F, 0.3F, 0);

    public static final Codec<InsigniaAnchor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("x", DEFAULT.x).forGetter(InsigniaAnchor::x),
            Codec.FLOAT.optionalFieldOf("y", DEFAULT.y).forGetter(InsigniaAnchor::y),
            Codec.FLOAT.optionalFieldOf("distance", DEFAULT.distance).forGetter(InsigniaAnchor::distance),
            Codec.INT.optionalFieldOf("echoes", DEFAULT.echoes).forGetter(InsigniaAnchor::echoes)
    ).apply(instance, InsigniaAnchor::new));

    public static final StreamCodec<ByteBuf, InsigniaAnchor> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, InsigniaAnchor::x,
            ByteBufCodecs.FLOAT, InsigniaAnchor::y,
            ByteBufCodecs.FLOAT, InsigniaAnchor::distance,
            ByteBufCodecs.VAR_INT, InsigniaAnchor::echoes,
            InsigniaAnchor::new
    );

    public InsigniaAnchor withPosition(float x, float y) {
        return new InsigniaAnchor(x, y, this.distance, this.echoes);
    }

    public InsigniaAnchor withDistance(float distance) {
        return new InsigniaAnchor(this.x, this.y, distance, this.echoes);
    }

    public InsigniaAnchor withEchoes(int echoes) {
        return new InsigniaAnchor(this.x, this.y, this.distance, echoes);
    }

    public InsigniaAnchor sanitized() {
        return new InsigniaAnchor(snap(this.x, STEP, WIDTH), snap(this.y, STEP, HEIGHT), snap(this.distance, DISTANCE_STEP, MAX_DISTANCE),
                Mth.clamp(this.echoes, 0, MAX_ECHOES));
    }

    private static float snap(float value, float step, float max) {
        if (!Float.isFinite(value)) return 0.0F;
        return Mth.clamp(Math.round(value / step) * step, 0.0F, max);
    }
}
