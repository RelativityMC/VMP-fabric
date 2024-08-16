package com.ishland.vmp.mixins.playerwatching.optimize_nearby_entity_tracking_lookups;

import com.ishland.vmp.common.playerwatching.EntityTrackerEntryExtension;
import com.ishland.vmp.common.playerwatching.EntityTrackerExtension;
import com.ishland.vmp.mixins.access.IEntityTrackerEntry;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.PlayerAssociatedNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;

@Mixin(ServerChunkLoadingManager.EntityTracker.class)
public abstract class MixinThreadedAnvilChunkStorageEntityTracker implements EntityTrackerExtension {

    @Shadow @Final private Entity entity;
    @Shadow @Final private Set<PlayerAssociatedNetworkHandler> listeners;

    @Shadow public abstract void updateTrackedStatus(ServerPlayerEntity player);

    @Shadow @Final private EntityTrackerEntry entry;
    @Shadow private ChunkSectionPos trackedSection;
    @Unique
    private double prevX = Double.NaN;

    @Unique
    private double prevY = Double.NaN;

    @Unique
    private double prevZ = Double.NaN;

    @Override
    public boolean isPositionUpdated() {
        final Vec3d pos = this.entity.getPos();
        return pos.x != this.prevX || pos.y != this.prevY || pos.z != prevZ;
    }

    @Override
    public void updatePosition() {
        final Vec3d pos = this.entity.getPos();
        this.prevX = pos.x;
        this.prevY = pos.y;
        this.prevZ = pos.z;
        this.trackedSection = ChunkSectionPos.from(this.entity);
    }

    @Override
    public Vec3d getPreviousLocation() {
        return new Vec3d(this.prevX, this.prevY, this.prevZ);
    }

    @Override
    public long getPreviousChunkPos() {
        return ChunkPos.toLong(ChunkSectionPos.getSectionCoord((int) this.prevX), ChunkSectionPos.getSectionCoord((int) this.prevX));
    }

    @Override
    public void updateListeners(Set<ServerPlayerEntity> triedPlayers) {
        for (PlayerAssociatedNetworkHandler listener : this.listeners.toArray(PlayerAssociatedNetworkHandler[]::new)) {
            final ServerPlayerEntity player = listener.getPlayer();
            if (triedPlayers != null) triedPlayers.add(player);
            if (player != null) this.updateTrackedStatus(player);
        }
    }

    @Override
    public void tryTick() {
        this.trackedSection = ChunkSectionPos.from(this.entity);
        this.entry.tick();
    }

    @Inject(method = "updateTrackedStatus(Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z", shift = At.Shift.BEFORE))
    private void beforeStartTracking(ServerPlayerEntity player, CallbackInfo ci) {
        if (this.listeners.isEmpty()) {
            ((EntityTrackerEntryExtension) this.entry).vmp$tickAlways();
        }
    }

    @Redirect(method = "updateTrackedStatus(Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkLoadingManager;isTracked(Lnet/minecraft/server/network/ServerPlayerEntity;II)Z"))
    private boolean assumeAlwaysTracked(ServerChunkLoadingManager instance, ServerPlayerEntity player, int chunkX, int chunkZ) {
        return true;
    }

}
