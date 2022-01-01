package com.ishland.vmp.mixins.playerwatching;

import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ThreadedAnvilChunkStorage.class, priority = 1005)
public class MixinTACSCancelSending {

    @Inject(method = "updatePosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/ChunkSectionPos;getSectionX()I", shift = At.Shift.BEFORE), cancellable = true, require = 0)
    private void beforeWatchPackets(CallbackInfo ci) {
        ci.cancel(); // Stop packet sending, handled by distance map
    }

    @Dynamic("Compatibility hack for krypton")
    @Inject(method = "sendChunks(Lnet/minecraft/util/math/ChunkSectionPos;Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void preventExtraSendChunks(CallbackInfo ci) {
        ci.cancel();
    }

}
