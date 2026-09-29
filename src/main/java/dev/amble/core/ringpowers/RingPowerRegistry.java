package dev.amble.core.ringpowers;

import dev.amble.core.ringpowers.impl.FlightRingPower;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RingPowerRegistry {
    private static final Map<Identifier, RingPower<?>> REGISTRY = new LinkedHashMap<>();

    public static final FlightRingPower FLIGHT = register(new FlightRingPower());

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

    public static List<RingPower<?>> forCorps(LanternCorps corps) {
        return REGISTRY.values().stream().filter(power -> power.isAvailableTo(corps)).toList();
    }

    public static void init() {}

    private RingPowerRegistry() {}
}
