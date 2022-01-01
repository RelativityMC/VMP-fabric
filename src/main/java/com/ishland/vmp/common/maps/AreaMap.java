package com.ishland.vmp.common.maps;

import com.ishland.vmp.common.util.SimpleObjectPool;
import io.papermc.paper.util.MCUtil;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;

import java.util.Collections;
import java.util.Set;

public class AreaMap<T> {

    private final Hash.Strategy<T> hashStrategy = new Hash.Strategy<T>() { // identity hash map strategy
        @Override
        public int hashCode(T o) {
            return System.identityHashCode(o);
        }

        @Override
        public boolean equals(T a, T b) {
            return a == b;
        }
    };

    private final SimpleObjectPool<ObjectLinkedOpenCustomHashSet<T>> pooledHashSets = new SimpleObjectPool<>(unused -> new ObjectLinkedOpenCustomHashSet<>(hashStrategy), ObjectLinkedOpenCustomHashSet::clear, 8192);
    private final Long2ObjectFunction<ObjectLinkedOpenCustomHashSet<T>> allocHashSet = unused -> pooledHashSets.alloc();
    private final Long2ObjectOpenHashMap<ObjectLinkedOpenCustomHashSet<T>> map = new Long2ObjectOpenHashMap<>();
    private final Object2IntOpenCustomHashMap<T> viewDistances = new Object2IntOpenCustomHashMap<>(hashStrategy);
    private final Object2LongOpenCustomHashMap<T> lastCenters = new Object2LongOpenCustomHashMap<>(hashStrategy);

    public Set<T> getObjectsInRange(long coordinateKey) {
        final ObjectLinkedOpenCustomHashSet<T> set = map.get(coordinateKey);
        return set != null ? set : Collections.emptySet();
    }

    public void add(T object, int x, int z, int rawViewDistance) {
        int viewDistance = rawViewDistance - 1;
        viewDistances.put(object, viewDistance);
        lastCenters.put(object, MCUtil.getCoordinateKey(x, z));
        for (int xx = x - viewDistance; xx <= x + viewDistance; xx++) {
            for (int zz = z - viewDistance; zz <= z + viewDistance; zz++) {
                final ObjectLinkedOpenCustomHashSet<T> set = map.computeIfAbsent(MCUtil.getCoordinateKey(xx, zz), allocHashSet);
                set.add(object);
            }
        }

        validate(object, x, z, viewDistance);
    }

    public void remove(T object) {
        if (!viewDistances.containsKey(object)) return;
        final int viewDistance = viewDistances.removeInt(object);
        final long lastCenter = lastCenters.removeLong(object);
        final int x = MCUtil.getCoordinateX(lastCenter);
        final int z = MCUtil.getCoordinateZ(lastCenter);
        for (int xx = x - viewDistance; xx <= x + viewDistance; xx++) {
            for (int zz = z - viewDistance; zz <= z + viewDistance; zz++) {
                final long coordinateKey = MCUtil.getCoordinateKey(xx, zz);
                final ObjectLinkedOpenCustomHashSet<T> set = map.get(coordinateKey);
                if (set == null)
                    throw new IllegalStateException("Expect non-null set in [%d,%d]".formatted(xx, zz));
                if (!set.remove(object))
                    throw new IllegalStateException("Expect %s in %s ([%d,%d])".formatted(object, set, xx, zz));
                if (set.isEmpty()) {
                    map.remove(coordinateKey);
                    pooledHashSets.release(set);
                }
            }
        }
    }

    public void update(T object, int x, int z, int rawViewDistance) {
        if (!viewDistances.containsKey(object))
            throw new IllegalArgumentException("Tried to update %s when not in map".formatted(object));
        final int viewDistance = rawViewDistance - 1;
        final int oldViewDistance = viewDistances.replace(object, viewDistance);
        final long oldCenter = lastCenters.replace(object, MCUtil.getCoordinateKey(x, z));
        final int oldX = MCUtil.getCoordinateX(oldCenter);
        final int oldZ = MCUtil.getCoordinateZ(oldCenter);

        updateAdds(object, oldX, oldZ, oldViewDistance, x, z, viewDistance);
        updateRemovals(object, oldX, oldZ, oldViewDistance, x, z, viewDistance);

        validate(object, x, z, viewDistance);
    }

    private void updateAdds(T object, int oldX, int oldZ, int oldViewDistance, int newX, int newZ, int newViewDistance) {
        int xLower = oldX - oldViewDistance;
        int xHigher = oldX + oldViewDistance;
        int zLower = oldZ - oldViewDistance;
        int zHigher = oldZ + oldViewDistance;

        for (int xx = newX - newViewDistance; xx <= newX + newViewDistance; xx ++) {
            for (int zz = newZ - newViewDistance; zz <= newZ + newViewDistance; zz ++) {
                if (!isInRange(xLower, xHigher, zLower, zHigher, xx, zz)) {
                    final ObjectLinkedOpenCustomHashSet<T> set = map.computeIfAbsent(MCUtil.getCoordinateKey(xx, zz), allocHashSet);
                    set.add(object);
                }
            }
        }
    }

    private void updateRemovals(T object, int oldX, int oldZ, int oldViewDistance, int newX, int newZ, int newViewDistance) {
        int xLower = newX - newViewDistance;
        int xHigher = newX + newViewDistance;
        int zLower = newZ - newViewDistance;
        int zHigher = newZ + newViewDistance;

        for (int xx = oldX - oldViewDistance; xx <= oldX + oldViewDistance; xx ++) {
            for (int zz = oldZ - oldViewDistance; zz <= oldZ + oldViewDistance; zz ++) {
                if (!isInRange(xLower, xHigher, zLower, zHigher, xx, zz)) {
                    final long coordinateKey = MCUtil.getCoordinateKey(xx, zz);
                    final ObjectLinkedOpenCustomHashSet<T> set = map.get(coordinateKey);
                    if (set == null)
                        throw new IllegalStateException("Expect non-null set in [%d,%d]".formatted(xx, zz));
                    if (!set.remove(object))
                        throw new IllegalStateException("Expect %s in %s ([%d,%d])".formatted(object, set, xx, zz));
                    if (set.isEmpty()) {
                        map.remove(coordinateKey);
                        pooledHashSets.release(set);
                    }
                }
            }
        }
    }

    private boolean isInRange(int xLower, int xHigher, int zLower, int zHigher, int x, int z) {
        return x >= xLower && x <= xHigher && z >= zLower && z <= zHigher;
    }

    // only for debugging
    private void validate(T object, int x, int z, int viewDistance) {
        for (int xx = x - viewDistance; xx <= x + viewDistance; xx++) {
            for (int zz = z - viewDistance; zz <= z + viewDistance; zz++) {
                final long coordinateKey = MCUtil.getCoordinateKey(xx, zz);
                final ObjectLinkedOpenCustomHashSet<T> set = map.get(coordinateKey);
                if (set == null)
                    throw new IllegalStateException("Expect non-null set in [%d,%d]".formatted(xx, zz));
                if (!set.contains(object))
                    throw new IllegalStateException("Expect %s in %s ([%d,%d])".formatted(object, set, xx, zz));
            }
        }
    }

}
