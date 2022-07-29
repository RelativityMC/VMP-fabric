package com.ishland.vmp.common.playerwatching;

import com.ishland.vmp.common.maps.AreaMap;
import com.ishland.vmp.common.util.SimpleObjectPool;
import com.ishland.vmp.mixins.access.IThreadedAnvilChunkStorageEntityTracker;
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectFunction;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;

public class NearbyEntityTracking {

    private final SimpleObjectPool<ReferenceLinkedOpenHashSet<?>> pooledHashSets =
            new SimpleObjectPool<>(unused -> new ReferenceLinkedOpenHashSet<>(),
                    ReferenceLinkedOpenHashSet::clear,
                    ts -> {
                        ts.clear();
                        ts.trim(4);
                    },
                    8192
            );
    private final Long2ObjectFunction<ReferenceLinkedOpenHashSet<ServerPlayerEntity>> allocHashSetPlayer = unused -> (ReferenceLinkedOpenHashSet<ServerPlayerEntity>) pooledHashSets.alloc();
    private final Object2ObjectFunction<ThreadedAnvilChunkStorage.EntityTracker, ReferenceLinkedOpenHashSet<ServerPlayerEntity>> allocHashSetPlayer1 = unused -> (ReferenceLinkedOpenHashSet<ServerPlayerEntity>) pooledHashSets.alloc();

    private final AreaMap<ThreadedAnvilChunkStorage.EntityTracker> areaMap = new AreaMap<>(
            (object, x, z) -> {
                final ReferenceLinkedOpenHashSet<ServerPlayerEntity> players = this.location2Players.get(ChunkPos.toLong(x, z));
                if (players != null) {
                    this.trackerPlayers.computeIfAbsent(object, allocHashSetPlayer1).addAll(players);
                }
            },
            (object, x, z) -> {
                final ReferenceLinkedOpenHashSet<ServerPlayerEntity> players = this.location2Players.get(ChunkPos.toLong(x, z));
                final ReferenceLinkedOpenHashSet<ServerPlayerEntity> set = this.trackerPlayers.get(object);
                if (set != null) {
                    if (players != null) {
                        for (ServerPlayerEntity player : players) {
                            set.remove(player);
                            object.stopTracking(player); // update immediately because
                        }
                    }
                    if (set.isEmpty()) {
                        this.trackerPlayers.remove(object);
                        this.pooledHashSets.release(set);
                    }
                }
            },
            false
    );
    private final ReferenceLinkedOpenHashSet<ThreadedAnvilChunkStorage.EntityTracker> allTrackers = new ReferenceLinkedOpenHashSet<>();
    private final Reference2ReferenceOpenHashMap<ThreadedAnvilChunkStorage.EntityTracker, ReferenceLinkedOpenHashSet<ServerPlayerEntity>> trackerPlayers = new Reference2ReferenceOpenHashMap<>();
    private final Long2ReferenceOpenHashMap<ReferenceLinkedOpenHashSet<ServerPlayerEntity>> location2Players = new Long2ReferenceOpenHashMap<>();
    private final Reference2LongOpenHashMap<ServerPlayerEntity> players2Location = new Reference2LongOpenHashMap<>();

    public void addEntityTracker(ThreadedAnvilChunkStorage.EntityTracker tracker) {
        final ChunkSectionPos trackedSection = ((IThreadedAnvilChunkStorageEntityTracker) tracker).getTrackedSection();
        this.areaMap.add(
                tracker,
                trackedSection.getX(),
                trackedSection.getZ(),
                getChunkViewDistance(tracker)
        );
        this.allTrackers.add(tracker);
    }

    public void removeEntityTracker(ThreadedAnvilChunkStorage.EntityTracker tracker) {
        this.areaMap.remove(tracker);
        this.allTrackers.remove(tracker);
    }

