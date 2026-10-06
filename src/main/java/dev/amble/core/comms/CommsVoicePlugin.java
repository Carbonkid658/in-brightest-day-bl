package dev.amble.core.comms;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import dev.amble.BrightestDay;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class CommsVoicePlugin implements VoicechatPlugin {
    public static final String CATEGORY = "brightestday_comms";

    @Override
    public String getPluginId() {
        return BrightestDay.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        VoicechatServerApi api = event.getVoicechat();
        api.registerVolumeCategory(api.volumeCategoryBuilder()
                .setId(CATEGORY)
                .setName("Ring Comms")
                .setDescription("Teammates talking to you through your power ring")
                .build());
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null) return;
        UUID target = Comms.receiver(sender.getPlayer().getUuid());
        if (target == null) return;

        VoicechatServerApi api = event.getVoicechat();
        VoicechatConnection receiver = api.getConnectionOf(target);
        if (receiver == null || !receiver.isConnected()) return;
        if (receiver.getPlayer().getPlayer() instanceof ServerPlayer listener && sender.getPlayer().getPlayer() instanceof ServerPlayer speaker
                && listener.level() == speaker.level() && listener.distanceTo(speaker) <= api.getVoiceChatDistance()) return;

        api.sendStaticSoundPacketTo(receiver, event.getPacket().staticSoundPacketBuilder().category(CATEGORY).build());
    }
}
