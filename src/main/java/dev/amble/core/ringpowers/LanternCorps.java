package dev.amble.core.ringpowers;

import com.mojang.serialization.Codec;
import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

public enum LanternCorps implements StringRepresentable, Translatable {
    GREEN("green", 0x00E03C, true),
    BLUE("blue", 0x2D8CFF, false),
    YELLOW("yellow", 0xFFE01A, true),
    ORANGE("orange", 0xFF8A00, true),
    RED("red", 0xE0141E, false),
    INDIGO("indigo", 0x5B2DB3, true),
    STAR_SAPPHIRE("star_sapphire", 0xE040C8, true),
    WHITE("white", 0xF4F4F4, true),
    BLACK("black", 0x2A2A2A, false);

    public static final Codec<LanternCorps> CODEC = StringRepresentable.fromEnum(LanternCorps::values);

    private static final IntFunction<LanternCorps> BY_ID =
            ByIdMap.continuous(LanternCorps::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

    public static final StreamCodec<ByteBuf, LanternCorps> STREAM_CODEC =
            ByteBufCodecs.idMapper(BY_ID, LanternCorps::ordinal);

    private final String name;
    private final int color;
    private final boolean constructs;

    LanternCorps(String name, int color, boolean constructs) {
        this.name = name;
        this.color = color;
        this.constructs = constructs;
    }

    public boolean canUse(RingPowerCategory category) {
        return category != RingPowerCategory.CONSTRUCT || this.constructs;
    }

    public Component displayName() {
        return Component.translatable(this.getTranslationKey());
    }

    public String oathKey() {
        return BrightestDay.MOD_ID + ".oath." + this.name;
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
