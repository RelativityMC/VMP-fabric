/*
 * Licensed under https://github.com/PaperMC/Paper/blob/master/licenses/MIT.md
 */


package com.destroystokyo.paper.util.misc;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Spottedleaf
 */
public final class PlayerAreaMap extends AreaMap<ServerPlayerEntity> {

    public PlayerAreaMap() {
        super();
    }

    public PlayerAreaMap(final PooledLinkedIdentityHashSets<ServerPlayerEntity> pooledHashSets) {
        super(pooledHashSets);
    }

    public PlayerAreaMap(final PooledLinkedIdentityHashSets<ServerPlayerEntity> pooledHashSets, final ChangeCallback<ServerPlayerEntity> addCallback,
                         final ChangeCallback<ServerPlayerEntity> removeCallback) {
        this(pooledHashSets, addCallback, removeCallback, null);
    }

    public PlayerAreaMap(final PooledLinkedIdentityHashSets<ServerPlayerEntity> pooledHashSets, final ChangeCallback<ServerPlayerEntity> addCallback,
                         final ChangeCallback<ServerPlayerEntity> removeCallback, final ChangeSourceCallback<ServerPlayerEntity> changeSourceCallback) {
        super(pooledHashSets, addCallback, removeCallback, changeSourceCallback);
    }

    @Override
    protected PooledLinkedIdentityHashSets.PooledObjectLinkedOpenIdentityHashSet<ServerPlayerEntity> getEmptySetFor(final ServerPlayerEntity player) {
        return ((PlayerCachedSingleHashSetAccessor) player).getCachedSingleHashSet();
    }

    public interface PlayerCachedSingleHashSetAccessor {
        PooledLinkedIdentityHashSets.PooledObjectLinkedOpenIdentityHashSet<ServerPlayerEntity> getCachedSingleHashSet();
    }
}
