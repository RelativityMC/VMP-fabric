package com.ishland.vmp.mixins.access;

import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.ChunkSectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkDeltaUpdateS2CPacket.class)
public interface IChunkDeltaUpdateS2CPacket {

    @Accessor
    ChunkSectionPos getSectionPos();

    @Mutable
    @Accessor
    void setSectionPos(ChunkSectionPos sectionPos);

    @Mutable
    @Accessor
    void setPositions(short[] positions);

    @Mutable
    @Accessor
    void setBlockStates(BlockState[] blockStates);

    @Mutable
    @Accessor
    void setNoLightingUpdates(boolean noLightingUpdates);

}
