package com.ishland.leafticket.mixins.playerwatching;

import com.ishland.leafticket.common.chunkwatching.AreaPlayerChunkWatchingManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.PlayerChunkWatchingManager;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.stream.Stream;

@Mixin(ThreadedAnvilChunkStorage.class)
public class MixinThreadedAnvilChunkStorage {

    @Shadow @Final private PlayerChunkWatchingManager playerChunkWatchingManager;

    @Shadow private int watchDistance;

    @Shadow @Final private static Logger LOGGER;

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/server/world/PlayerChunkWatchingManager"))
    private PlayerChunkWatchingManager redirectNewPlayerChunkWatchingManager() {
        return new AreaPlayerChunkWatchingManager();
    }

    @Inject(method = "setViewDistance", at = @At("RETURN"))
    private void onSetViewDistance(CallbackInfo ci) {
        if (this.playerChunkWatchingManager instanceof AreaPlayerChunkWatchingManager areaPlayerChunkWatchingManager) {
            LOGGER.info("Changing watch distance to {}", this.watchDistance);
            areaPlayerChunkWatchingManager.setWatchDistance(this.watchDistance);
        } else {
            throw new IllegalArgumentException("Not an instance of AreaPlayerChunkWatchingManager");
        }
    }

    /**
     * @author ishland
     * @reason no more filter (implemented by distance map)
     */
    @Overwrite
    public Stream<ServerPlayerEntity> getPlayersWatchingChunk(ChunkPos chunkPos, boolean onlyOnWatchDistanceEdge) {
        return this.playerChunkWatchingManager.getPlayersWatchingChunk(chunkPos.toLong());
    }

}
