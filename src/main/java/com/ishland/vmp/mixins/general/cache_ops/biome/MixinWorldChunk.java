package com.ishland.vmp.mixins.general.cache_ops.biome;

import com.ishland.vmp.common.general.cache_ops.biome.PreloadingBiome;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.BlendingData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.CompletableFuture;

@Mixin(WorldChunk.class)
public abstract class MixinWorldChunk extends Chunk implements PreloadingBiome {

    public MixinWorldChunk(ChunkPos pos, UpgradeData upgradeData, HeightLimitView heightLimitView, Registry<Biome> biome, long inhabitedTime, @Nullable ChunkSection[] sectionArrayInitializer, @Nullable BlendingData blendingData) {
        super(pos, upgradeData, heightLimitView, biome, inhabitedTime, sectionArrayInitializer, blendingData);
    }

    private static int vmp$getBiomeIndex(int x, int z) {
        return (z & 0b1111) << 4 | (x & 0b1111);
    }

    @Unique
    private CompletableFuture<Void> vmp$preloadBiomeFuture = null;
    @Unique
    private RegistryEntry<Biome>[][] vmp$preloadedBiome = null; // y, xz(indexed)

    @Override
    public void vmp$tryPreloadBiome(ChunkRegion chunkRegion) {
        synchronized (this) {
            if (vmp$preloadBiomeFuture == null) {
                vmp$preloadBiomeFuture = CompletableFuture.runAsync(() -> {
                    RegistryEntry<Biome>[][] preloadedBiome = new RegistryEntry[this.getHeight()][16 * 16];
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            for (int y = 0; y < this.getHeight(); y++) {
                                preloadedBiome[y][vmp$getBiomeIndex(x, z)]
                                        = chunkRegion.getBiome(new BlockPos(this.pos.getStartX() + x, this.getBottomY() + y, this.pos.getStartZ() + z));
                            }
                        }
                    }
                    this.vmp$preloadedBiome = preloadedBiome;
                });
            }
        }
    }

    @Override
    public RegistryEntry<Biome> vmp$getBiomeCached(int x, int y, int z) {
        if (vmp$preloadedBiome == null) {
            return null;
        }
        final int yIndex = MathHelper.clamp(y + getBottomY(), 0, this.getHeight() - 1);
        return vmp$preloadedBiome[yIndex][vmp$getBiomeIndex(x, z)];
    }
}
