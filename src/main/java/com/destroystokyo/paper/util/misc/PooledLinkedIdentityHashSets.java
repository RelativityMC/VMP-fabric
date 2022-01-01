/*
 * Licensed under https://github.com/PaperMC/Paper/blob/master/licenses/MIT.md
 */

package com.destroystokyo.paper.util.misc;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

import java.lang.ref.WeakReference;

/** @author Spottedleaf */
public class PooledLinkedIdentityHashSets<E> {

    /* Tested via https://gist.github.com/Spottedleaf/a93bb7a8993d6ce142d3efc5932bf573 */

    // we really want to avoid that equals() check as much as possible...
    protected final Object2ObjectOpenHashMap<PooledObjectLinkedOpenIdentityHashSet<E>, PooledObjectLinkedOpenIdentityHashSet<E>> mapPool = new Object2ObjectOpenHashMap<>(128, 0.25f);

    protected void decrementReferenceCount(final PooledObjectLinkedOpenIdentityHashSet<E> current) {
        if (current.referenceCount == 0) {
            throw new IllegalStateException("Cannot decrement reference count for " + current);
        }
        if (current.referenceCount == -1 || --current.referenceCount > 0) {
            return;
        }

        this.mapPool.remove(current);
        return;
    }

    public PooledObjectLinkedOpenIdentityHashSet<E> findMapWith(final PooledObjectLinkedOpenIdentityHashSet<E> current, final E object) {
        final PooledObjectLinkedOpenIdentityHashSet<E> cached = current.getAddCache(object);

        if (cached != null) {
            decrementReferenceCount(current);

            if (cached.referenceCount == 0) {
                // bring the map back from the dead
                PooledObjectLinkedOpenIdentityHashSet<E> contending = this.mapPool.putIfAbsent(cached, cached);
                if (contending != null) {
                    // a map already exists with the elements we want
                    if (contending.referenceCount != -1) {
                        ++contending.referenceCount;
                    }
                    current.updateAddCache(object, contending);
                    return contending;
                }

                cached.referenceCount = 1;
            } else if (cached.referenceCount != -1) {
                ++cached.referenceCount;
            }

            return cached;
        }

        if (!current.add(object)) {
            return current;
        }

        // we use get/put since we use a different key on put
        PooledObjectLinkedOpenIdentityHashSet<E> ret = this.mapPool.get(current);

        if (ret == null) {
            ret = new PooledObjectLinkedOpenIdentityHashSet<>(current);
            current.remove(object);
            this.mapPool.put(ret, ret);
            ret.referenceCount = 1;
        } else {
            if (ret.referenceCount != -1) {
                ++ret.referenceCount;
            }
            current.remove(object);
        }

        current.updateAddCache(object, ret);

        decrementReferenceCount(current);
        return ret;
    }

    // rets null if current.size() == 1
    public PooledObjectLinkedOpenIdentityHashSet<E> findMapWithout(final PooledObjectLinkedOpenIdentityHashSet<E> current, final E object) {
        if (current.set.size() == 1) {
            decrementReferenceCount(current);
            return null;
        }

        final PooledObjectLinkedOpenIdentityHashSet<E> cached = current.getRemoveCache(object);

        if (cached != null) {
            decrementReferenceCount(current);

            if (cached.referenceCount == 0) {
                // bring the map back from the dead
                PooledObjectLinkedOpenIdentityHashSet<E> contending = this.mapPool.putIfAbsent(cached, cached);
                if (contending != null) {
                    // a map already exists with the elements we want
                    if (contending.referenceCount != -1) {
                        ++contending.referenceCount;
                    }
                    current.updateRemoveCache(object, contending);
                    return contending;
                }

                cached.referenceCount = 1;
            } else if (cached.referenceCount != -1) {
                ++cached.referenceCount;
            }

            return cached;
        }

        if (!current.remove(object)) {
            return current;
        }

        // we use get/put since we use a different key on put
        PooledObjectLinkedOpenIdentityHashSet<E> ret = this.mapPool.get(current);

        if (ret == null) {
            ret = new PooledObjectLinkedOpenIdentityHashSet<>(current);
            current.add(object);
            this.mapPool.put(ret, ret);
            ret.referenceCount = 1;
        } else {
            if (ret.referenceCount != -1) {
                ++ret.referenceCount;
            }
            current.add(object);
        }

        current.updateRemoveCache(object, ret);

        decrementReferenceCount(current);
        return ret;
    }

    static final class RawSetObjectLinkedOpenIdentityHashSet<E> extends ObjectOpenCustomHashSet<E> {

        private static <T> Strategy<T> makeIdentityHashCodeStrategy() {
            return new Strategy<>() {
                @Override
                public int hashCode(T o) {
                    return System.identityHashCode(o);
                }

                @Override
                public boolean equals(T a, T b) {
                    return a == b;
                }
            };
        }

