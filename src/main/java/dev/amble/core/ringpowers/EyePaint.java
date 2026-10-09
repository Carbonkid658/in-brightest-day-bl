package dev.amble.core.ringpowers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record EyePaint(long white, long corps) {
    public static final int SIZE = 8;
    public static final int NONE = 0;
    public static final int WHITE = 1;
    public static final int CORPS = 2;
    public static final EyePaint EMPTY = new EyePaint(0L, 0L);

    public static final Codec<EyePaint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("white", 0L).forGetter(EyePaint::white),
            Codec.LONG.optionalFieldOf("corps", 0L).forGetter(EyePaint::corps)
    ).apply(instance, EyePaint::new));

    public static final StreamCodec<ByteBuf, EyePaint> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.LONG, EyePaint::white,
            ByteBufCodecs.LONG, EyePaint::corps,
            EyePaint::new
    );

    public boolean isEmpty() {
        return this.white == 0L && this.corps == 0L;
    }

    public int get(int column, int row) {
        long bit = bit(column, row);
        if ((this.white & bit) != 0L) return WHITE;
        if ((this.corps & bit) != 0L) return CORPS;
        return NONE;
    }

    public EyePaint with(int column, int row, int kind) {
        long bit = bit(column, row);
        long white = this.white & ~bit;
        long corps = this.corps & ~bit;
        if (kind == WHITE) white |= bit;
        if (kind == CORPS) corps |= bit;
        return new EyePaint(white, corps);
    }

    public EyePaint sanitized() {
        return new EyePaint(this.white, this.corps & ~this.white);
    }

    private static long bit(int column, int row) {
        return 1L << (row * SIZE + column);
    }
}
