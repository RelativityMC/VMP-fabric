package com.ishland.vmp.mixins.access;

import net.minecraft.server.world.ServerChunkLoadingManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerChunkLoadingManager.LevelManager.class)
public interface IThreadedAnvilChunkStorageLevelManager {

    @Accessor
    ServerChunkLoadingManager getField_17443();

}
