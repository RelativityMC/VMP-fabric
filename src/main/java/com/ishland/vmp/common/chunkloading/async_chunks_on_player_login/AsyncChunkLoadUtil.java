package com.ishland.vmp.common.chunkloading.async_chunks_on_player_login;

import com.google.common.base.Preconditions;
import com.ishland.vmp.mixins.access.IServerChunkManager;
import com.ishland.vmp.mixins.access.IThreadedAnvilChunkStorage;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Unit;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class AsyncChunkLoadUtil {

    private static final ChunkTicketType<Unit> ASYNC_CHUNK_LOAD = ChunkTicketType.create("vmp_async_chunk_load", (unit, unit2) -> 0);

    public static CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> scheduleChunkLoad(ServerWorld world, ChunkPos pos) {
        return scheduleChunkLoadWithRadius(world, pos, 3);
    }

    public static CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> scheduleChunkLoadWithRadius(ServerWorld world, ChunkPos pos, int radius) {
        return scheduleChunkLoadWithLevel(world, pos, 33 - radius);
    }

    public static CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> scheduleChunkLoadToStatus(ServerWorld world, ChunkPos pos, ChunkStatus status) {
        return scheduleChunkLoadWithLevel(world, pos, 33 + ChunkStatus.getDistanceFromFull(status));
    }

    public static CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> scheduleChunkLoadWithLevel(ServerWorld world, ChunkPos pos, int level) {
        if (!world.getServer().isOnThread()) {
            return CompletableFuture
                    .supplyAsync(() -> scheduleChunkLoadWithLevel(world, pos, level), world.getServer())
                    .thenCompose(Function.identity());
        }
        final ServerChunkManager chunkManager = world.getChunkManager();
        final ChunkTicketManager ticketManager = ((IServerChunkManager) chunkManager).getTicketManager();
        ticketManager.addTicketWithLevel(ASYNC_CHUNK_LOAD, pos, level, Unit.INSTANCE);
        ((IServerChunkManager) chunkManager).invokeTick();
        final ChunkHolder chunkHolder = ((IThreadedAnvilChunkStorage) chunkManager.threadedAnvilChunkStorage).invokeGetCurrentChunkHolder(pos.toLong());
        if (chunkHolder == null) {
            throw new IllegalStateException("Chunk not there when requested");
        }
        final ChunkHolder.LevelType levelType = ChunkHolder.getLevelType(level);

        final CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> future = switch (levelType) {
            case INACCESSIBLE -> chunkHolder.getChunkAt(ChunkHolder.getTargetStatusForLevel(level), world.getChunkManager().threadedAnvilChunkStorage);
            case BORDER -> chunkHolder.getAccessibleFuture().thenApply(either -> either.mapLeft(Function.identity()));
            case TICKING -> chunkHolder.getTickingFuture().thenApply(either -> either.mapLeft(Function.identity()));
            case ENTITY_TICKING -> chunkHolder.getEntityTickingFuture().thenApply(either -> either.mapLeft(Function.identity()));
        };
        future.whenCompleteAsync((unused, throwable) -> {
            if (throwable != null) throwable.printStackTrace();
            ticketManager.removeTicketWithLevel(ASYNC_CHUNK_LOAD, pos, level, Unit.INSTANCE);
        }, world.getServer());
        return future;
    }

    private static final ThreadLocal<Boolean> isRespawnChunkLoadFinished = ThreadLocal.withInitial(() -> false);

    public static void setIsRespawnChunkLoadFinished(boolean value) {
        isRespawnChunkLoadFinished.set(value);
    }

    public static boolean isRespawnChunkLoadFinished() {
        return isRespawnChunkLoadFinished.get();
    }

}
