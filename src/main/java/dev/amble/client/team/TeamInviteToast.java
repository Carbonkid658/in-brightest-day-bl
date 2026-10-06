package dev.amble.client.team;

import dev.amble.client.screens.LanternWidgets;
import dev.amble.core.BrightestDayItems;
import dev.amble.core.ringpowers.LanternCorps;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public class TeamInviteToast implements Toast {
    private static final long DISPLAY_MS = 8000L;
    private static final int WIDTH = 200;
    private static final int HEIGHT = 32;
    private static final int TEXT_X = 28;

    private final UUID inviter;
    private final Component title;
    private final Component hint;
    private final @Nullable LanternCorps corps;
    private Toast.Visibility visibility = Toast.Visibility.SHOW;

    public TeamInviteToast(UUID inviter, Component name, @Nullable LanternCorps corps) {
        this.inviter = inviter;
        this.corps = corps;
        this.title = Component.translatable("toast.brightestday.team_invite.title", name);
        this.hint = Component.translatable("toast.brightestday.team_invite.hint", Component.keybind("key.brightestday.team"));
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return this.visibility;
    }

    @Override
    public void update(ToastManager manager, long fullyVisibleForMs) {
        boolean expired = fullyVisibleForMs >= DISPLAY_MS * manager.getNotificationDisplayTimeMultiplier();
        this.visibility = expired || !ClientTeams.invitedBy(this.inviter) ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
        int accent = this.corps != null ? ARGB.opaque(this.corps.color()) : LanternWidgets.accent();
        LanternWidgets.panel(graphics, 0, 0, this.width(), this.height(), accent);
        if (this.corps != null) graphics.item(new ItemStack(BrightestDayItems.ring(this.corps)), 8, 8);
        graphics.text(font, font.plainSubstrByWidth(this.title.getString(), WIDTH - TEXT_X - 6), TEXT_X, 7, accent, true);
        graphics.text(font, font.plainSubstrByWidth(this.hint.getString(), WIDTH - TEXT_X - 6), TEXT_X, 18, LanternWidgets.TEXT, false);
    }

    @Override
    public Object getToken() {
        return this.inviter;
    }

    @Override
    public int width() {
        return WIDTH;
    }

    @Override
    public int height() {
        return HEIGHT;
    }
}
