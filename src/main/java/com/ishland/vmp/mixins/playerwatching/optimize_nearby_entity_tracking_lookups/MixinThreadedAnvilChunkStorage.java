package com.ishland.vmp.mixins.playerwatching.optimize_nearby_entity_tracking_lookups;

import com.ishland.vmp.common.playerwatching.NearbyEntityTracking;
import com.ishland.vmp.mixins.access.IThreadedAnvilChunkStorageEntityTracker;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.PlayerChunkWatchingManager;
import net.minecraft.server.world.ServerChunkLoadingManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(ServerChunkLoadingManager.class)
public class MixinThreadedAnvilChunkStorage {

    @Shadow
    @Final
    private Int2ObjectMap<ServerChunkLoadingManager.EntityTracker> entityTrackers;
    @Shadow @Final private ServerChunkLoadingManager.TicketManager ticketManager;
    @Shadow @Final private PlayerChunkWatchingManager playerChunkWatchingManager;
    @Unique
    private final NearbyEntityTracking nearbyEntityTracking = new NearbyEntityTracking();


    @Redirect(method = "loadEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkLoadingManager$EntityTracker;updateTrackedStatus(Ljava/util/List;)V"))
    private void redirectUpdateOnAddEntity(ServerChunkLoadingManager.EntityTracker instance, List<ServerPlayerEntity> players) {
        this.nearbyEntityTracking.addEntityTracker(instance);
    }

    @Redirect(method = "loadEntity", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;values()Lit/unimi/dsi/fastutil/objects/ObjectCollection;"))
    private <T> ObjectCollection<T> nullifyTrackerListOnAddEntity(Int2ObjectMap<T> instance) {
        if (this.entityTrackers == instance) return Int2ObjectMaps.<T>emptyMap().values();
        else return instance.values();
    }

    @Redirect(method = "unloadEntity", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;values()Lit/unimi/dsi/fastutil/objects/ObjectCollection;"))
    private <T> ObjectCollection<T> nullifyTrackerListOnRemoveEntity(Int2ObjectMap<T> instance) {
        if (this.entityTrackers == instance) return Int2ObjectMaps.<T>emptyMap().values();
        else return instance.values();
    }

    @Redirect(method = "unloadEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkLoadingManager$EntityTracker;stopTracking()V"))
    private void redirectUpdateOnRemoveEntity(ServerChunkLoadingManager.EntityTracker instance) {
        this.nearbyEntityTracking.removeEntityTracker(instance);
        instance.stopTracking();
    }

    @Redirect(method = "updatePosition", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;values()Lit/unimi/dsi/fastutil/objects/ObjectCollection;"))
    private <T> ObjectCollection<T> redirectTrackersOnUpdatePosition(Int2ObjectMap<T> instance, ServerPlayerEntity player) {
        if (this.entityTrackers != instance) {
            return instance.values();
        } else {
            return Int2ObjectMaps.<T>emptyMap().values(); // nullify, already handled in tick call
        }
    }

    /**
     * @author ishland
     * @reason use nearby tracker lookup
     */
    @Overwrite
    public void tickEntityMovement() {
//        for(ServerPlayerEntity serverPlayerEntity : this.playerChunkWatchingManager.getPlayersWatchingChunk()) {
////            this.sendWatchPackets(serverPlayerEntity); // done with distance maps
//            serverPlayerEntity.networkHandler.chunkDataSender.sendChunkBatches(serverPlayerEntity);
//        }

        try {
            this.nearbyEntityTracking.tick(this.ticketManager);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
