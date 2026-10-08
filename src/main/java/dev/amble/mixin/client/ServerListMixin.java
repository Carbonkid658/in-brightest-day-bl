package dev.amble.mixin.client;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Locale;

@Mixin(ServerList.class)
public abstract class ServerListMixin {
    @Unique
    private static final String OFFICIAL_NAME = "In Brightest Day - Official Server";
    @Unique
    private static final String OFFICIAL_IP = "in-brightest-day.modrinth.gg";

    @Shadow
    @Final
    private List<ServerData> serverList;

    @Inject(method = "load", at = @At("TAIL"))
    private void brightestday$pinOfficialServer(CallbackInfo ci) {
        this.serverList.removeIf(server -> brightestday$isOfficial(server.ip));
        this.serverList.addFirst(new ServerData(OFFICIAL_NAME, OFFICIAL_IP, ServerData.Type.OTHER));
    }

    @Unique
    private static boolean brightestday$isOfficial(String ip) {
        String address = ip.trim().toLowerCase(Locale.ROOT);
        if (address.endsWith(":25565")) address = address.substring(0, address.length() - ":25565".length());
        if (address.endsWith(".")) address = address.substring(0, address.length() - 1);
        return address.equals(OFFICIAL_IP);
    }
}
