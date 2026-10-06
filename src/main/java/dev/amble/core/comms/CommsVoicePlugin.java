package dev.amble.core.comms;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import dev.amble.BrightestDay;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CommsVoicePlugin implements VoicechatPlugin {
    public static final String CATEGORY = "ring_comms";
    public static final String MEGAPHONE_CATEGORY = "megaphone";

    private record Codec(OpusDecoder decoder, OpusEncoder encoder) {
        byte[] boost(byte[] opus) {
            if (opus.length == 0) {
                this.decoder.resetState();
                this.encoder.resetState();
                return opus;
            }
            short[] samples = this.decoder.decode(opus);
            for (int i = 0; i < samples.length; i++) {
                samples[i] = (short) Math.clamp(Math.round(samples[i] * Megaphone.GAIN), Short.MIN_VALUE, Short.MAX_VALUE);
            }
            return this.encoder.encode(samples);
        }

        void close() {
            this.decoder.close();
            this.encoder.close();
        }
    }

    private static final Map<UUID, Codec> CODECS = new ConcurrentHashMap<>();

    @Override
    public String getPluginId() {
        return BrightestDay.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
    }

    private void amplify(MicrophonePacketEvent event, VoicechatConnection sender) {
        UUID id = sender.getPlayer().getUuid();
        if (!Megaphone.isActive(id)) {
            Codec codec = CODECS.remove(id);
            if (codec != null) codec.close();
            return;
        }

        VoicechatServerApi api = event.getVoicechat();
        Codec codec = CODECS.computeIfAbsent(id, key -> new Codec(api.createDecoder(), api.createEncoder()));
        byte[] boosted = codec.boost(event.getPacket().getOpusEncodedData());
        float range = (float) (api.getVoiceChatDistance() * Megaphone.RANGE_MULTIPLIER);
        event.cancel();

        de.maxhenkel.voicechat.api.ServerPlayer speaker = sender.getPlayer();
        for (de.maxhenkel.voicechat.api.ServerPlayer listener : api.getPlayersInRange(speaker.getServerLevel(), speaker.getPosition(), range)) {
            if (listener.getUuid().equals(id)) continue;
            VoicechatConnection connection = api.getConnectionOf(listener);
            if (connection == null || !connection.isConnected()) continue;
            api.sendEntitySoundPacketTo(connection, event.getPacket().entitySoundPacketBuilder()
                    .entityUuid(id)
                    .distance(range)
                    .opusEncodedData(boosted)
                    .category(MEGAPHONE_CATEGORY)
                    .build());
        }
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        VoicechatServerApi api = event.getVoicechat();
        api.registerVolumeCategory(api.volumeCategoryBuilder()
                .setId(CATEGORY)
                .setName("Ring Comms")
                .setDescription("Teammates talking to you through your power ring")
                .build());
        api.registerVolumeCategory(api.volumeCategoryBuilder()
                .setId(MEGAPHONE_CATEGORY)
                .setName("Megaphones")
                .setDescription("Lanterns amplifying their voice with a hard-light megaphone")
                .build());
        BrightestDay.LOGGER.info("Ring comms registered with Simple Voice Chat");
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null) return;
        amplify(event, sender);
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
