package com.ishland.vmp.mixins.playerwatching;

import net.minecraft.server.network.ChunkFilter;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.PlayerChunkWatchingManager;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.Set;

@Mixin(value = ThreadedAnvilChunkStorage.class, priority = 1005)
public class MixinTACSCancelSending {

    @Redirect(method = "updatePosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;sendWatchPackets(Lnet/minecraft/server/network/ServerPlayerEntity;)V"))
    private void beforeWatchPacketsOnMoving(ThreadedAnvilChunkStorage instance, ServerPlayerEntity player) {
        // Stop packet sending, handled by distance map
    }

    @Redirect(method = "setViewDistance", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/PlayerChunkWatchingManager;getPlayersWatchingChunk()Ljava/util/Set;"))
    private Set<ServerPlayerEntity> redirectWatchPacketsOnChangingVD(PlayerChunkWatchingManager instance) {
        return Collections.emptySet(); // Stop packet sending, handled by distance map
    }

    @Redirect(method = "handlePlayerAddedOrRemoved", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;sendWatchPackets(Lnet/minecraft/server/network/ServerPlayerEntity;)V"))
    private void redirectWatchPacketOnPlayerChanges0(ThreadedAnvilChunkStorage instance, ServerPlayerEntity player) {
        // Stop packet sending, handled by distance map
    }

    @Redirect(method = "handlePlayerAddedOrRemoved", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;sendWatchPackets(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ChunkFilter;)V"))
    private void redirectWatchPacketOnPlayerChanges1(ThreadedAnvilChunkStorage instance, ServerPlayerEntity player, ChunkFilter chunkFilter) {
        // Stop packet sending, handled by distance map
    }

}
