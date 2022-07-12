package com.ishland.vmp.common.chunksending;

import com.google.common.util.concurrent.RateLimiter;
import com.ishland.vmp.common.chunkwatching.PlayerClientVDTracking;
import com.ishland.vmp.common.config.Config;
import com.ishland.vmp.common.maps.AreaMap;
import io.papermc.paper.util.MCUtil;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PlayerChunkSendingSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger("PlayerChunkSendingSystem");

    public static final boolean ENABLED;

    //    private static final int MAX_CONCURRENT_SENDS = 4;

    static {
        if (FabricLoader.getInstance().isModLoaded("c2me")) {
            final VersionPredicate predicate;
            try {
                predicate = VersionPredicate.parse(">=0.2.0+alpha.5.11");
            } catch (VersionParsingException e) {
                throw new RuntimeException(e);
            }
            if (predicate.test(FabricLoader.getInstance().getModContainer("c2me").get().getMetadata().getVersion())) {
                boolean isEnabled = true;
                if (FabricLoader.getInstance().isModLoaded("c2me-notickvd")) {
                    try {
                        final Class<?> entryPoint = Class.forName("com.ishland.c2me.notickvd.ModuleEntryPoint");
                        final Field enabledField = entryPoint.getDeclaredField("enabled");
                        enabledField.setAccessible(true);
                        isEnabled = (boolean) enabledField.get(null);
                    } catch (Throwable t) {
                    }
                } else {
                    isEnabled = false;
                }
                ENABLED = isEnabled;
            } else {
                LOGGER.warn("An old version of C2ME is detected, disabling chunk sender system rewrite");
                ENABLED = false;
            }
        } else {
            ENABLED = true;
        }
    }

    private final Reference2ReferenceLinkedOpenHashMap<ServerPlayerEntity, PlayerState> players = new Reference2ReferenceLinkedOpenHashMap<>();
    private final AreaMap<ServerPlayerEntity> areaMap = new AreaMap<>(
            (player, x, z) -> {
                final PlayerState state = players.computeIfAbsent(player, PlayerState::new);
                state.sendQueue.add(new ChunkPos(x, z));
            },
            (player, x, z) -> {
                final PlayerState state = players.get(player);
                if (state != null) {
                    state.unloadChunk(x, z);
                }
            },
            false
    );
    private final ChunkSendingHandle chunkSendingHandle;
    private final ChunkUnloadingHandle chunkUnloadingHandle;

    private final Object2LongOpenHashMap<ServerPlayerEntity> positions = new Object2LongOpenHashMap<>();

    private int watchDistance = 5;

    public PlayerChunkSendingSystem(ChunkSendingHandle chunkSendingHandle, ChunkUnloadingHandle chunkUnloadingHandle) {
        this.chunkSendingHandle = chunkSendingHandle;
        this.chunkUnloadingHandle = chunkUnloadingHandle;
    }

    public void tick() {
        for (PlayerState state : this.players.values()) {
            state.tick();
        }
    }

    public void onChunkLoaded(long pos) {
        for (Object _player : this.areaMap.getObjectsInRangeArray(pos)) {
            if (_player instanceof ServerPlayerEntity player) {
                final PlayerState state = this.players.get(player);
                state.sendQueue.add(new ChunkPos(pos));
            }
        }
    }

    public void setWatchDistance(int watchDistance) {
        this.watchDistance = Math.max(3, watchDistance);
        for (Object2LongMap.Entry<ServerPlayerEntity> entry : this.positions.object2LongEntrySet()) {
            this.areaMap.update(
                    entry.getKey(),
                    MCUtil.getCoordinateX(entry.getLongValue()),
                    MCUtil.getCoordinateZ(entry.getLongValue()),
                    getActualWatchDistance(entry.getKey())
            );
        }
    }

    public void add(ServerPlayerEntity player, int x, int z) {
        this.areaMap.add(player, x, z, getActualWatchDistance(player));
        this.positions.put(player, MCUtil.getCoordinateKey(x, z));
    }

    public void remove(ServerPlayerEntity player) {
        this.areaMap.remove(player);
        this.players.remove(player);
        this.positions.removeLong(player);
    }

    public void movePlayer(ServerPlayerEntity player, long currentPos) {
        final int x = ChunkPos.getPackedX(currentPos);
        final int z = ChunkPos.getPackedZ(currentPos);

        this.areaMap.update(player, x, z, getActualWatchDistance(player));
        this.positions.put(player, MCUtil.getCoordinateKey(x, z));

        final PlayerState state = this.players.get(player);
        if (state != null) state.updateQueue();
    }

    private int getActualWatchDistance(ServerPlayerEntity player) {
        return (player instanceof PlayerClientVDTracking tracking && tracking.getClientViewDistance() != -1)
                ? Math.min(tracking.getClientViewDistance() + 1, this.watchDistance)
                : this.watchDistance;
    }

    @SuppressWarnings("UnstableApiUsage")
    private class PlayerState {

        private final ObjectArrayList<ChunkPos> tmp = new ObjectArrayList<>();

        private final PriorityBlockingQueue<ChunkPos> sendQueue = new PriorityBlockingQueue<>(441, this::compare);
        private final LongOpenHashSet sentChunks = new LongOpenHashSet();
        //        private final Semaphore semaphore = new Semaphore(MAX_CONCURRENT_SENDS);
        private final RateLimiter rateLimiter =
                Config.TARGET_CHUNK_SEND_RATE > 0 ?
                        RateLimiter.create(Config.TARGET_CHUNK_SEND_RATE, 1, TimeUnit.SECONDS)
                        : null;

        private final ServerPlayerEntity player;
        private ChunkPos center;

        public PlayerState(ServerPlayerEntity player) {
            this.player = player;
            this.center = player.getChunkPos();
        }

        public void tick() {
            if (this.player instanceof PlayerClientVDTracking tracking) {
                if (tracking.isClientViewDistanceChanged() && PlayerChunkSendingSystem.this.positions.containsKey(this.player)) {
                    PlayerChunkSendingSystem.this.movePlayer(this.player, PlayerChunkSendingSystem.this.positions.getLong(this.player));
                }
            }
            ChunkPos pos;
            while ((pos = sendQueue.peek()) != null) {
                if (rateLimiter == null || rateLimiter.tryAcquire()) {
                    chunkSendingHandle.sendChunk(this.player, pos);
                    sendQueue.poll();
                } else {
                    break;
                }
            }
        }

        public void unloadChunk(int x, int z) {
            final long coordinateKey = MCUtil.getCoordinateKey(x, z);
            final ChunkPos pos = new ChunkPos(x, z);
            sendQueue.remove(pos);
            chunkUnloadingHandle.unloadChunk(this.player, pos);
        }

        public void updateQueue() {
            this.tmp.clear();
            this.sendQueue.drainTo(this.tmp);
            this.center = this.player.getChunkPos();
            this.sendQueue.addAll(tmp);
            this.tmp.clear();
        }

        private int compare(final ChunkPos a, final ChunkPos b) {
            return Integer.compare(chebyshevDistance(a), chebyshevDistance(b));
        }

        private int chebyshevDistance(ChunkPos pos) {
            return Math.max(Math.abs(pos.x - center.x), Math.abs(pos.z - center.z));
        }

    }

    public interface ChunkSendingHandle {
        void sendChunk(ServerPlayerEntity player, ChunkPos pos);
    }

    public interface ChunkUnloadingHandle {
        void unloadChunk(ServerPlayerEntity player, ChunkPos pos);
    }

}
