package com.ishland.vmp.mixins.chunksending;

import com.ishland.vmp.common.chunkwatching.AreaPlayerChunkWatchingManager;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.PlayerChunkWatchingManager;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ThreadedAnvilChunkStorage.class, priority = 990)
public class MixinThreadedAnvilChunkStorage {

    @Shadow @Final private PlayerChunkWatchingManager playerChunkWatchingManager;

    /**
     * @author ishland
     * @reason take over chunk sending
     */
    @Overwrite
    private void method_17243(MutableObject<ChunkDataS2CPacket> mutableObject, WorldChunk worldChunk, ServerPlayerEntity player) {
        if (this.playerChunkWatchingManager instanceof AreaPlayerChunkWatchingManager manager) {
            manager.onChunkLoaded(worldChunk.getPos().toLong());
        } else {
            throw new IllegalArgumentException("Not an instance of AreaPlayerChunkWatchingManager");
        }
    }

}
