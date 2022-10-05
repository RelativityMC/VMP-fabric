package com.ishland.vmp.common.util;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class SimpleObjectPool<T> {

    private final Function<SimpleObjectPool<T>, T> constructor;
    private final Consumer<T> initializer;
    private final Consumer<T> onRecycle;
    private final int size;

    private Object[] cachedObjects = null;
    private int allocatedCount = 0;

    public SimpleObjectPool(Function<SimpleObjectPool<T>, T> constructor, Consumer<T> initializer, Consumer<T> onRecycle, int size) {
        this.constructor = Objects.requireNonNull(constructor);
        this.initializer = Objects.requireNonNull(initializer);
        this.onRecycle = Objects.requireNonNull(onRecycle);
        Preconditions.checkArgument(size > 0);
        this.size = size;
    }

    private void init() {
        if (this.cachedObjects == null) {
            this.cachedObjects = new Object[this.size];

            for (int i = 0; i < size; i++) {
                final T object = this.constructor.apply(this);
                this.cachedObjects[i] = object;
            }
        }
    }

    public T alloc() {
        final T object;
        synchronized (this) {
            init();
            if (this.allocatedCount >= this.size) { // oversized, falling back to normal alloc
                object = this.constructor.apply(this);
                return object;
            }

            // get an object from the array
            final int ordinal = this.allocatedCount++;
            object = (T) this.cachedObjects[ordinal];
            this.cachedObjects[ordinal] = null;
        }

        this.initializer.accept(object); // initialize the object

        return object;
    }

    public void release(T object) {
        synchronized (this) {
            init();
            if (this.allocatedCount == 0) return; // pool is full

            this.onRecycle.accept(object);
            this.cachedObjects[--this.allocatedCount] = object; // store the object into the pool
        }
    }

    public int getAllocatedCount() {
        return this.allocatedCount;
    }

}
