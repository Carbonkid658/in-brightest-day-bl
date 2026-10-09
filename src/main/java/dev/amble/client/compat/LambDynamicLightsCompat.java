package dev.amble.client.compat;

import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.entity.EntityLightSource;
import dev.lambdaurora.lambdynlights.api.entity.EntityLightSourceManager;
import net.minecraft.advancements.predicates.entity.EntityTypePredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityTypes;

import java.util.List;
import java.util.Optional;

public class LambDynamicLightsCompat implements DynamicLightsInitializer {

    @Override
    public void onInitializeDynamicLights(DynamicLightsContext context) {
        var event = context.entityLightSourceManager().onRegisterEvent();
        event.register(event.defaultPhaseId(), LambDynamicLightsCompat::registerPlayerLight);
    }

    private static void registerPlayerLight(EntityLightSourceManager.RegisterContext context) {
        context.register(new EntityLightSource(
                new EntityLightSource.EntityPredicate(
                        Optional.of(EntityTypePredicate.of(context.registryLookup().lookupOrThrow(Registries.ENTITY_TYPE), EntityTypes.PLAYER)),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                ),
                List.of(RingLightLuminance.INSTANCE)
        ));
    }
}
