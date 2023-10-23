package com.ishland.vmp.mixins.playerwatching;

import net.minecraft.server.network.ChunkFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ChunkFilter.class)
public interface MixinChunkFilter {

    /**
     * @author ishland
     * @reason use chebyshev distance
     */
    @Overwrite
    static boolean isWithinDistance(int centerX, int centerZ, int viewDistance, int x, int z, boolean includeEdge) {
        int actualViewDistance = viewDistance + (includeEdge ? 1 : 0);
        int xDistance = Math.abs(centerX - x);
        int zDistance = Math.abs(centerZ - z);
        return xDistance <= actualViewDistance && zDistance <= actualViewDistance;
    }

}
