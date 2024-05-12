package com.ishland.vmp.mixins.access;

import net.minecraft.class_9761;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(class_9761.class)
public interface IAbstractChunkHolder {

    @Invoker("method_60454")
    void invokeUpdateStatuses(ThreadedAnvilChunkStorage chunkStorage);

}
