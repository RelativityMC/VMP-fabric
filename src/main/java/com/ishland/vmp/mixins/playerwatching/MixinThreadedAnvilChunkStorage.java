package com.ishland.vmp.mixins.playerwatching;

import com.ishland.vmp.common.chunkwatching.AreaPlayerChunkWatchingManager;
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
public abstract class MixinThreadedAnvilChunkStorage {

    @Shadow @Final private PlayerChunkWatchingManager playerChunkWatchingManager;

    @Shadow private int watchDistance;

    @Shadow @Final private static Logger LOGGER;

    @Shadow
    private static int getChebyshevDistance(ChunkPos pos, ServerPlayerEntity player, boolean useWatchedPosition) {
        throw new AbstractMethodError();
    }

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
        final Stream<ServerPlayerEntity> playersWatchingChunk = this.playerChunkWatchingManager.getPlayersWatchingChunk(chunkPos.toLong());
        if (!onlyOnWatchDistanceEdge)
            return playersWatchingChunk;
        else
            return playersWatchingChunk.filter(player -> getChebyshevDistance(chunkPos, player, true) == this.watchDistance);
    }

}
