package com.ishland.vmp.mixins.access;

import net.minecraft.server.world.ServerChunkLoadingManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerChunkLoadingManager.TicketManager.class)
public interface IThreadedAnvilChunkStorageTicketManager {

    @Accessor
    ServerChunkLoadingManager getField_17443();

}
