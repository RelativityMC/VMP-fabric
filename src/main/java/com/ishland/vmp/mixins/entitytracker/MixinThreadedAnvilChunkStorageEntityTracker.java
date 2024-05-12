package com.ishland.vmp.mixins.entitytracker;

import com.ishland.vmp.mixins.access.IThreadedAnvilChunkStorage;
import net.minecraft.server.world.ServerChunkLoadingManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerChunkLoadingManager.EntityTracker.class)
public abstract class MixinThreadedAnvilChunkStorageEntityTracker {

    @Shadow @Final private ServerChunkLoadingManager field_18245;

    @Shadow protected abstract int getMaxTrackDistance();

    @Unique
    private int lastDistanceUpdate = 0;

    @Unique
    private int cachedMaxDistance = 0;

    @Redirect(method = "updateTrackedStatus(Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkLoadingManager$EntityTracker;getMaxTrackDistance()I"))
    private int redirectGetMaxTrackDistance(ServerChunkLoadingManager.EntityTracker instance) {
        final int ticks = ((IThreadedAnvilChunkStorage) field_18245).getWorld().getServer().getTicks();
        if (lastDistanceUpdate != ticks || cachedMaxDistance == 0) {
            final int maxTrackDistance = this.getMaxTrackDistance();
            this.cachedMaxDistance = maxTrackDistance;
            this.lastDistanceUpdate = ticks;
            return maxTrackDistance;
        }
        return this.cachedMaxDistance;
    }

}
