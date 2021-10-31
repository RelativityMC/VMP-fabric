package com.ishland.leafticket.mixins.access;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.Executor;

@Mixin(ChunkHolder.class)
public interface IChunkHolder {

    @Accessor
    int getLastTickLevel();

    @Invoker
    void invokeTick(ThreadedAnvilChunkStorage chunkStorage, Executor executor);

}
