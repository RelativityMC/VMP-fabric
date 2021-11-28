package com.ishland.vmp.mixins.playerwatching;

import com.google.common.collect.ImmutableList;
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

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class MixinThreadedAnvilChunkStorage {

    @Shadow @Final private PlayerChunkWatchingManager playerChunkWatchingManager;

    @Shadow private int watchDistance;

    @Shadow @Final private static Logger LOGGER;

    @Shadow
    private static boolean isOnDistanceEdge(ChunkPos chunkPos, ServerPlayerEntity player, boolean usePlayerWatchedSection, int distance) {
        throw new AbstractMethodError();
    }

    @Shadow
    private static boolean isWithinDistance(ChunkPos chunkPos, ServerPlayerEntity player, boolean usePlayerWatchedSection, int distance) {
        throw new AbstractMethodError();
    }

    @Shadow @Final private ThreadedAnvilChunkStorage.TicketManager ticketManager;

    @Shadow protected abstract boolean canTickChunk(ServerPlayerEntity player, ChunkPos pos);

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
     * @reason use array for iteration
     */
    @Overwrite
    public List<ServerPlayerEntity> getPlayersWatchingChunk(ChunkPos chunkPos, boolean onlyOnWatchDistanceEdge) {
        Object[] set = ((AreaPlayerChunkWatchingManager) this.playerChunkWatchingManager).getPlayersWatchingChunkArray(chunkPos.toLong());
        ImmutableList.Builder<ServerPlayerEntity> builder = ImmutableList.builder();

        for (Object __player : set) {
            if (__player instanceof ServerPlayerEntity serverPlayerEntity) {
                if (onlyOnWatchDistanceEdge && isOnDistanceEdge(chunkPos, serverPlayerEntity, true, this.watchDistance) || !onlyOnWatchDistanceEdge && isWithinDistance(chunkPos, serverPlayerEntity, true, this.watchDistance)) {
                    builder.add(serverPlayerEntity);
                }
            }
        }

        return builder.build();
    }

    /**
     * @author ishland
     * @reason use array for iteration
     */
    @Overwrite
    public List<ServerPlayerEntity> getPlayersWatchingChunk(ChunkPos pos) {
        long l = pos.toLong();
        if (!this.ticketManager.shouldTick(l)) {
            return List.of();
        } else {
            ImmutableList.Builder<ServerPlayerEntity> builder = ImmutableList.builder();

            for (Object __player : ((AreaPlayerChunkWatchingManager) this.playerChunkWatchingManager).getPlayersWatchingChunkArray(l)) {
                if (__player instanceof ServerPlayerEntity serverPlayerEntity) {
                    if (this.canTickChunk(serverPlayerEntity, pos)) {
                        builder.add(serverPlayerEntity);
                    }
                }
            }

            return builder.build();
        }
    }

    /**
     * @author ishland
     * @reason use array for iteration
     */
    @Overwrite
    public boolean shouldTick(ChunkPos pos) {
        long l = pos.toLong();
        if (!this.ticketManager.shouldTick(l)) {
            return false;
        } else {
            for (Object __player : ((AreaPlayerChunkWatchingManager) this.playerChunkWatchingManager).getPlayersWatchingChunkArray(l)) {
                if (__player instanceof ServerPlayerEntity serverPlayerEntity) {
                    if (this.canTickChunk(serverPlayerEntity, pos)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

}
