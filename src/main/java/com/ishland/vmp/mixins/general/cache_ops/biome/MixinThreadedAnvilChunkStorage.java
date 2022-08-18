package com.ishland.vmp.mixins.general.cache_ops.biome;

import com.ishland.vmp.common.general.cache_ops.biome.PreloadingBiome;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ThreadedAnvilChunkStorage.class)
public class MixinThreadedAnvilChunkStorage {

    @Inject(method = "method_31416", at = @At("RETURN"))
    private static void onMakeChunkAccessible(List<Chunk> chunks, CallbackInfoReturnable<WorldChunk> cir) {
        final WorldChunk worldChunk = cir.getReturnValue();
        if (worldChunk != null) {
            ((PreloadingBiome) worldChunk).vmp$tryPreloadBiome(
                    new ChunkRegion(
                            (ServerWorld) worldChunk.getWorld(),
                            chunks,
                            ChunkStatus.FULL,
                            0
                    )
            );
        }
    }

}
