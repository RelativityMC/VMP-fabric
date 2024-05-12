package com.ishland.vmp.mixins.playerwatching.optimize_nearby_player_lookups;

import com.ishland.vmp.common.chunkwatching.AreaPlayerChunkWatchingManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.PlayerChunkWatchingManager;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerChunkLoadingManager.class)
public abstract class MixinThreadedAnvilChunkStorage {

    @Shadow @Final private PlayerChunkWatchingManager playerChunkWatchingManager;

    @Shadow @Final private ServerChunkLoadingManager.TicketManager ticketManager;

    @Shadow
    private static double getSquaredDistance(ChunkPos pos, Entity entity) {
        throw new AbstractMethodError();
    }

//    /**
//     * @author ishland
//     * @reason optimize nearby player lookups
//     */
//    @Overwrite
//    public boolean isTooFarFromPlayersToSpawnMobs(ChunkPos chunkPos) {
//        long l = chunkPos.toLong();
//        if (!this.ticketManager.shouldTick(l)) return true;
//        final AreaPlayerChunkWatchingManager chunkWatchingManager = (AreaPlayerChunkWatchingManager) this.playerChunkWatchingManager;
//        final Object[] array = chunkWatchingManager.getPlayersWatchingChunkArray(chunkPos.toLong());
//
//        for (Object __player : array) {
//            if (__player instanceof ServerPlayerEntity player) {
//                if (!player.isSpectator() && getSquaredDistance(chunkPos, player) < 16384.0) return false;
//            }
//        }
//        return true;
//    }

}
