package com.ishland.vmp.mixins.playerwatching;

import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

@Mixin(value = ThreadedAnvilChunkStorage.class, priority = 1005)
public class MixinTACSCancelSending {

    @Inject(method = "updatePosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/ChunkSectionPos;getSectionX()I", shift = At.Shift.BEFORE), cancellable = true, require = 0)
    private void beforeWatchPacketsOnMoving(CallbackInfo ci) {
        ci.cancel(); // Stop packet sending, handled by distance map
    }

    @Redirect(method = "setViewDistance", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;getPlayersWatchingChunk(Lnet/minecraft/util/math/ChunkPos;Z)Ljava/util/List;"))
    private List<ServerPlayerEntity> redirectWatchPacketsOnChangingVD(ThreadedAnvilChunkStorage instance, ChunkPos chunkPos, boolean onlyOnWatchDistanceEdge) {
        return Collections.emptyList(); // Stop packet sending, handled by distance map
    }

    @Redirect(method = "handlePlayerAddedOrRemoved", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;sendWatchPackets(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/math/ChunkPos;Lorg/apache/commons/lang3/mutable/MutableObject;ZZ)V"), require = 0)
    private void redirectWatchPacketOnPlayerChanges(ThreadedAnvilChunkStorage instance, ServerPlayerEntity player, ChunkPos pos, MutableObject<ChunkDataS2CPacket> mutableObject, boolean oldWithinViewDistance, boolean newWithinViewDistance) {
        // Stop packet sending, handled by distance map
    }

}
