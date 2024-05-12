package com.ishland.vmp.mixins.access;

import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.world.chunk.AbstractChunkHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractChunkHolder.class)
public interface IAbstractChunkHolder {

    @Invoker
    void invokeUpdateStatus(ServerChunkLoadingManager chunkStorage);

}
