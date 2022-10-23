package com.ishland.vmp.common.chunk.iteration;

import net.minecraft.server.world.ChunkHolder;

public interface ITickableChunkSource {

    Iterable<ChunkHolder> vmp$tickableChunksIterator();

}
