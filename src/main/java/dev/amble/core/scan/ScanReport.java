package dev.amble.core.scan;

import dev.amble.core.blocks.LanternBlock;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.LanternCorps;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record ScanReport(Component title, List<Component> lines) {
    private static final double SPEED_TO_BLOCKS_PER_SECOND = 43.17;
    private static final int MAX_EFFECTS = 3;

    public static ScanReport of(ServerPlayer scanner, Entity entity) {
        List<Component> lines = new ArrayList<>();
        lines.add(line("type", entity.getType().getDescription()));

        if (entity instanceof LivingEntity living) {
            lines.add(line("health", number(living.getHealth()), number(living.getMaxHealth())));
            lines.add(line("armor", living.getArmorValue()));
            if (living.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                lines.add(line("attack", number(living.getAttributeValue(Attributes.ATTACK_DAMAGE))));
            }
            if (living.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                lines.add(line("speed", number(living.getAttributeValue(Attributes.MOVEMENT_SPEED) * SPEED_TO_BLOCKS_PER_SECOND)));
            }
            lines.add(line("disposition", disposition(entity)));

            List<Component> effects = living.getActiveEffects().stream()
                    .limit(MAX_EFFECTS)
                    .map(MobEffectInstance::getEffect)
                    .map(effect -> effect.value().getDisplayName())
                    .toList();
            if (!effects.isEmpty()) lines.add(line("effects", join(effects)));
        }

        if (entity instanceof OwnableEntity ownable && ownable.getOwner() != null) {
            lines.add(line("owner", ownable.getOwner().getDisplayName()));
        }

        if (entity instanceof Player player) {
            lines.add(PowerRingItem.getWornCorps(player)
                    .map(corps -> line("corps", corps.displayName().copy().withColor(corps.color()),
                            Math.round(PowerRingItem.getChargeFraction(PowerRingItem.getWornRing(player)) * 100)))
                    .orElseGet(() -> Component.translatable("scan.brightestday.ringless")));
        }

        lines.add(line("distance", number(scanner.distanceTo(entity))));
        return new ScanReport(entity.getDisplayName(), lines);
    }

    public static ScanReport of(ServerPlayer scanner, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        List<Component> lines = new ArrayList<>();

        if (state.getBlock() instanceof LanternBlock lantern) {
            LanternCorps corps = lantern.corps();
            lines.add(Component.translatable("scan.brightestday.lantern", corps.displayName()).withColor(corps.color()));
        }

        lines.add(line("id", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()));
        lines.add(line("hardness", number(state.getDestroySpeed(level, pos))));
        lines.add(line("blast_resistance", number(state.getBlock().getExplosionResistance())));
        lines.add(line("tool", tool(state)));
        if (state.getLightEmission() > 0) lines.add(line("light", state.getLightEmission()));
        lines.add(line("position", pos.getX(), pos.getY(), pos.getZ()));
        lines.add(line("distance", number(Math.sqrt(scanner.distanceToSqr(Vec3.atCenterOf(pos))))));

        return new ScanReport(state.getBlock().getName(), lines);
    }

    private static Component disposition(Entity entity) {
        if (entity instanceof Enemy) return Component.translatable("scan.brightestday.hostile").withStyle(ChatFormatting.RED);
        if (entity instanceof NeutralMob) return Component.translatable("scan.brightestday.neutral").withStyle(ChatFormatting.GOLD);
        return Component.translatable("scan.brightestday.passive").withStyle(ChatFormatting.GREEN);
    }

    private static Component tool(BlockState state) {
        String tool = state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? "pickaxe"
                : state.is(BlockTags.MINEABLE_WITH_AXE) ? "axe"
                : state.is(BlockTags.MINEABLE_WITH_SHOVEL) ? "shovel"
                : state.is(BlockTags.MINEABLE_WITH_HOE) ? "hoe"
                : "hand";
        MutableComponent name = Component.translatable("scan.brightestday.tool." + tool);
        return state.requiresCorrectToolForDrops() ? name.append(Component.translatable("scan.brightestday.required")) : name;
    }

    private static Component line(String key, Object... args) {
        return Component.translatable("scan.brightestday." + key, args);
    }

    private static Component join(List<Component> parts) {
        MutableComponent joined = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) joined.append(", ");
            joined.append(parts.get(i));
        }
        return joined;
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
