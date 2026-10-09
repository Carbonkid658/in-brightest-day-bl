package dev.amble.core.mannequin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.LanternCorps;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

public record Hologram(ItemStack ring, Optional<UUID> owner, HologramSettings settings) {
    public static final Hologram EMPTY = new Hologram(ItemStack.EMPTY, Optional.empty(), HologramSettings.DEFAULT);

    public static final Codec<Hologram> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("ring", ItemStack.EMPTY).forGetter(Hologram::ring),
            UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Hologram::owner),
            HologramSettings.CODEC.optionalFieldOf("settings", HologramSettings.DEFAULT).forGetter(Hologram::settings)
    ).apply(instance, Hologram::new));

    public Optional<LanternCorps> corps() {
        return PowerRingItem.getCorps(this.ring);
    }

    public boolean ownedBy(UUID player) {
        return this.owner.map(player::equals).orElse(true);
    }

    public Hologram withRing(ItemStack ring) {
        return new Hologram(ring, this.owner, this.settings);
    }

    public Hologram withSettings(HologramSettings settings) {
        return new Hologram(this.ring, this.owner, settings);
    }
}
