package dev.amble.mixin;

import dev.amble.core.official.OfficialServer;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerStatusMixin {
    @Inject(method = "buildServerStatus", at = @At("RETURN"), cancellable = true)
    private void brightestday$appendRoster(CallbackInfoReturnable<ServerStatus> cir) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        if (server.hidesOnlinePlayers()) return;
        List<OfficialServer.Member> members = server.getPlayerList().getPlayers().stream()
                .filter(ServerPlayer::allowsListing)
                .map(OfficialServer::member)
                .toList();
        ServerStatus status = cir.getReturnValue();
        cir.setReturnValue(new ServerStatus(status.description().copy().append(OfficialServer.tag(members)),
                status.players(), status.version(), status.favicon(), status.enforcesSecureChat()));
    }
}
