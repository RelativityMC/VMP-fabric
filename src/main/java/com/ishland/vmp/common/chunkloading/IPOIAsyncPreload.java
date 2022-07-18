package com.ishland.vmp.common.chunkloading;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

import java.util.concurrent.CompletableFuture;

public interface IPOIAsyncPreload {

    CompletableFuture<Void> preloadChunksAtAsync(ServerWorld world, BlockPos pos, int radius);

}
