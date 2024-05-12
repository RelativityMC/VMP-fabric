package com.ishland.vmp.common.playerwatching;

import com.ishland.vmp.common.config.Config;
import com.ishland.vmp.common.maps.AreaMap;
import com.ishland.vmp.common.playerwatching.compat.EntityPositionTransformer;
import com.ishland.vmp.common.util.SimpleObjectPool;
import com.ishland.vmp.mixins.access.IThreadedAnvilChunkStorageEntityTracker;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.Reference2LongMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class NearbyEntityTracking {

    private static final EntityPositionTransformer[] transformers;

    static {
        List<EntityPositionTransformer> list = new ArrayList<>();
        if (FabricLoader.getInstance().isModLoaded("valkyrienskies")) {
            System.out.println("ValkyrienSkies detected, applying compatibility patch");
            try {
                list.add((EntityPositionTransformer)
                        Class.forName("com.ishland.vmp.common.playerwatching.compat.ValkyrienSkies2ShipPositionTransformer")
                                .getDeclaredConstructor().newInstance());
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        transformers = list.toArray(EntityPositionTransformer[]::new);
    }

    public static void init() {
        // intentionally empty
    }

    private final SimpleObjectPool<ReferenceLinkedOpenHashSet<?>> pooledHashSets =
            new SimpleObjectPool<>(unused -> new ReferenceLinkedOpenHashSet<>(),
                    ReferenceLinkedOpenHashSet::clear,
                    ts -> {
                        ts.clear();
                        ts.trim(4);
                    },
                    8192
            );

    // AreaMap implementation for long-lived entities
    private final AreaMap<ServerChunkLoadingManager.EntityTracker> areaMap = new AreaMap<>();
    private final Reference2ReferenceLinkedOpenHashMap<ServerPlayerEntity, ReferenceLinkedOpenHashSet<ServerChunkLoadingManager.EntityTracker>> playerTrackers = new Reference2ReferenceLinkedOpenHashMap<>();
    private final Reference2LongOpenHashMap<ServerChunkLoadingManager.EntityTracker> tracker2ChunkPos = new Reference2LongOpenHashMap<>();

    // vanilla-like implementation for short-lived entities
    private static final int STAGING_TRACKER_LIFETIME = 200; // 10s
    private final AtomicLong ticks = new AtomicLong(0L);
    private final ObjectLinkedOpenHashSet<StagedTracker> stagingTrackers = new ObjectLinkedOpenHashSet<>();

    private void addEntityTrackerAreaMap(ServerChunkLoadingManager.EntityTracker tracker) {
        // update is done lazily on next tickEntityMovement
        final ChunkPos pos = getEntityChunkPos(((IThreadedAnvilChunkStorageEntityTracker) tracker).getEntity());
        this.areaMap.add(
                tracker,
                pos.x,
                pos.z,
                getChunkViewDistance(tracker)
        );
        this.tracker2ChunkPos.put(tracker, pos.toLong());
    }

    public void addEntityTracker(ServerChunkLoadingManager.EntityTracker tracker) {
        if (Config.OPTIMIZED_ENTITY_TRACKING_USE_STAGING_AREA) {
            stagingTrackers.addAndMoveToLast(new StagedTracker(tracker, ticks.get()));
            for (ServerPlayerEntity player : this.playerTrackers.keySet()) {
                tracker.updateTrackedStatus(player);
            }
        } else {
            this.addEntityTrackerAreaMap(tracker);
        }
    }

    public void removeEntityTracker(ServerChunkLoadingManager.EntityTracker tracker) {
        // remove from staging
        if (this.stagingTrackers.remove(new StagedTracker(tracker, 0L))) { // 0L is a dummy value (not used in equals(..) and hashCode())
            tracker.stopTracking();
        }

        // remove from AreaMap
        this.areaMap.remove(tracker);
        this.tracker2ChunkPos.removeLong(tracker);
    }

    public void addPlayer(ServerPlayerEntity player) {
        this.playerTrackers.put(player, (ReferenceLinkedOpenHashSet<ServerChunkLoadingManager.EntityTracker>) this.pooledHashSets.alloc());
    }

    public void removePlayer(ServerPlayerEntity player) {
        // remove player in staging
        for (StagedTracker stagingTracker : this.stagingTrackers) {
            stagingTracker.tracker().stopTracking(player);
        }

        // remove player in AreaMap
        final ReferenceLinkedOpenHashSet<ServerChunkLoadingManager.EntityTracker> originalTrackers = this.playerTrackers.remove(player);
        if (originalTrackers != null) {
            for (ServerChunkLoadingManager.EntityTracker tracker : originalTrackers) {
                tracker.stopTracking(player);
            }
            this.pooledHashSets.release(originalTrackers);
        }
    }

    private final ReferenceLinkedOpenHashSet<ServerChunkLoadingManager.EntityTracker> trackerTickList = new ReferenceLinkedOpenHashSet<>() {
        @Override
        protected void rehash(int newN) {
            if (this.n < newN) {
                super.rehash(newN);
            }
        }
    };

    private static ChunkPos getEntityChunkPos(Entity entity) {
        Vec3d pos = entity.getPos();
        for (EntityPositionTransformer transformer : transformers) {
            pos = transformer.transform(entity, pos);
        }
        return new ChunkPos(ChunkSectionPos.getSectionCoord(pos.x), ChunkSectionPos.getSectionCoord(pos.z));
    }

    public void tick(ServerChunkLoadingManager.TicketManager ticketManager) {
        tickStaging(ticketManager);

        for (Reference2LongMap.Entry<ServerChunkLoadingManager.EntityTracker> entry : this.tracker2ChunkPos.reference2LongEntrySet()) {
            final ChunkPos pos = getEntityChunkPos(((IThreadedAnvilChunkStorageEntityTracker) entry.getKey()).getEntity());
            if (pos.toLong() != entry.getLongValue()) {
                this.areaMap.update(entry.getKey(), pos.x, pos.z, getChunkViewDistance(entry.getKey()));
                entry.setValue(pos.toLong());
            }
        }

        trackerTickList.clear();

        for (var entry : this.playerTrackers.entrySet()) {
            final ServerPlayerEntity player = entry.getKey();
            final Set<ServerChunkLoadingManager.EntityTracker> currentTrackers = this.areaMap.getObjectsInRange(getEntityChunkPos(player).toLong());

            boolean isPlayerPositionUpdated = ((ServerPlayerEntityExtension) player).vmpTracking$isPositionUpdated();
            ((ServerPlayerEntityExtension) player).vmpTracking$updatePosition();

            // update original trackers
            final ReferenceLinkedOpenHashSet<ServerChunkLoadingManager.EntityTracker> trackers = entry.getValue();
            for (ObjectListIterator<ServerChunkLoadingManager.EntityTracker> iterator = trackers.iterator(); iterator.hasNext(); ) {
                ServerChunkLoadingManager.EntityTracker entityTracker = iterator.next();
                if (currentTrackers.contains(entityTracker)) {
                    handleTracker(ticketManager, player, isPlayerPositionUpdated, entityTracker);
                } else {
                    entityTracker.stopTracking(player);
                    iterator.remove();
                }
            }

            // update new trackers
            for (ServerChunkLoadingManager.EntityTracker entityTracker : currentTrackers) {
                if (!trackers.contains(entityTracker)) {
                    handleTracker(ticketManager, player, isPlayerPositionUpdated, entityTracker);
                    trackers.add(entityTracker);
                }
            }
        }
        for (ServerChunkLoadingManager.EntityTracker entityTracker : trackerTickList) {
            ((EntityTrackerExtension) entityTracker).updatePosition();
        }
    }

    private void tickStaging(ServerChunkLoadingManager.TicketManager ticketManager) {
        // migrate staging trackers to AreaMap
        final long currentTicks = this.ticks.incrementAndGet();
        for (ObjectListIterator<StagedTracker> iterator = this.stagingTrackers.iterator(); iterator.hasNext(); ) {
            StagedTracker stagingTracker = iterator.next();
            if (currentTicks - stagingTracker.tickAdded() >= STAGING_TRACKER_LIFETIME) {
                iterator.remove();
//                System.out.println(String.format("Migrating staging tracker %s", ((IThreadedAnvilChunkStorageEntityTracker) stagingTracker.tracker()).getEntity()));
                addEntityTrackerAreaMap(stagingTracker.tracker());
            } else {
                break;
            }
        }

        final List<ServerPlayerEntity> players = List.copyOf(this.playerTrackers.keySet());
        for(StagedTracker staged : this.stagingTrackers) {
            final ServerChunkLoadingManager.EntityTracker entityTracker = staged.tracker();
            ChunkSectionPos chunkSectionPos = ((IThreadedAnvilChunkStorageEntityTracker) entityTracker).getTrackedSection();
            final Entity entity = ((IThreadedAnvilChunkStorageEntityTracker) entityTracker).getEntity();
            ChunkSectionPos chunkSectionPos2 = ChunkSectionPos.from(entity);
            boolean bl = !Objects.equals(chunkSectionPos, chunkSectionPos2);
            entityTracker.updateTrackedStatus(players);
            if (bl) {
                ((IThreadedAnvilChunkStorageEntityTracker) entityTracker).setTrackedSection(chunkSectionPos2);
            }

            if (bl || ticketManager.shouldTickEntities(chunkSectionPos2.toChunkPos().toLong())) {
                ((EntityTrackerExtension) entityTracker).tryTick();
            }
        }
    }

    private void handleTracker(ServerChunkLoadingManager.TicketManager ticketManager, ServerPlayerEntity player, boolean isPlayerPositionUpdated, ServerChunkLoadingManager.EntityTracker entityTracker) {
        final ChunkSectionPos trackedPos = ((IThreadedAnvilChunkStorageEntityTracker) entityTracker).getTrackedSection();
        if (trackerTickList.add(entityTracker) && ticketManager.shouldTickEntities(ChunkPos.toLong(trackedPos.getSectionX(), trackedPos.getSectionZ()))) {
            tryTickTracker(entityTracker);
        }
        if (isPlayerPositionUpdated || ((EntityTrackerExtension) entityTracker).isPositionUpdated()) {
            tryUpdateTracker(entityTracker, player);
        }
    }

    private static void tryUpdateTracker(ServerChunkLoadingManager.EntityTracker entityTracker, ServerPlayerEntity player) {
        entityTracker.updateTrackedStatus(player);
    }

    private static void tryTickTracker(ServerChunkLoadingManager.EntityTracker entityTracker) {
        ((EntityTrackerExtension) entityTracker).tryTick();
    }

    private int getChunkViewDistance(ServerChunkLoadingManager.EntityTracker tracker) {
        return (int) Math.ceil(((IThreadedAnvilChunkStorageEntityTracker) tracker).invokeGetMaxTrackDistance() / 16.0) + 1;
    }

    private record StagedTracker(ServerChunkLoadingManager.EntityTracker tracker, long tickAdded) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StagedTracker that = (StagedTracker) o;
            return tracker == that.tracker;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(tracker);
        }
    }

}
