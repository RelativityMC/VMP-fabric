package com.ishland.vmp.mixins.access;

import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.PlayerChunkWatchingManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.thread.ThreadExecutor;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ThreadedAnvilChunkStorage.class)
public interface IThreadedAnvilChunkStorage {

    @Invoker
    ChunkHolder invokeGetCurrentChunkHolder(long pos);

    @Invoker
    ChunkHolder invokeGetChunkHolder(long pos);

    @Accessor
    ServerWorld getWorld();

    @Accessor
    PlayerChunkWatchingManager getPlayerChunkWatchingManager();

    @Accessor
    ThreadExecutor<Runnable> getMainThreadExecutor();

}
