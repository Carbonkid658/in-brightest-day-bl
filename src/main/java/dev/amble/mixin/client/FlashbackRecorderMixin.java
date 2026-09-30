package dev.amble.mixin.client;

import dev.amble.client.compat.ReplaySnapshot;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Pseudo
@Mixin(targets = "com.moulberry.flashback.record.Recorder")
public abstract class FlashbackRecorderMixin {

    @Inject(method = "writeCustomSnapshot", at = @At("HEAD"), require = 0)
    private void brightestday$snapshotRingState(Consumer<Packet<? super ClientGamePacketListener>> consumer, CallbackInfo ci) {
        ReplaySnapshot.write(payload -> consumer.accept(new ClientboundCustomPayloadPacket(payload)));
    }
}
