package dev.amble.core.ringpowers;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.tags.DamageTypeTags;
import dev.amble.BrightestDay;
import dev.amble.core.items.PowerRingItem;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class RingBenefits {
    private static final Identifier MODIFIER_ID = BrightestDay.id("ring_benefits");

    private static final int REGEN_INTERVAL = 40;
    private static final float REGEN_AMOUNT = 1.0F;
    private static final int WATER_DRAIN_PER_SECOND = 1;
    private static final int LAVA_DRAIN_PER_SECOND = 3;

    private record Bonus(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {}

    private static final List<Bonus> BONUSES = List.of(
            new Bonus(Attributes.ATTACK_DAMAGE, 3.0, AttributeModifier.Operation.ADD_VALUE),
            new Bonus(Attributes.ARMOR, 4.0, AttributeModifier.Operation.ADD_VALUE),
            new Bonus(Attributes.ARMOR_TOUGHNESS, 2.0, AttributeModifier.Operation.ADD_VALUE),
            new Bonus(Attributes.KNOCKBACK_RESISTANCE, 0.25, AttributeModifier.Operation.ADD_VALUE),
            new Bonus(Attributes.MOVEMENT_SPEED, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new Bonus(Attributes.BLOCK_BREAK_SPEED, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new Bonus(Attributes.SUBMERGED_MINING_SPEED, 0.8, AttributeModifier.Operation.ADD_VALUE)
    );

    public static void init() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(entity instanceof Player player && source.is(DamageTypeTags.IS_FALL) && isActive(player)));
    }

    public static boolean isActive(Player player) {
        return !PowerRingItem.getWornRing(player).isEmpty() && PowerRingItem.hasCharge(player);
    }

    public static void tick(ServerPlayer player, int serverTick) {
        boolean active = isActive(player);
        for (Bonus bonus : BONUSES) {
            AttributeInstance instance = player.getAttribute(bonus.attribute());
            if (instance == null) continue;

            boolean applied = instance.hasModifier(MODIFIER_ID);
            if (active && !applied) {
                instance.addTransientModifier(new AttributeModifier(MODIFIER_ID, bonus.amount(), bonus.operation()));
            } else if (!active && applied) {
                instance.removeModifier(MODIFIER_ID);
            }
        }
        if (!active) return;

        if (player.isOnFire()) player.clearFire();
        if (player.getFoodData().getFoodLevel() < 1) player.getFoodData().setFoodLevel(1);

        if (serverTick % REGEN_INTERVAL == 0 && player.isAlive() && player.getHealth() < player.getMaxHealth()) {
            player.heal(REGEN_AMOUNT);
        }

        if (serverTick % 20 == 0 && !player.hasInfiniteMaterials()) {
            if (player.isInLava()) PowerRingItem.drainWorn(player, LAVA_DRAIN_PER_SECOND);
            else if (player.isUnderWater()) PowerRingItem.drainWorn(player, WATER_DRAIN_PER_SECOND);
        }
    }

    private RingBenefits() {}
}
