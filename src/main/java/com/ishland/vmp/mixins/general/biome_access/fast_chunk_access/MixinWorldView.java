package com.ishland.vmp.mixins.general.biome_access.fast_chunk_access;

import com.ishland.vmp.mixins.access.IThreadedAnvilChunkStorage;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.OptionalChunk;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletableFuture;

@Mixin(WorldView.class)
public interface MixinWorldView {

    @Redirect(method = "getBiomeForNoiseGen", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldView;getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;"))
    private Chunk redirectBiomeChunk(WorldView instance, int x, int z, ChunkStatus chunkStatus, boolean create) {
        if (!create && instance instanceof ServerWorld world) {
            final ChunkHolder holder = ((IThreadedAnvilChunkStorage) world.getChunkManager().threadedAnvilChunkStorage).invokeGetChunkHolder(ChunkPos.toLong(x, z));
            if (holder != null) {
                final CompletableFuture<OptionalChunk<WorldChunk>> future = holder.getAccessibleFuture();
                final OptionalChunk<WorldChunk> either = future.getNow(null);
                if (either != null) {
                    final WorldChunk chunk = either.orElse(null);
                    if (chunk != null) return chunk;
                }
            }
        }
        return instance.getChunk(x, z, chunkStatus, create);
    }

}
