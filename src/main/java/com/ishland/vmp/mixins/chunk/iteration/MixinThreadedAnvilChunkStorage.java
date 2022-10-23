package com.ishland.vmp.mixins.chunk.iteration;

import com.ishland.vmp.common.chunk.iteration.ITickableChunkSource;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThreadedAnvilChunkStorage.class)
public class MixinThreadedAnvilChunkStorage implements ITickableChunkSource {

    @Shadow @Final private Long2ObjectLinkedOpenHashMap<ChunkHolder> currentChunkHolders;

    @Unique
    private Long2ObjectLinkedOpenHashMap<ChunkHolder> vmp$tickingChunks = new Long2ObjectLinkedOpenHashMap<>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.vmp$tickingChunks = new Long2ObjectLinkedOpenHashMap<>();
    }

    @Inject(method = "onChunkStatusChange", at = @At("HEAD"))
    private void listenChunkStatusChange(ChunkPos chunkPos, ChunkHolder.LevelType levelType, CallbackInfo ci) {
        final ChunkHolder chunkHolder = this.currentChunkHolders.get(chunkPos.toLong());
        if (chunkHolder == null) return;
        if (chunkHolder.getLevelType().isAfter(ChunkHolder.LevelType.TICKING)) {
            this.vmp$tickingChunks.put(chunkPos.toLong(), chunkHolder);
        } else {
            this.vmp$tickingChunks.remove(chunkPos.toLong());
        }
    }

    @Override
    public Iterable<ChunkHolder> vmp$tickableChunksIterator() {
        return this.vmp$tickingChunks.values();
    }
}
