package dev.amble.client.render;

import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.ColorTweak;
import dev.amble.core.ringpowers.CorpsColors;
import dev.amble.core.ringpowers.LanternCorps;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class LanternSuit {
    public static final RenderStateDataKey<Identifier> GLOW = RenderStateDataKey.create(() -> "brightestday:suit_glow");

    private static final float FADE_TICKS = 24.0F;
    private static final Map<Integer, Fade> FADES = new HashMap<>();
    private static @Nullable ClientLevel level;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(LanternSuit::tick);
    }

    private static void tick(Minecraft client) {
        if (client.level != level) {
            level = client.level;
            FADES.clear();
            SuitTextures.clear();
        }
        if (level == null) return;

        for (AbstractClientPlayer player : level.players()) {
            Optional<LanternCorps> corps = PowerRingItem.getWornCorps(player);
            ColorTweak tweak = BrightestDayAttachments.getColorTweak(player);
            boolean wanted = corps.isPresent()
                    && tweak.suit()
                    && PowerRingItem.hasCharge(player);

            Fade fade = FADES.get(player.getId());
            if (fade == null) {
                if (!wanted) continue;
                fade = new Fade(tweak.mask());
                FADES.put(player.getId(), fade);
            }
            if (wanted) {
                fade.corps = corps.get();
                fade.color = CorpsColors.of(player);
                fade.maskOffset = tweak.maskOffset();
            }
            fade.tick(wanted, tweak.mask());
        }

        FADES.entrySet().removeIf(entry -> {
            boolean done = !(level.getEntity(entry.getKey()) instanceof Player) || entry.getValue().hidden();
            if (done) SuitTextures.release(entry.getKey());
            return done;
        });
    }

    public static void extract(Avatar entity, AvatarRenderState state, float partialTicks) {
        FabricRenderState data = (FabricRenderState) state;
        data.setData(GLOW, null);

        Fade fade = FADES.get(entity.getId());
        if (fade == null || !(entity instanceof Player)) return;

        float progress = fade.progress(partialTicks);
        if (progress <= 0.0F) return;

        float maskProgress = fade.maskProgress(partialTicks);
        SuitTextures.Entry entry = SuitTextures.update(entity.getId(), state.skin, fade.corps, state.mainArm, fade.maskOffset, maskProgress, progress, fade.color);
        if (entry == null) return;

        PlayerSkin skin = state.skin;
        state.skin = new PlayerSkin(new ClientAsset.ResourceTexture(entry.bodyId, entry.bodyId), skin.cape(), skin.elytra(), skin.model(), skin.secure());
        if (progress < 1.0F || (maskProgress > 0.0F && maskProgress < 1.0F)) data.setData(GLOW, entry.glowId);
    }

    private static final class Fade {
        private float previous;
        private float current;
        private LanternCorps corps = LanternCorps.GREEN;
        private int color = LanternCorps.GREEN.color();
        private int maskOffset;
        private float maskPrevious;
        private float maskCurrent;

        private Fade(boolean mask) {
            this.maskPrevious = this.maskCurrent = mask ? 1.0F : 0.0F;
        }

        private void tick(boolean wanted, boolean mask) {
            this.previous = this.current;
            this.current = Mth.approach(this.current, wanted ? 1.0F : 0.0F, 1.0F / FADE_TICKS);
            this.maskPrevious = this.maskCurrent;
            this.maskCurrent = Mth.approach(this.maskCurrent, mask ? 1.0F : 0.0F, 1.0F / FADE_TICKS);
        }

        private float progress(float partialTicks) {
            return Mth.lerp(partialTicks, this.previous, this.current);
        }

        private float maskProgress(float partialTicks) {
            return Mth.lerp(partialTicks, this.maskPrevious, this.maskCurrent);
        }

        private boolean hidden() {
            return this.previous <= 0.0F && this.current <= 0.0F;
        }
    }

    private LanternSuit() {}
}
