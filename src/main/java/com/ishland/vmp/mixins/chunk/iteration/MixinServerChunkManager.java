package com.ishland.vmp.mixins.chunk.iteration;

import com.ishland.vmp.common.chunk.iteration.ITickableChunkSource;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerChunkManager.class)
public class MixinServerChunkManager {

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;entryIterator()Ljava/lang/Iterable;"))
    private Iterable<ChunkHolder> redirectVisibleChunks(ServerChunkLoadingManager instance) {
        return ((ITickableChunkSource) instance).vmp$tickableChunksIterator();
    }

}
