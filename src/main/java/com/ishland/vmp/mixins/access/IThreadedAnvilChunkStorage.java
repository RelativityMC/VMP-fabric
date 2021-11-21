package com.ishland.vmp.mixins.access;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ThreadedAnvilChunkStorage.class)
public interface IThreadedAnvilChunkStorage {

    @Invoker
    ChunkHolder invokeGetCurrentChunkHolder(long pos);

    @Accessor
    ServerWorld getWorld();

}
