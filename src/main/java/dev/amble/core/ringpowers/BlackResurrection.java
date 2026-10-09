package dev.amble.core.ringpowers;

import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.BrightestDayComponents;
import dev.amble.core.items.PowerRingItem;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.ItemStack;

/** Death is only a suggestion to a Black Lantern: a fatal blow has a small chance to be refused. */
public final class BlackResurrection {
    public static final float CHANCE = 0.05F;
    /** The ring must hold more than this fraction of its charge to refuse death... */
    public static final float REQUIRED_CHARGE = 0.51F;
    /** ...and refusing it costs this fraction of the ring's full capacity. */
    public static final float CHARGE_COST = 0.5F;
    private static final float REVIVE_HEALTH = 3.0F;

    public static void init() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer player) || !BlackFeeding.isBlack(player)) return true;
            // /kill and the void bypass invulnerability; there is nothing to rise back to.
            if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
            if (player.getRandom().nextFloat() >= CHANCE) return true;
            ItemStack ring = PowerRingItem.getWornRing(player);
            boolean free = player.hasInfiniteMaterials();
            if (!free && PowerRingItem.getChargeFraction(ring) <= REQUIRED_CHARGE) return true;
            if (!free) {
                PowerRingItem.drainRing(ring, Math.round(BrightestDayComponents.MAX_POWER * CHARGE_COST));
                if (ring == BrightestDayAttachments.getRing(player)) BrightestDayAttachments.setRing(player, ring);
            }
            rise(player);
            return false;
        });
    }

    private static void rise(ServerPlayer player) {
        player.setHealth(Math.min(REVIVE_HEALTH, player.getMaxHealth()));
        player.removeAllEffects();
        player.clearFire();

        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1.0), player.getZ(), 40, 0.4, 0.6, 0.4, 0.3);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.0F, 0.5F);

        MinecraftServer server = level.getServer();
        Component message = Component.translatable("message.brightestday.black.rise", player.getDisplayName())
                .withStyle(ChatFormatting.BOLD).withColor(LanternCorps.BLACK.textColor());
        for (ServerPlayer other : server.getPlayerList().getPlayers()) other.sendSystemMessage(message);
    }

    private BlackResurrection() {}
}
