package dev.amble.core.ringpowers.constructs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record ConstructToolData(UUID owner, long expiresAt) {
    public static final Codec<ConstructToolData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(ConstructToolData::owner),
            Codec.LONG.fieldOf("expires_at").forGetter(ConstructToolData::expiresAt)
    ).apply(instance, ConstructToolData::new));

    public static final StreamCodec<ByteBuf, ConstructToolData> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ConstructToolData::owner,
            ByteBufCodecs.VAR_LONG, ConstructToolData::expiresAt,
            ConstructToolData::new
    );
}
