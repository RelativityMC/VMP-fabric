package com.ishland.vmp.mixins.chunkloading;

import com.ishland.vmp.common.chunkloading.IPOIAsyncPreload;
import com.ishland.vmp.common.chunkloading.async_chunks_on_player_login.AsyncChunkLoadUtil;
import com.ishland.vmp.mixins.access.IPointOfInterestSet;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.poi.PointOfInterestSet;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.storage.SerializingRegionBasedStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Mixin(PointOfInterestStorage.class)
public abstract class MixinPointOfInterestStorage extends SerializingRegionBasedStorage<PointOfInterestSet> implements IPOIAsyncPreload {

    @Shadow @Final private LongSet preloadedChunks;

    public MixinPointOfInterestStorage(Path path, Function<Runnable, Codec<PointOfInterestSet>> codecFactory, Function<Runnable, PointOfInterestSet> factory, DataFixer dataFixer, DataFixTypes dataFixTypes, boolean dsync, HeightLimitView world) {
        super(path, codecFactory, factory, dataFixer, dataFixTypes, dsync, world);
    }

    @Override
    public CompletableFuture<Void> preloadChunksAtAsync(ServerWorld world, BlockPos pos, int radius) {
        if (!world.getServer().isOnThread()) {
            return CompletableFuture
                    .supplyAsync(() -> preloadChunksAtAsync(world, pos, radius), world.getServer())
                    .thenCompose(Function.identity());
        }
        final CompletableFuture[] futures = ChunkSectionPos.stream(new ChunkPos(pos), Math.floorDiv(radius, 16), this.world.getBottomSectionCoord(), this.world.getTopSectionCoord())
                .map(sectionPos -> Pair.of(sectionPos, this.get(sectionPos.asLong())))
                .filter(pair -> !(pair.getSecond()).map(pointOfInterestSet -> ((IPointOfInterestSet) pointOfInterestSet).invokeIsValid()).orElse(false))
                .map(pair -> pair.getFirst().toChunkPos())
                .filter(chunkPos -> !this.preloadedChunks.contains(chunkPos.toLong()))
                .map(chunkPos ->
                        AsyncChunkLoadUtil.scheduleChunkLoadToStatus(world, chunkPos, ChunkStatus.EMPTY)
                                .whenCompleteAsync((either, unused1) -> either.ifLeft(chunk -> this.preloadedChunks.add(chunk.getPos().toLong())), world.getServer())
                )
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }
}
