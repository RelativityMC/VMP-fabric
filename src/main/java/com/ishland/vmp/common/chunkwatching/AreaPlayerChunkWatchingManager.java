package com.ishland.vmp.common.chunkwatching;

import com.ishland.vmp.common.maps.AreaMap;
import io.papermc.paper.util.MCUtil;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.network.ChunkFilter;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.Set;

public class AreaPlayerChunkWatchingManager {

    public static final int GENERAL_PLAYER_AREA_MAP_DISTANCE = (int) Math.ceil(
            Arrays.stream(SpawnGroup.values())
                    .mapToInt(SpawnGroup::getImmediateDespawnRange)
                    .reduce(0, Math::max) / 16.0
    );

    private final AreaMap<ServerPlayerEntity> playerAreaMap;
    private final AreaMap<ServerPlayerEntity> generalPlayerAreaMap = new AreaMap<>();
    private final Object2LongOpenHashMap<ServerPlayerEntity> positions = new Object2LongOpenHashMap<>();
    private Listener addListener = null;
    private Listener removeListener = null;

    private int watchDistance = 5;

    public AreaPlayerChunkWatchingManager() {
        this(null, null, null);
    }

    public AreaPlayerChunkWatchingManager(Listener addListener, Listener removeListener, ThreadedAnvilChunkStorage tacs) {
        this.addListener = addListener;
        this.removeListener = removeListener;

        this.playerAreaMap = new AreaMap<>(
                (object, x, z) -> {
                    if (this.addListener != null) {
                        this.addListener.accept(object, x, z);
                    }
                },
                (object, x, z) -> {
                    if (this.removeListener != null) {
                        this.removeListener.accept(object, x, z);
                    }
                },
                true);
    }

    public void tick() {
        for (Object2LongMap.Entry<ServerPlayerEntity> entry : this.positions.object2LongEntrySet()) {
            final ServerPlayerEntity player = entry.getKey();
            final PlayerClientVDTracking vdTracking = (PlayerClientVDTracking) player;
            if (vdTracking.isClientViewDistanceChanged()) {
                vdTracking.getClientViewDistance();
                final long pos = entry.getLongValue();
                this.movePlayer(pos, player);
                player.setChunkFilter(ChunkFilter.cylindrical(new ChunkPos(pos), this.getViewDistance(player)));
            }
        }

    }

    public void setWatchDistance(int watchDistance) {
        this.watchDistance = Math.max(2, watchDistance);
        final ObjectIterator<Object2LongMap.Entry<ServerPlayerEntity>> iterator = positions.object2LongEntrySet().fastIterator();
        while (iterator.hasNext()) {
            final Object2LongMap.Entry<ServerPlayerEntity> entry = iterator.next();
//            if (this.isWatchDisabled(entry.getKey())) continue;

            this.playerAreaMap.update(
                    entry.getKey(),
                    MCUtil.getCoordinateX(entry.getLongValue()),
                    MCUtil.getCoordinateZ(entry.getLongValue()),
                    getViewDistance(entry.getKey()));

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

    public Set<ServerPlayerEntity> getPlayersWatchingChunk(long l) {
        return this.playerAreaMap.getObjectsInRange(l);
    }

    public Object[] getPlayersWatchingChunkArray(long coordinateKey) {
        return this.playerAreaMap.getObjectsInRangeArray(coordinateKey);
    }

    public Object[] getPlayersInGeneralAreaMap(long coordinateKey) {
        return this.generalPlayerAreaMap.getObjectsInRangeArray(coordinateKey);
    }

    public void add(ServerPlayerEntity player, long pos) {
//        System.out.println(String.format("addPlayer %s to %s", player, new ChunkPos(pos)));
        final int x = ChunkPos.getPackedX(pos);
        final int z = ChunkPos.getPackedZ(pos);

        this.playerAreaMap.add(player, x, z, getViewDistance(player));
        this.generalPlayerAreaMap.add(player, x, z, GENERAL_PLAYER_AREA_MAP_DISTANCE);

        this.positions.put(player, MCUtil.getCoordinateKey(x, z));
    }

    public void remove(ServerPlayerEntity player) {
//        System.out.println(String.format("removePlayer %s", player));
        this.playerAreaMap.remove(player);
        this.generalPlayerAreaMap.remove(player);

        this.positions.removeLong(player);
    }

    public void movePlayer(long currentPos, ServerPlayerEntity player) {
//        System.out.println(String.format("movePlayer %s to %s", player, new ChunkPos(currentPos)));
//        if (!this.isWatchDisabled(player))
        final int x = ChunkPos.getPackedX(currentPos);
        final int z = ChunkPos.getPackedZ(currentPos);

        this.playerAreaMap.update(player, x, z, getViewDistance(player));
        this.generalPlayerAreaMap.update(player, x, z, GENERAL_PLAYER_AREA_MAP_DISTANCE);

        this.positions.put(player, MCUtil.getCoordinateKey(x, z));
    }

    private int getViewDistance(ServerPlayerEntity player) {
        return MathHelper.clamp(player.getViewDistance().orElse(2), 2, this.watchDistance);
    }

    public interface Listener {
        void accept(ServerPlayerEntity player, int chunkX, int chunkZ);
    }
}
