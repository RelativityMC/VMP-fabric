package com.ishland.vmp.mixins.entitytracker;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.world.ServerChunkLoadingManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChunkLoadingManager.class)
public class MixinThreadedAnvilChunkStorage {

    @Mutable
    @Shadow @Final private Int2ObjectMap<ServerChunkLoadingManager.EntityTracker> entityTrackers;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {
        this.entityTrackers = new Int2ObjectLinkedOpenHashMap<>();
    }

}
