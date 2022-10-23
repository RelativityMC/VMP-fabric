package com.ishland.vmp.common.chunk.loading;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.CompletableFuture;

public interface IPOIAsyncPreload {

    CompletableFuture<Void> preloadChunksAtAsync(ServerWorld world, BlockPos pos, int radius);

}
