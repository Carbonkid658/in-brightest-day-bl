package dev.amble.core.ringpowers;

import com.mojang.serialization.Codec;
import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

public enum LanternCorps implements StringRepresentable, Translatable {
    GREEN("green", 0x00E03C),
    BLUE("blue", 0x2D8CFF),
    YELLOW("yellow", 0xFFE01A),
    ORANGE("orange", 0xFF8A00),
    RED("red", 0xE0141E),
    INDIGO("indigo", 0x5B2DB3),
    STAR_SAPPHIRE("star_sapphire", 0xE040C8),
    WHITE("white", 0xF4F4F4),
    BLACK("black", 0x2A2A2A);

    public static final Codec<LanternCorps> CODEC = StringRepresentable.fromEnum(LanternCorps::values);

    private static final IntFunction<LanternCorps> BY_ID =
            ByIdMap.continuous(LanternCorps::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

    public static final StreamCodec<ByteBuf, LanternCorps> STREAM_CODEC =
            ByteBufCodecs.idMapper(BY_ID, LanternCorps::ordinal);

    private final String name;
    private final int color;

    LanternCorps(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public int color() {
        return this.color;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    @Override
    public String getTranslationKey() {
        return BrightestDay.MOD_ID + ".lantern_corps." + this.name;
    }
}
