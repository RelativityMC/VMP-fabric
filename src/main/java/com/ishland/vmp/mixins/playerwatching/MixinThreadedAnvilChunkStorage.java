package com.ishland.vmp.mixins.playerwatching;

import com.google.common.collect.ImmutableList;
import com.ishland.vmp.common.chunkwatching.AreaPlayerChunkWatchingManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.PlayerChunkWatchingManager;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
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

    @Shadow @Final private ThreadedAnvilChunkStorage.TicketManager ticketManager;

    @Shadow protected abstract boolean canTickChunk(ServerPlayerEntity player, ChunkPos pos);

    @Shadow
    private static boolean method_39976(int i, int j, int k, int l, int m) {
        throw new AbstractMethodError();
    }

    @Shadow
    public static boolean method_39975(int i, int j, int k, int l, int m) {
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

//    /**
//     * @author ishland
//     * @reason use array for iteration
//     */
//    @Overwrite
//    public List<ServerPlayerEntity> getPlayersWatchingChunk(ChunkPos chunkPos, boolean onlyOnWatchDistanceEdge) {
//        Set<ServerPlayerEntity> set = ((AreaPlayerChunkWatchingManager) this.playerChunkWatchingManager).getPlayersWatchingChunk(chunkPos.toLong());
//        ImmutableList.Builder<ServerPlayerEntity> builder = ImmutableList.builder();
//
//        for (Object __player : set) {
//            if (__player instanceof ServerPlayerEntity serverPlayerEntity) {
//                ChunkSectionPos chunkSectionPos = serverPlayerEntity.getWatchedSection();
//                if (onlyOnWatchDistanceEdge && method_39976(chunkPos.x, chunkPos.z, chunkSectionPos.getSectionX(), chunkSectionPos.getSectionZ(), this.watchDistance) || !onlyOnWatchDistanceEdge && method_39975(chunkPos.x, chunkPos.z, chunkSectionPos.getSectionX(), chunkSectionPos.getSectionZ(), this.watchDistance)) {
//                    builder.add(serverPlayerEntity);
//                }
//            }
//        }
//
//        return builder.build();
//    }

//    /**
//     * @author ishland
//     * @reason use array for iteration
//     */
//    @Overwrite
//    public List<ServerPlayerEntity> getPlayersWatchingChunk(ChunkPos pos) {
//        long l = pos.toLong();
//        if (!this.ticketManager.shouldTick(l)) {
//            return List.of();
//        } else {
//            ImmutableList.Builder<ServerPlayerEntity> builder = ImmutableList.builder();
//
//            for (Object __player : ((AreaPlayerChunkWatchingManager) this.playerChunkWatchingManager).getPlayersWatchingChunkArray(l)) {
//                if (__player instanceof ServerPlayerEntity serverPlayerEntity) {
//                    if (this.canTickChunk(serverPlayerEntity, pos)) {
//                        builder.add(serverPlayerEntity);
//                    }
//                }
//            }
//
//            return builder.build();
//        }
//    }

//    /**
//     * @author ishland
//     * @reason use array for iteration
//     */
//    @Overwrite
//    public boolean shouldTick(ChunkPos pos) {
//        long l = pos.toLong();
//        if (!this.ticketManager.shouldTick(l)) {
//            return false;
//        } else {
//            for (Object __player : ((AreaPlayerChunkWatchingManager) this.playerChunkWatchingManager).getPlayersWatchingChunkArray(l)) {
//                if (__player instanceof ServerPlayerEntity serverPlayerEntity) {
//                    if (this.canTickChunk(serverPlayerEntity, pos)) {
//                        return true;
//                    }
//                }
//            }
//            return false;
//        }
//    }

}
