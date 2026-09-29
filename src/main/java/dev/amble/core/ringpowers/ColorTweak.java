package dev.amble.core.ringpowers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public record ColorTweak(float brightness, float saturation, boolean aura) {
    public static final float MIN_SATURATION = -0.6F;
    public static final ColorTweak NONE = new ColorTweak(0.0F, 0.0F, false);

    public static final Codec<ColorTweak> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(-1.0F, 1.0F).optionalFieldOf("brightness", 0.0F).forGetter(ColorTweak::brightness),
            Codec.floatRange(-1.0F, 1.0F).optionalFieldOf("saturation", 0.0F).forGetter(ColorTweak::saturation),
            Codec.BOOL.optionalFieldOf("aura", false).forGetter(ColorTweak::aura)
    ).apply(instance, ColorTweak::new));

    public static final StreamCodec<ByteBuf, ColorTweak> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ColorTweak::brightness,
            ByteBufCodecs.FLOAT, ColorTweak::saturation,
            ByteBufCodecs.BOOL, ColorTweak::aura,
            ColorTweak::new
    );

    public ColorTweak clamped() {
        return new ColorTweak(Mth.clamp(this.brightness, -1.0F, 1.0F), Mth.clamp(this.saturation, MIN_SATURATION, 1.0F), this.aura);
    }
}
