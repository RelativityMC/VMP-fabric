package com.ishland.leafticket.mixins.ticketsystem.playertracker;

import io.papermc.paper.util.misc.Delayed8WayDistancePropagator2D;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkTicketManager.class)
public abstract class MixinChunkTicketManager {

    @Mutable
    @Shadow
    @Final
    private ChunkTicketManager.NearbyChunkTicketUpdater nearbyChunkTicketUpdater;

    @Shadow
    public abstract <T> void removeTicket(ChunkTicketType<T> type, ChunkPos pos, int radius, T argument);

    @Shadow
    @Final
    private static int NEARBY_PLAYER_TICKET_LEVEL;

    @Shadow
    public abstract <T> void addTicket(ChunkTicketType<T> type, ChunkPos pos, int radius, T argument);

    @Unique
    LongOpenHashSet playerTrackedChunks;

    @Unique
    LongOpenHashSet playerTrackingSourceChunks;

    @Unique
    Long2IntLinkedOpenHashMap playerTrackingUpdates;

    @Unique
    Delayed8WayDistancePropagator2D nearbyPlayerTracker;

    @Unique
    int playerWatchDistance;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.nearbyChunkTicketUpdater = null; // fail-fast incompatibility
        this.playerTrackedChunks = new LongOpenHashSet();
        this.playerTrackingSourceChunks = new LongOpenHashSet();
        this.playerTrackingUpdates = new Long2IntLinkedOpenHashMap();
        this.nearbyPlayerTracker = new Delayed8WayDistancePropagator2D((long coordinate, byte oldLevel, byte newLevel) -> {
            this.playerTrackingUpdates.putAndMoveToLast(coordinate, newLevel);
        });
        this.playerWatchDistance = 0;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ChunkTicketManager$NearbyChunkTicketUpdater;updateLevels()V"))
    private void tickPlayerTracker(ChunkTicketManager.NearbyChunkTicketUpdater __, ThreadedAnvilChunkStorage threadedAnvilChunkStorage) {
        this.nearbyPlayerTracker.propagateUpdates();
        while (!this.playerTrackingUpdates.isEmpty()) {
            long key = this.playerTrackingUpdates.firstLongKey();
            int newLevel = this.playerTrackingUpdates.removeFirstInt();
            final ChunkPos pos = new ChunkPos(key);
            if (newLevel == 0) {
                if (this.playerTrackedChunks.remove(key))
                    this.removeTicket(ChunkTicketType.PLAYER, pos, NEARBY_PLAYER_TICKET_LEVEL, pos);
            } else {
                if (this.playerTrackedChunks.add(key))
                    this.addTicket(ChunkTicketType.PLAYER, pos, NEARBY_PLAYER_TICKET_LEVEL, pos);
            }
        }
    }

    @Redirect(method = "setWatchDistance", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ChunkTicketManager$NearbyChunkTicketUpdater;setWatchDistance(I)V"))
    private void redirectSetWatchDistance(ChunkTicketManager.NearbyChunkTicketUpdater __, int watchDistance) {
        this.playerWatchDistance = watchDistance;
        final LongIterator iterator = this.playerTrackingSourceChunks.iterator();
        while (iterator.hasNext()) {
            final long pos = iterator.nextLong();
            this.nearbyPlayerTracker.setSource(pos, this.playerWatchDistance);
        }
    }

    @Redirect(method = "handleChunkEnter", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ChunkTicketManager$NearbyChunkTicketUpdater;updateLevel(JIZ)V"))
    private void startTracking(ChunkTicketManager.NearbyChunkTicketUpdater __, long pos, int i, boolean b) {
        this.nearbyPlayerTracker.setSource(pos, this.playerWatchDistance);
    }

    @Redirect(method = "handleChunkLeave", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ChunkTicketManager$NearbyChunkTicketUpdater;updateLevel(JIZ)V"))
    private void stopTracking(ChunkTicketManager.NearbyChunkTicketUpdater __, long pos, int i, boolean b) {
        this.nearbyPlayerTracker.removeSource(pos);
    }
}
