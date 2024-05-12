package com.ishland.vmp.mixins.playerwatching;

import net.minecraft.server.world.ServerChunkLoadingManager;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChunkLoadingManager.class)
public class MixinTACSCancelSendingKrypton {

    @Dynamic("Compatibility hack for krypton")
    @Inject(method = {
            "sendChunks(Lnet/minecraft/util/math/ChunkSectionPos;Lnet/minecraft/server/network/ServerPlayerEntity;)V",
            "sendSpiralChunkWatchPackets(Lnet/minecraft/server/network/ServerPlayerEntity;)V",
            "unloadChunks(Lnet/minecraft/server/network/ServerPlayerEntity;III)V",
            "sendChunkWatchPackets(Lnet/minecraft/util/math/ChunkSectionPos;Lnet/minecraft/server/network/ServerPlayerEntity;)V"
    }, at = @At("HEAD"), cancellable = true, require = 0)
    private void preventExtraSendChunks(CallbackInfo ci) {
        ci.cancel();
    }

}
