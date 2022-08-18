package com.ishland.vmp.common.general.cache_ops.biome;

import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.biome.Biome;

public interface PreloadingBiome {

    void vmp$tryPreloadBiome(ChunkRegion chunkRegion);

    RegistryEntry<Biome> vmp$getBiomeCached(int x, int y, int z);

}
