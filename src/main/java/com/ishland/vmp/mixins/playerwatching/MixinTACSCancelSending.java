package com.ishland.vmp.mixins.playerwatching;

import net.minecraft.server.network.ChunkFilter;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.PlayerChunkWatchingManager;
import net.minecraft.server.world.ServerChunkLoadingManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.Set;

@Mixin(value = ServerChunkLoadingManager.class, priority = 1005)
public class MixinTACSCancelSending {

//    @Redirect(method = "updatePosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkLoadingManager;sendWatchPackets(Lnet/minecraft/server/network/ServerPlayerEntity;)V"))
//    private void beforeWatchPacketsOnMoving(ThreadedAnvilChunkStorage instance, ServerPlayerEntity player) {
//        // Stop packet sending, handled by distance map
//    }

    @Redirect(method = "setViewDistance", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/PlayerChunkWatchingManager;getPlayersWatchingChunk()Ljava/util/Set;"))
    private Set<ServerPlayerEntity> redirectWatchPacketsOnChangingVD(PlayerChunkWatchingManager instance) {
        return Collections.emptySet(); // Stop packet sending, handled by distance map
    }

//    @Redirect(method = "handlePlayerAddedOrRemoved", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkLoadingManager;sendWatchPackets(Lnet/minecraft/server/network/ServerPlayerEntity;)V"))
//    private void redirectWatchPacketOnPlayerChanges0(ThreadedAnvilChunkStorage instance, ServerPlayerEntity player) {
//        // Stop packet sending, handled by distance map
//    }

    @Redirect(method = "handlePlayerAddedOrRemoved", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;setChunkFilter(Lnet/minecraft/server/network/ChunkFilter;)V"))
    private void redirectChunkFilterSet(ServerPlayerEntity instance, ChunkFilter chunkFilter) {
        // Stop packet sending, handled by distance map
    }

//    @Redirect(method = "handlePlayerAddedOrRemoved", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkLoadingManager;sendWatchPackets(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ChunkFilter;)V"))
//    private void redirectWatchPacketOnPlayerChanges1(ThreadedAnvilChunkStorage instance, ServerPlayerEntity player, ChunkFilter chunkFilter) {
//        // Stop packet sending, handled by distance map
//    }

    /**
     * @author ishland
     * @reason Stop packet sending, handled by distance map
     */
    @Overwrite
    private void sendWatchPackets(ServerPlayerEntity player) {
        // no-op
    }

    /**
     * @author ishland
     * @reason Stop packet sending, handled by distance map
     */
    @Overwrite
    private void sendWatchPackets(ServerPlayerEntity player, ChunkFilter chunkFilter) {
        // no-op
    }

}
