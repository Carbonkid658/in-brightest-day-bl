package dev.amble.core.ringpowers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public record ColorTweak(float brightness, float saturation) {
    public static final ColorTweak NONE = new ColorTweak(0.0F, 0.0F);

    public static final Codec<ColorTweak> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(-1.0F, 1.0F).optionalFieldOf("brightness", 0.0F).forGetter(ColorTweak::brightness),
            Codec.floatRange(-1.0F, 1.0F).optionalFieldOf("saturation", 0.0F).forGetter(ColorTweak::saturation)
    ).apply(instance, ColorTweak::new));

    public static final StreamCodec<ByteBuf, ColorTweak> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ColorTweak::brightness,
            ByteBufCodecs.FLOAT, ColorTweak::saturation,
            ColorTweak::new
    );

    public ColorTweak clamped() {
        return new ColorTweak(Mth.clamp(this.brightness, -1.0F, 1.0F), Mth.clamp(this.saturation, -1.0F, 1.0F));
    }
}