    public void addPlayer(ServerPlayerEntity player) {
        final long location = player.getChunkPos().toLong();

        // update maps
        this.location2Players.computeIfAbsent(location, allocHashSetPlayer).add(player);
        this.players2Location.put(player, location);
        for (ThreadedAnvilChunkStorage.EntityTracker entityTracker : this.areaMap.getObjectsInRange(location)) {
            this.trackerPlayers.computeIfAbsent(entityTracker, allocHashSetPlayer1).add(player);
        }

        // players tracking will be done automatically in next tick call
    }

    public void updatePlayer(ServerPlayerEntity player) {
        this.removePlayerInternal(player, false);
        this.addPlayer(player);
    }

    public void removePlayer(ServerPlayerEntity player) {
        if (!this.players2Location.containsKey(player)) return;
        this.removePlayerInternal(player, true);
    }

    private void removePlayerInternal(ServerPlayerEntity player, boolean doStopTracking) {
        final long location = this.players2Location.getLong(player);

        // first update tracked statuses
        for (ThreadedAnvilChunkStorage.EntityTracker entityTracker : this.areaMap.getObjectsInRange(location)) {
            if (doStopTracking) {
                entityTracker.stopTracking(player);
            }
            final ReferenceLinkedOpenHashSet<ServerPlayerEntity> set = this.trackerPlayers.get(entityTracker);
            if (set != null) {
                set.remove(player);
                if (set.isEmpty()) {
                    this.trackerPlayers.remove(entityTracker);
                    this.pooledHashSets.release(set);
                }
            }
        }

        // then remove player in map
        this.players2Location.removeLong(player);
        final ReferenceLinkedOpenHashSet<ServerPlayerEntity> playersAtLocation =
                this.location2Players.computeIfAbsent(location, allocHashSetPlayer);
        playersAtLocation.remove(player);
        tryReleaseSet(location, playersAtLocation);
    }

    public void tick() {
        for (ThreadedAnvilChunkStorage.EntityTracker entityTracker : this.allTrackers) {
            if (((EntityTrackerExtension) entityTracker).isPositionUpdated()) {
                final long previousChunkPos = ((EntityTrackerExtension) entityTracker).getPreviousChunkPos();
                final ChunkPos currentChunkPos = ((IThreadedAnvilChunkStorageEntityTracker) entityTracker).getEntity().getChunkPos();
                ((EntityTrackerExtension) entityTracker).updatePosition();

                // update AreaMap
                if (previousChunkPos != currentChunkPos.toLong()) {
                    this.areaMap.update(
                            entityTracker,
                            currentChunkPos.x,
                            currentChunkPos.z,
                            getChunkViewDistance(entityTracker)
                    );
                }
            }
            
            final ReferenceLinkedOpenHashSet<ServerPlayerEntity> triedPlayers = (ReferenceLinkedOpenHashSet<ServerPlayerEntity>) this.pooledHashSets.alloc();
            ((EntityTrackerExtension) entityTracker).updateListeners(triedPlayers); // store players to avoid updating again
            final ReferenceLinkedOpenHashSet<ServerPlayerEntity> set = this.trackerPlayers.get(entityTracker);
            if (set != null) {
                for (ServerPlayerEntity player : set) {
                    if (!triedPlayers.contains(player)) {
                        entityTracker.updateTrackedStatus(player);
                    }
                }
            }
            this.pooledHashSets.release(triedPlayers);

            ((EntityTrackerExtension) entityTracker).tryTick();
        }
    }

    private void tryReleaseSet(long location, ReferenceLinkedOpenHashSet<?> playersAtLocation) {
        if (playersAtLocation.isEmpty()) {
            this.location2Players.remove(location);
            this.pooledHashSets.release(playersAtLocation);
        }
    }

    private int getChunkViewDistance(ThreadedAnvilChunkStorage.EntityTracker tracker) {
        return (int) Math.ceil(((IThreadedAnvilChunkStorageEntityTracker) tracker).invokeGetMaxTrackDistance() / 16.0);
    }

}
