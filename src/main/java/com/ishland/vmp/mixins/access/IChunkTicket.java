package com.ishland.vmp.mixins.access;

import net.minecraft.server.world.ChunkTicket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkTicket.class)
public interface IChunkTicket {

    @Invoker("isExpired")
    boolean invokeIsExpired1(long currentTick); // compiler doing shit

}
