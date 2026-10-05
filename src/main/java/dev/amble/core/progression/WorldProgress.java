package dev.amble.core.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.LanternCorps;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

public record WorldProgress(Optional<UUID> indigoOne, boolean meteorFallen, Optional<BlockPos> sanctuary, boolean sanctuaryBuilt,
                            List<Shrine> shrines, Set<UUID> blessed, List<Battery> batteries) {
    public static final WorldProgress EMPTY = new WorldProgress(Optional.empty(), false, Optional.empty(), false, List.of(), Set.of(), List.of());

    public record Shrine(int index, BlockPos pos) {
        public static final Codec<Shrine> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("index").forGetter(Shrine::index),
                BlockPos.CODEC.fieldOf("pos").forGetter(Shrine::pos)
        ).apply(instance, Shrine::new));
    }

    public record Battery(BlockPos pos, LanternCorps corps, boolean active) {
        public static final Codec<Battery> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Battery::pos),
                LanternCorps.CODEC.fieldOf("corps").forGetter(Battery::corps),
                Codec.BOOL.optionalFieldOf("active", false).forGetter(Battery::active)
        ).apply(instance, Battery::new));

        public Battery withActive(boolean active) {
            return new Battery(this.pos, this.corps, active);
        }
    }

    public static final Codec<WorldProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.optionalFieldOf("indigo_one").forGetter(WorldProgress::indigoOne),
            Codec.BOOL.optionalFieldOf("meteor_fallen", false).forGetter(WorldProgress::meteorFallen),
            BlockPos.CODEC.optionalFieldOf("sanctuary").forGetter(WorldProgress::sanctuary),
            Codec.BOOL.optionalFieldOf("sanctuary_built", false).forGetter(WorldProgress::sanctuaryBuilt),
            Shrine.CODEC.listOf().optionalFieldOf("shrines", List.of()).forGetter(WorldProgress::shrines),
            UUIDUtil.CODEC.listOf().xmap(list -> (Set<UUID>) new HashSet<>(list), List::copyOf).optionalFieldOf("blessed", Set.of()).forGetter(WorldProgress::blessed),
            Battery.CODEC.listOf().optionalFieldOf("batteries", List.of()).forGetter(WorldProgress::batteries)
    ).apply(instance, WorldProgress::new));

    public static final AttachmentType<WorldProgress> STATE =
            AttachmentRegistry.<WorldProgress>builder()
                    .initializer(() -> EMPTY)
                    .persistent(CODEC)
                    .buildAndRegister(BrightestDay.id("world_progress"));

    public static void init() {}

    public static WorldProgress get(MinecraftServer server) {
        return server.overworld().getAttachedOrElse(STATE, EMPTY);
    }

    public static void update(MinecraftServer server, UnaryOperator<WorldProgress> change) {
        server.overworld().setAttached(STATE, change.apply(get(server)));
    }

    public WorldProgress withIndigoOne(UUID uuid) {
        return new WorldProgress(Optional.of(uuid), this.meteorFallen, this.sanctuary, this.sanctuaryBuilt, this.shrines, this.blessed, this.batteries);
    }

    public WorldProgress withoutIndigoOne() {
        return new WorldProgress(Optional.empty(), this.meteorFallen, this.sanctuary, this.sanctuaryBuilt, this.shrines, this.blessed, this.batteries);
    }

    public WorldProgress withMeteorFallen() {
        return new WorldProgress(this.indigoOne, true, this.sanctuary, this.sanctuaryBuilt, this.shrines, this.blessed, this.batteries);
    }

    public WorldProgress withSanctuary(BlockPos pos, boolean built) {
        return new WorldProgress(this.indigoOne, this.meteorFallen, Optional.of(pos), built, this.shrines, this.blessed, this.batteries);
    }

    public WorldProgress withShrine(int index, BlockPos pos) {
        List<Shrine> shrines = new ArrayList<>(this.shrines);
        shrines.removeIf(shrine -> shrine.index() == index);
        shrines.add(new Shrine(index, pos));
        return new WorldProgress(this.indigoOne, this.meteorFallen, this.sanctuary, this.sanctuaryBuilt, List.copyOf(shrines), this.blessed, this.batteries);
    }

    public Optional<BlockPos> shrine(int index) {
        return this.shrines.stream().filter(shrine -> shrine.index() == index).map(Shrine::pos).findFirst();
    }

    public WorldProgress withBlessed(UUID uuid) {
        Set<UUID> blessed = new HashSet<>(this.blessed);
        blessed.add(uuid);
        return new WorldProgress(this.indigoOne, this.meteorFallen, this.sanctuary, this.sanctuaryBuilt, this.shrines, Set.copyOf(blessed), this.batteries);
    }

    public WorldProgress withBatteries(List<Battery> batteries) {
        return new WorldProgress(this.indigoOne, this.meteorFallen, this.sanctuary, this.sanctuaryBuilt, this.shrines, this.blessed, List.copyOf(batteries));
    }

    public WorldProgress withBattery(Battery battery) {
        List<Battery> batteries = new ArrayList<>(this.batteries);
        batteries.removeIf(existing -> existing.pos().equals(battery.pos()));
        batteries.add(battery);
        return this.withBatteries(batteries);
    }
}
