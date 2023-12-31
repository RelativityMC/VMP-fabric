package com.ishland.vmp.mixins.access;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkSectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ThreadedAnvilChunkStorage.EntityTracker.class)
public interface IThreadedAnvilChunkStorageEntityTracker {

    @Accessor
    Entity getEntity();

    @Accessor
    ChunkSectionPos getTrackedSection();

    @Accessor
    void setTrackedSection(ChunkSectionPos trackedSection);

    @Invoker
    int invokeGetMaxTrackDistance();

}
