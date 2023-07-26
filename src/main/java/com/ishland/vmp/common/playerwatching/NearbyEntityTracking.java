package com.ishland.vmp.common.playerwatching;

import com.ishland.vmp.common.maps.AreaMap;
import com.ishland.vmp.common.playerwatching.compat.EntityPositionTransformer;
import com.ishland.vmp.common.util.SimpleObjectPool;
import com.ishland.vmp.mixins.access.IThreadedAnvilChunkStorageEntityTracker;
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.objects.Object2ObjectFunction;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.Reference2LongMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

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

    private final AreaMap<ThreadedAnvilChunkStorage.EntityTracker> areaMap = new AreaMap<>(
            (object, x, z) -> {
            },
            (object, x, z) -> {
            },
            false
    );

    private final Reference2ReferenceLinkedOpenHashMap<ServerPlayerEntity, ReferenceLinkedOpenHashSet<ThreadedAnvilChunkStorage.EntityTracker>> playerTrackers = new Reference2ReferenceLinkedOpenHashMap<>();
    private final Reference2LongOpenHashMap<ThreadedAnvilChunkStorage.EntityTracker> tracker2ChunkPos = new Reference2LongOpenHashMap<>();

    public void addEntityTracker(ThreadedAnvilChunkStorage.EntityTracker tracker) {
        final ChunkPos pos = getEntityChunkPos(((IThreadedAnvilChunkStorageEntityTracker) tracker).getEntity());
        this.areaMap.add(
                tracker,
                pos.x,
                pos.z,
                getChunkViewDistance(tracker)
        );
        this.tracker2ChunkPos.put(tracker, pos.toLong());
    }

    public void removeEntityTracker(ThreadedAnvilChunkStorage.EntityTracker tracker) {
        this.areaMap.remove(tracker);
        this.tracker2ChunkPos.removeLong(tracker);
    }

    public void addPlayer(ServerPlayerEntity player) {
        this.playerTrackers.put(player, (ReferenceLinkedOpenHashSet<ThreadedAnvilChunkStorage.EntityTracker>) this.pooledHashSets.alloc());
    }

    public void removePlayer(ServerPlayerEntity player) {
        final ReferenceLinkedOpenHashSet<ThreadedAnvilChunkStorage.EntityTracker> originalTrackers = this.playerTrackers.remove(player);
        if (originalTrackers != null) {
            for (ThreadedAnvilChunkStorage.EntityTracker tracker : originalTrackers) {
                tracker.stopTracking(player);
            }
            this.pooledHashSets.release(originalTrackers);
        }
    }

    private final ReferenceLinkedOpenHashSet<ThreadedAnvilChunkStorage.EntityTracker> trackerTickList = new ReferenceLinkedOpenHashSet<>() {
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

    public void tick(ThreadedAnvilChunkStorage.TicketManager ticketManager) {
        for (Reference2LongMap.Entry<ThreadedAnvilChunkStorage.EntityTracker> entry : this.tracker2ChunkPos.reference2LongEntrySet()) {
            final ChunkPos pos = getEntityChunkPos(((IThreadedAnvilChunkStorageEntityTracker) entry.getKey()).getEntity());
            if (pos.toLong() != entry.getLongValue()) {
                this.areaMap.update(entry.getKey(), pos.x, pos.z, getChunkViewDistance(entry.getKey()));
                entry.setValue(pos.toLong());
            }
        }

        trackerTickList.clear();

        for (var entry : this.playerTrackers.entrySet()) {
            final ServerPlayerEntity player = entry.getKey();
            final Set<ThreadedAnvilChunkStorage.EntityTracker> currentTrackers = this.areaMap.getObjectsInRange(getEntityChunkPos(player).toLong());

            boolean isPlayerPositionUpdated = ((ServerPlayerEntityExtension) player).vmpTracking$isPositionUpdated();
            ((ServerPlayerEntityExtension) player).vmpTracking$updatePosition();

            // update original trackers
            final ReferenceLinkedOpenHashSet<ThreadedAnvilChunkStorage.EntityTracker> trackers = entry.getValue();
            for (ObjectListIterator<ThreadedAnvilChunkStorage.EntityTracker> iterator = trackers.iterator(); iterator.hasNext(); ) {
                ThreadedAnvilChunkStorage.EntityTracker entityTracker = iterator.next();
                if (currentTrackers.contains(entityTracker)) {
                    handleTracker(ticketManager, player, isPlayerPositionUpdated, entityTracker);
                } else {
                    entityTracker.stopTracking(player);
                    iterator.remove();
                }
            }

            // update new trackers
            for (ThreadedAnvilChunkStorage.EntityTracker entityTracker : currentTrackers) {
                if (!trackers.contains(entityTracker)) {
                    handleTracker(ticketManager, player, isPlayerPositionUpdated, entityTracker);
                    trackers.add(entityTracker);
                }
            }
        }
        for (ThreadedAnvilChunkStorage.EntityTracker entityTracker : trackerTickList) {
            ((EntityTrackerExtension) entityTracker).updatePosition();
        }
    }

    private void handleTracker(ThreadedAnvilChunkStorage.TicketManager ticketManager, ServerPlayerEntity player, boolean isPlayerPositionUpdated, ThreadedAnvilChunkStorage.EntityTracker entityTracker) {
        final ChunkSectionPos trackedPos = ((IThreadedAnvilChunkStorageEntityTracker) entityTracker).getTrackedSection();
        if (trackerTickList.add(entityTracker) && ticketManager.shouldTickEntities(ChunkPos.toLong(trackedPos.getSectionX(), trackedPos.getSectionZ()))) {
            tryTickTracker(entityTracker);
        }
        if (isPlayerPositionUpdated || ((EntityTrackerExtension) entityTracker).isPositionUpdated()) {
            tryUpdateTracker(entityTracker, player);
        }
    }

    private static void tryUpdateTracker(ThreadedAnvilChunkStorage.EntityTracker entityTracker, ServerPlayerEntity player) {
        entityTracker.updateTrackedStatus(player);
    }

    private static void tryTickTracker(ThreadedAnvilChunkStorage.EntityTracker entityTracker) {
        ((EntityTrackerExtension) entityTracker).tryTick();
    }

    private int getChunkViewDistance(ThreadedAnvilChunkStorage.EntityTracker tracker) {
        return (int) Math.ceil(((IThreadedAnvilChunkStorageEntityTracker) tracker).invokeGetMaxTrackDistance() / 16.0) + 1;
    }

}
