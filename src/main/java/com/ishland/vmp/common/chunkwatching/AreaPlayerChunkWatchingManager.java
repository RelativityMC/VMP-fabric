package com.ishland.vmp.common.chunkwatching;

import com.ishland.vmp.common.maps.AreaMap;
import io.papermc.paper.util.MCUtil;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.PlayerChunkWatchingManager;
import net.minecraft.util.math.ChunkPos;

import java.util.Arrays;
import java.util.Set;

public class AreaPlayerChunkWatchingManager extends PlayerChunkWatchingManager {

    public static final int GENERAL_PLAYER_AREA_MAP_DISTANCE = (int) Math.ceil(
            Arrays.stream(SpawnGroup.values())
                    .mapToInt(SpawnGroup::getImmediateDespawnRange)
                    .reduce(0, Math::max) / 16.0
    );

    private final AreaMap<ServerPlayerEntity> playerAreaMap = new AreaMap<>(
            (object, x, z) -> {
                if (this.addListener != null) {
                    this.addListener.accept(object, x, z);
                }
            },
            (object, x, z) -> {
                if (this.removeListener != null) {
                    this.removeListener.accept(object, x, z);
                }
            }
    );
    private final AreaMap<ServerPlayerEntity> generalPlayerAreaMap = new AreaMap<>();

    private final Object2LongOpenHashMap<ServerPlayerEntity> positions = new Object2LongOpenHashMap<>();
    private Listener addListener = null;
    private Listener removeListener = null;

    private int watchDistance = 5;

    public AreaPlayerChunkWatchingManager() {
        this(null, null);
    }

    public AreaPlayerChunkWatchingManager(Listener addListener, Listener removeListener) {
        this.addListener = addListener;
        this.removeListener = removeListener;
    }

    public void setWatchDistance(int watchDistance) {
        this.watchDistance = Math.max(3, watchDistance);
        final ObjectIterator<Object2LongMap.Entry<ServerPlayerEntity>> iterator = positions.object2LongEntrySet().fastIterator();
        while (iterator.hasNext()) {
            final Object2LongMap.Entry<ServerPlayerEntity> entry = iterator.next();
            if (this.isWatchDisabled(entry.getKey())) continue;

            this.playerAreaMap.update(
                    entry.getKey(),
                    MCUtil.getCoordinateX(entry.getLongValue()),
                    MCUtil.getCoordinateZ(entry.getLongValue()),
                    this.watchDistance);

            this.generalPlayerAreaMap.update(
                    entry.getKey(),
                    MCUtil.getCoordinateX(entry.getLongValue()),
                    MCUtil.getCoordinateZ(entry.getLongValue()),
                    GENERAL_PLAYER_AREA_MAP_DISTANCE);
        }
    }

    public int getWatchDistance() {
        return watchDistance;
    }

    @Override
    public Set<ServerPlayerEntity> getPlayersWatchingChunk(long l) {
        return this.playerAreaMap.getObjectsInRange(l);
    }

    public Object[] getPlayersWatchingChunkArray(long coordinateKey) {
        return this.playerAreaMap.getObjectsInRangeArray(coordinateKey);
    }

    public Object[] getPlayersInGeneralAreaMap(long coordinateKey) {
        return this.generalPlayerAreaMap.getObjectsInRangeArray(coordinateKey);
    }

    @Override
    public void add(long l, ServerPlayerEntity player, boolean watchDisabled) {
//        System.out.println(String.format("addPlayer %s to %s", player, new ChunkPos(l)));
        super.add(l, player, watchDisabled);
        final int x = ChunkPos.getPackedX(l);
        final int z = ChunkPos.getPackedZ(l);

        this.playerAreaMap.add(player, x, z, this.watchDistance);
        this.generalPlayerAreaMap.add(player, x, z, GENERAL_PLAYER_AREA_MAP_DISTANCE);

        this.positions.put(player, MCUtil.getCoordinateKey(x, z));
    }

    @Override
    public void remove(long l, ServerPlayerEntity player) {
//        System.out.println(String.format("removePlayer %s", player));
        super.remove(l, player);

        this.playerAreaMap.remove(player);
        this.generalPlayerAreaMap.remove(player);

        this.positions.removeLong(player);
    }

    @Override
    public void disableWatch(ServerPlayerEntity player) {
        super.disableWatch(player);
//        this.playerAreaMap.remove(player);
    }

    @Override
    public void enableWatch(ServerPlayerEntity player) {
        super.enableWatch(player);
//        final long pos = this.positions.getLong(player);
//        this.playerAreaMap.add(player, MCUtil.getCoordinateX(pos), MCUtil.getCoordinateZ(pos), this.watchDistance);
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
//        System.out.println(String.format("movePlayer %s to %s", player, new ChunkPos(currentPos)));
//        if (!this.isWatchDisabled(player))
        final int x = ChunkPos.getPackedX(currentPos);
        final int z = ChunkPos.getPackedZ(currentPos);

        this.playerAreaMap.update(player, x, z, this.watchDistance);
        this.generalPlayerAreaMap.update(player, x, z, GENERAL_PLAYER_AREA_MAP_DISTANCE);

        this.positions.put(player, MCUtil.getCoordinateKey(x, z));
    }

    public interface Listener {
        void accept(ServerPlayerEntity player, int chunkX, int chunkZ);
    }
}
