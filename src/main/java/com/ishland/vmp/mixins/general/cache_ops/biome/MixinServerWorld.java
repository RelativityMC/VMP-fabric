package com.ishland.vmp.mixins.general.cache_ops.biome;

import com.ishland.vmp.common.general.cache_ops.biome.PreloadingBiome;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ReadOnlyChunk;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld extends World {

    @Shadow public abstract ServerChunkManager getChunkManager();

    protected MixinServerWorld(MutableWorldProperties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> dimension, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) {
        super(properties, registryRef, dimension, profiler, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }

    @Override
    public RegistryEntry<Biome> getBiome(BlockPos pos) {
        Chunk chunk = this.getChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()), ChunkStatus.BIOMES, false);
        if (chunk instanceof ReadOnlyChunk readOnlyChunk)
            chunk = readOnlyChunk.getWrappedChunk();
        if (chunk instanceof PreloadingBiome preloadingBiome) {
            final RegistryEntry<Biome> biome = preloadingBiome.vmp$getBiomeCached(pos.getX(), pos.getY(), pos.getZ());
            if (biome != null) return biome;
        }
        return super.getBiome(pos);
    }
}
