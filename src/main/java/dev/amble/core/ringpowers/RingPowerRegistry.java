package dev.amble.core.ringpowers;

import dev.amble.core.ringpowers.constructs.AreaShieldConstruct;
import dev.amble.core.ringpowers.constructs.BeamConstruct;
import dev.amble.core.ringpowers.constructs.BlastConstruct;
import dev.amble.core.ringpowers.constructs.EntityShieldConstruct;
import dev.amble.core.ringpowers.constructs.HealBeamConstruct;
import dev.amble.core.ringpowers.constructs.SculptConstruct;
import dev.amble.core.ringpowers.constructs.ToolForgeConstruct;
import dev.amble.core.ringpowers.constructs.WallConstruct;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import dev.amble.core.ringpowers.impl.FlightRingPower;
import dev.amble.core.ringpowers.impl.LightRingPower;
import dev.amble.core.ringpowers.impl.ScanRingPower;
import dev.amble.core.ringpowers.impl.TractorBeamRingPower;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RingPowerRegistry {
    private static final Map<Identifier, RingPower<?>> REGISTRY = new LinkedHashMap<>();

    public static final FlightRingPower FLIGHT = register(new FlightRingPower());
    public static final ArmedRingPower ARMED = register(new ArmedRingPower());
    public static final LightRingPower LIGHT = register(new LightRingPower());
    public static final BlastConstruct BLAST = register(new BlastConstruct());
    public static final BeamConstruct BEAM = register(new BeamConstruct());
    public static final HealBeamConstruct HEAL_BEAM = register(new HealBeamConstruct());
    public static final EntityShieldConstruct ENTITY_SHIELD = register(new EntityShieldConstruct());
    public static final AreaShieldConstruct AREA_SHIELD = register(new AreaShieldConstruct());
    public static final WallConstruct WALL = register(new WallConstruct());
    public static final SculptConstruct SCULPT = register(new SculptConstruct());
    public static final ToolForgeConstruct TOOL_FORGE = register(new ToolForgeConstruct());
    public static final TractorBeamRingPower TRACTOR_BEAM = register(new TractorBeamRingPower());
    public static final ScanRingPower SCAN = register(new ScanRingPower());

    public static <T extends RingPower<?>> T register(T power) {
        if (REGISTRY.putIfAbsent(power.id(), power) != null) {
            throw new IllegalStateException("Duplicate ring power id: " + power.id());
        }
        return power;
    }

    public static Optional<RingPower<?>> get(Identifier id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    public static Collection<RingPower<?>> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static List<RingPower<?>> forCorps(LanternCorps corps, @Nullable LanternCorps borrowed) {
        return REGISTRY.values().stream()
                .filter(power -> power.isAvailableTo(corps) || borrowed != null && power.isAvailableTo(borrowed))
                .toList();
    }

    public static List<RingPower<?>> forCorps(LanternCorps corps) {
        return REGISTRY.values().stream().filter(power -> power.isAvailableTo(corps)).toList();
    }

    public static void init() {}

    private RingPowerRegistry() {}
}
