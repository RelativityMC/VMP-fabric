package com.ishland.vmp.common.chunkwatching;

import com.destroystokyo.paper.util.misc.PlayerAreaMap;
import com.destroystokyo.paper.util.misc.PooledLinkedHashSets;
import io.papermc.paper.util.MCUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.PlayerChunkWatchingManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;

import java.util.function.LongFunction;
import java.util.stream.Stream;

public class AreaPlayerChunkWatchingManager extends PlayerChunkWatchingManager {

    private static final Object[] EMPTY = new Object[0];

    private final Long2ObjectOpenHashMap<ObjectSet<ServerPlayerEntity>> map = new Long2ObjectOpenHashMap<>();
    private final LongFunction<ObjectSet<ServerPlayerEntity>> setSupplier = unused -> new ObjectArraySet<>();
    private final PooledLinkedHashSets<ServerPlayerEntity> pooledLinkedPlayerHashSets = new PooledLinkedHashSets<>();
    private final PlayerAreaMap playerAreaMap = new PlayerAreaMap(
            this.pooledLinkedPlayerHashSets,
            (player, rangeX, rangeZ, currPosX, currPosZ, prevPosX, prevPosZ, newState) -> { // add
                map.computeIfAbsent(ChunkPos.toLong(rangeX, rangeZ), setSupplier).add(player);
            },
            (player, rangeX, rangeZ, currPosX, currPosZ, prevPosX, prevPosZ, newState) -> { // remove
                final long pos = ChunkPos.toLong(rangeX, rangeZ);
                final ObjectSet<ServerPlayerEntity> set = map.computeIfAbsent(pos, setSupplier);
                set.remove(player);
                if (set.isEmpty()) map.remove(pos);
            }
    );
    private final Object2LongOpenHashMap<ServerPlayerEntity> positions = new Object2LongOpenHashMap<>();

    private int watchDistance = 5;

    public void setWatchDistance(int watchDistance) {
        this.watchDistance = MathHelper.clamp(watchDistance, 3, 33);
        final ObjectIterator<Object2LongMap.Entry<ServerPlayerEntity>> iterator = positions.object2LongEntrySet().fastIterator();
        while (iterator.hasNext()) {
            final Object2LongMap.Entry<ServerPlayerEntity> entry = iterator.next();
            if (this.isWatchDisabled(entry.getKey())) continue;
            playerAreaMap.update(
                    entry.getKey(),
                    MCUtil.getCoordinateX(entry.getLongValue()),
                    MCUtil.getCoordinateZ(entry.getLongValue()),
                    this.watchDistance);
        }
    }

    public int getWatchDistance() {
        return watchDistance;
    }

    @Override
    public Stream<ServerPlayerEntity> getPlayersWatchingChunk(long l) {
        final ObjectSet<ServerPlayerEntity> serverPlayerEntities = map.get(l);
        if (serverPlayerEntities == null) return Stream.empty();
        return serverPlayerEntities.stream();
    }

    public Object[] getPlayersWatchingChunkArray(long l) {
        final PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayerEntity> objectsInRange = this.playerAreaMap.getObjectsInRange(l);
        if (objectsInRange != null) return objectsInRange.getBackingSet();
        else return EMPTY;
    }

    public ObjectSet<ServerPlayerEntity> getPlayerWatchingChunkSet(long l) {
        return map.get(l);
    }

    @Override
    public void add(long l, ServerPlayerEntity player, boolean watchDisabled) {
        super.add(l, player, watchDisabled);
        final int x = ChunkPos.getPackedX(l);
        final int z = ChunkPos.getPackedZ(l);
        this.playerAreaMap.add(player, x, z, this.watchDistance);
        this.positions.put(player, MCUtil.getCoordinateKey(x, z));
    }

    @Override
    public void remove(long l, ServerPlayerEntity player) {
        super.remove(l, player);
        this.playerAreaMap.remove(player);
        this.positions.removeLong(player);
    }

    @Override
    public void disableWatch(ServerPlayerEntity player) {
        super.disableWatch(player);
        this.playerAreaMap.remove(player);
    }

    @Override
    public void enableWatch(ServerPlayerEntity player) {
        super.enableWatch(player);
        final long pos = this.positions.getLong(player);
        this.playerAreaMap.add(player, MCUtil.getCoordinateX(pos), MCUtil.getCoordinateZ(pos), this.watchDistance);
    }

    @Override
    public boolean isWatchInactive(ServerPlayerEntity player) {
        return super.isWatchInactive(player);
    }

    @Override
    public boolean isWatchDisabled(ServerPlayerEntity player) {
        return super.isWatchDisabled(player);
    }

    @Override
    public void movePlayer(long prevPos, long currentPos, ServerPlayerEntity player) {
        this.playerAreaMap.update(player, ChunkPos.getPackedX(currentPos), ChunkPos.getPackedZ(currentPos), this.watchDistance);
    }
}
