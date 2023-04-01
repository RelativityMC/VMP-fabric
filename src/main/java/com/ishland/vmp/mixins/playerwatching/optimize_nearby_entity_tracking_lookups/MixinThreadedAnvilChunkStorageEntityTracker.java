package com.ishland.vmp.mixins.playerwatching.optimize_nearby_entity_tracking_lookups;

import com.ishland.vmp.common.playerwatching.EntityTrackerEntryExtension;
import com.ishland.vmp.common.playerwatching.EntityTrackerExtension;
import com.ishland.vmp.mixins.access.IEntityTrackerEntry;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.EntityTrackingListener;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;

@Mixin(ThreadedAnvilChunkStorage.EntityTracker.class)
public abstract class MixinThreadedAnvilChunkStorageEntityTracker implements EntityTrackerExtension {

    @Shadow @Final private Entity entity;
    @Shadow @Final private Set<EntityTrackingListener> listeners;

    @Shadow public abstract void updateTrackedStatus(ServerPlayerEntity player);

    @Shadow @Final private EntityTrackerEntry entry;
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
        for (EntityTrackingListener listener : this.listeners.toArray(EntityTrackingListener[]::new)) {
            final ServerPlayerEntity player = listener.getPlayer();
            if (triedPlayers != null) triedPlayers.add(player);
            if (player != null) this.updateTrackedStatus(player);
        }
    }

    @Override
    public void tryTick() {
        if (!this.listeners.isEmpty()) {
            this.entry.tick();
        } else {
            final List<Entity> currentPassegers = this.entity.getPassengerList();
            if (!((IEntityTrackerEntry) this.entry).getLastPassengers().equals(currentPassegers)) {
                ((IEntityTrackerEntry) this.entry).setLastPassengers(currentPassegers);
            }

            if (this.entity instanceof ServerPlayerEntity player) {
                // for some reasons mojang decides to sync entity data here, so we need to do it manually

                if (this.entity.velocityModified) {
                    player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(this.entity));
                    this.entity.velocityModified = false;
                }

                ((EntityTrackerEntryExtension) this.entry).vmp$syncEntityData();
            }
        }
    }

    @Inject(method = "updateTrackedStatus(Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z", shift = At.Shift.BEFORE))
    private void beforeStartTracking(ServerPlayerEntity player, CallbackInfo ci) {
        if (this.listeners.isEmpty()) {
            ((EntityTrackerEntryExtension) this.entry).vmp$tickAlways();
        }
    }

}
