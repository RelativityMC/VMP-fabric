package com.ishland.vmp.mixins.access;

import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ServerChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerChunkManager.class)
public interface IServerChunkManager {

    @Accessor
    ChunkTicketManager getTicketManager();

    @Invoker
    boolean invokeTick();

}
