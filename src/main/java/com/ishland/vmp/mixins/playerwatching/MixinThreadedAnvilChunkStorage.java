package com.ishland.vmp.mixins.playerwatching;

import com.google.common.collect.ImmutableList;
import com.ishland.vmp.common.chunkwatching.AreaPlayerChunkWatchingManager;
import com.ishland.vmp.common.config.Config;
import com.ishland.vmp.common.playerwatching.TACSExtension;
import net.minecraft.network.packet.s2c.play.ChunkRenderDistanceCenterS2CPacket;
import net.minecraft.server.network.ChunkFilter;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.PlayerChunkWatchingManager;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class MixinThreadedAnvilChunkStorage implements TACSExtension {

    @Shadow @Final private PlayerChunkWatchingManager playerChunkWatchingManager;

    @Shadow private int watchDistance;

    @Shadow @Final private static Logger LOGGER;

    @Shadow @Final private ThreadedAnvilChunkStorage.TicketManager ticketManager;

    @Shadow protected abstract boolean canTickChunk(ServerPlayerEntity player, ChunkPos pos);

    @Shadow protected abstract void track(ServerPlayerEntity player, ChunkPos pos);

    @Shadow
    protected static void untrack(ServerPlayerEntity player, ChunkPos pos) {
        throw new AbstractMethodError();
    }

    @Shadow protected abstract void updateWatchedSection(ServerPlayerEntity player);

    @Shadow abstract int getViewDistance(ServerPlayerEntity player);

    @Unique
    private AreaPlayerChunkWatchingManager areaPlayerChunkWatchingManager;

    @Override
    public AreaPlayerChunkWatchingManager getAreaPlayerChunkWatchingManager() {
        return this.areaPlayerChunkWatchingManager;
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;setViewDistance(I)V"))
    private void redirectNewPlayerChunkWatchingManager(CallbackInfo ci) {
        this.areaPlayerChunkWatchingManager = new AreaPlayerChunkWatchingManager(
                (player, chunkX, chunkZ) -> this.track(player, new ChunkPos(chunkX, chunkZ)),
                (player, chunkX, chunkZ) -> untrack(player, new ChunkPos(chunkX, chunkZ)),
                (ThreadedAnvilChunkStorage) (Object) this);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTick(CallbackInfo ci) {
        areaPlayerChunkWatchingManager.tick();
    }

    @Inject(method = "setViewDistance", at = @At("RETURN"))
    private void onSetViewDistance(CallbackInfo ci) {
        if (Config.SHOW_CHUNK_TRACKING_MESSAGES) {
            LOGGER.info("Changing watch distance to {}", this.watchDistance);
        }
        areaPlayerChunkWatchingManager.setWatchDistance(this.watchDistance);
    }

    /**
     * @author ishland
     * @reason use array for iteration & use squares as cylinders are expensive
     */
    @Overwrite
    public List<ServerPlayerEntity> getPlayersWatchingChunk(ChunkPos chunkPos, boolean onlyOnWatchDistanceEdge) {
        final AreaPlayerChunkWatchingManager watchingManager = this.areaPlayerChunkWatchingManager;

//        if (!onlyOnWatchDistanceEdge) {
//            // implementation details: this object implements java.util.List
//            // but does not support any of the java.util.List specific methods
//            // this may cause incompatibility for mods that expects a good java.util.List
//            return (List<ServerPlayerEntity>) watchingManager.getPlayersWatchingChunk(chunkPos.toLong());
//        }

        Object[] set = watchingManager.getPlayersWatchingChunkArray(chunkPos.toLong());
        ImmutableList.Builder<ServerPlayerEntity> builder = ImmutableList.builder();

        for (Object __player : set) {
            if (__player instanceof ServerPlayerEntity serverPlayerEntity) {
                ChunkSectionPos watchedPos = serverPlayerEntity.getWatchedSection();
                int chebyshevDistance = Math.max(Math.abs(watchedPos.getSectionX() - chunkPos.x), Math.abs(watchedPos.getSectionZ() - chunkPos.z));
                if (chebyshevDistance > this.watchDistance) {
                    continue;
                }
                if (!serverPlayerEntity.networkHandler.chunkDataSender.isInNextBatch(chunkPos.toLong()) &&
                    (!onlyOnWatchDistanceEdge || chebyshevDistance == this.watchDistance)) {
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

            for (Object __player : this.areaPlayerChunkWatchingManager.getPlayersInGeneralAreaMap(l)) {
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
            for (Object __player : this.areaPlayerChunkWatchingManager.getPlayersInGeneralAreaMap(l)) {
                if (__player instanceof ServerPlayerEntity serverPlayerEntity) {
                    if (this.canTickChunk(serverPlayerEntity, pos)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @Inject(method = "handlePlayerAddedOrRemoved", at = @At("HEAD"))
    private void onHandlePlayerAddedOrRemoved(ServerPlayerEntity player, boolean added, CallbackInfo ci) {
        if (added) {
            this.vmp$updateWatchedSection(player);
            this.areaPlayerChunkWatchingManager.add(player, player.getWatchedSection().toChunkPos().toLong());
        } else {
            this.areaPlayerChunkWatchingManager.remove(player);
        }
    }

    @Inject(method = "updatePosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;updateWatchedSection(Lnet/minecraft/server/network/ServerPlayerEntity;)V"))
    private void onPlayerSectionChange(ServerPlayerEntity player, CallbackInfo ci) {
        this.vmp$updateWatchedSection(player);
        this.areaPlayerChunkWatchingManager.movePlayer(player.getWatchedSection().toChunkPos().toLong(), player);
    }

    @Unique
    private void vmp$updateWatchedSection(ServerPlayerEntity player) {
        this.updateWatchedSection(player);
        player.networkHandler.sendPacket(new ChunkRenderDistanceCenterS2CPacket(player.getWatchedSection().getSectionX(), player.getWatchedSection().getSectionZ()));
        player.setChunkFilter(ChunkFilter.cylindrical(player.getWatchedSection().toChunkPos(), this.getViewDistance(player)));
    }

}