        public RawSetObjectLinkedOpenIdentityHashSet() {
            super(makeIdentityHashCodeStrategy());
        }

        public RawSetObjectLinkedOpenIdentityHashSet(final int capacity) {
            super(capacity, makeIdentityHashCodeStrategy());
        }

        public RawSetObjectLinkedOpenIdentityHashSet(final int capacity, final float loadFactor) {
            super(capacity, loadFactor, makeIdentityHashCodeStrategy());
        }

        @Override
        public RawSetObjectLinkedOpenIdentityHashSet<E> clone() {
            return (RawSetObjectLinkedOpenIdentityHashSet<E>)super.clone();
        }

        public E[] getRawSet() {
            return this.key;
        }
    }

    public static final class PooledObjectLinkedOpenIdentityHashSet<E> {

        private static final WeakReference NULL_REFERENCE = new WeakReference<>(null);

        final RawSetObjectLinkedOpenIdentityHashSet<E> set;
        int referenceCount; // -1 if special
        int hash; // optimize hashcode

        // add cache
        WeakReference<E> lastAddObject = NULL_REFERENCE;
        WeakReference<PooledObjectLinkedOpenIdentityHashSet<E>> lastAddMap = NULL_REFERENCE;

        // remove cache
        WeakReference<E> lastRemoveObject = NULL_REFERENCE;
        WeakReference<PooledObjectLinkedOpenIdentityHashSet<E>> lastRemoveMap = NULL_REFERENCE;

        public PooledObjectLinkedOpenIdentityHashSet(final PooledLinkedIdentityHashSets<E> pooledSets) {
            this.set = new RawSetObjectLinkedOpenIdentityHashSet<>(2, 0.8f);
        }

        public PooledObjectLinkedOpenIdentityHashSet(final E single) {
            this((PooledLinkedIdentityHashSets<E>)null);
            this.referenceCount = -1;
            this.add(single);
        }

        public PooledObjectLinkedOpenIdentityHashSet(final PooledObjectLinkedOpenIdentityHashSet<E> other) {
            this.set = other.set.clone();
            this.hash = other.hash;
        }

        // from https://github.com/Spottedleaf/ConcurrentUtil/blob/master/src/main/java/ca/spottedleaf/concurrentutil/util/IntegerUtil.java
        // generated by https://github.com/skeeto/hash-prospector
        private static int hash0(int x) {
            x *= 0x36935555;
            x ^= x >>> 16;
            return x;
        }

        PooledObjectLinkedOpenIdentityHashSet<E> getAddCache(final E element) {
            final E currentAdd = this.lastAddObject.get();

            if (currentAdd == null || !(currentAdd == element || currentAdd.equals(element))) {
                return null;
            }

            return this.lastAddMap.get();
        }

        PooledObjectLinkedOpenIdentityHashSet<E> getRemoveCache(final E element) {
            final E currentRemove = this.lastRemoveObject.get();

            if (currentRemove == null || !(currentRemove == element || currentRemove.equals(element))) {
                return null;
            }

            return this.lastRemoveMap.get();
        }

        void updateAddCache(final E element, final PooledObjectLinkedOpenIdentityHashSet<E> map) {
            this.lastAddObject = new WeakReference<>(element);
            this.lastAddMap = new WeakReference<>(map);
        }

        void updateRemoveCache(final E element, final PooledObjectLinkedOpenIdentityHashSet<E> map) {
            this.lastRemoveObject = new WeakReference<>(element);
            this.lastRemoveMap = new WeakReference<>(map);
        }

        boolean add(final E element) {
            boolean added =  this.set.add(element);

            if (added) {
                this.hash += hash0(System.identityHashCode(element));
            }

            return added;
        }

        boolean remove(Object element) {
            boolean removed = this.set.remove(element);

            if (removed) {
                this.hash -= hash0(System.identityHashCode(element));
            }

            return removed;
        }

        public boolean contains(final Object element) {
            return this.set.contains(element);
        }

        public E[] getBackingSet() {
            return this.set.getRawSet();
        }

        public int size() {
            return this.set.size();
        }

        @Override
        public int hashCode() {
            return this.hash;
        }

        @Override
        public boolean equals(final Object other) {
            if (!(other instanceof PooledLinkedIdentityHashSets.PooledObjectLinkedOpenIdentityHashSet)) {
                return false;
            }
            if (this.referenceCount == 0) {
                return other == this;
            } else {
                if (other == this) {
                    // Unfortunately we are never equal to our own instance while in use!
                    return false;
                }
                return this.hash == ((PooledObjectLinkedOpenIdentityHashSet)other).hash && this.set.equals(((PooledObjectLinkedOpenIdentityHashSet)other).set);
            }
        }

        @Override
        public String toString() {
            return "PooledHashSet: size: " + this.set.size() + ", reference count: " + this.referenceCount + ", hash: " +
                this.hashCode() + ", identity: " + System.identityHashCode(this) + " map: " + this.set.toString();
        }
    }
}
