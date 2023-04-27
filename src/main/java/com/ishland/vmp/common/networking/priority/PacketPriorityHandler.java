package com.ishland.vmp.common.networking.priority;

import com.google.common.base.Preconditions;
import com.ishland.raknetify.fabric.common.connection.RakNetMultiChannel;
import com.ishland.vmp.common.config.Config;
import com.ishland.vmp.common.util.SimpleObjectPool;
import com.ishland.vmp.mixins.access.IChunkDeltaUpdateS2CPacket;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;
import it.unimi.dsi.fastutil.ints.Int2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectFunction;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.shorts.Short2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import net.minecraft.block.BlockState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.JigsawGeneratingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkLoadDistanceS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkRenderDistanceCenterS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.SignEditorOpenS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.PriorityQueue;

public class PacketPriorityHandler extends ChannelDuplexHandler {

    private static final int IP_TOS_LOWDELAY = 0b00010000;
    private static final int IP_TOS_THROUGHPUT = 0b00001000;
    private static final int IP_TOS_RELIABILITY = 0b00000100;

    private static final int DEFAULT_SNDBUF = 8 * 1024;

    public static void setupPacketPriority(Channel channel) {
        if (Config.USE_PACKET_PRIORITY_SYSTEM) {
            if (channel instanceof SocketChannel) {
                channel.pipeline().addLast("vmp_packet_priority", new PacketPriorityHandler());
                channel.config().setOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(4 * 1024, 8 * 1024));
                channel.config().setOption(ChannelOption.IP_TOS, IP_TOS_LOWDELAY | IP_TOS_THROUGHPUT); // reduce latency
            }
        }
    }

    public static final Object SYNC_REQUEST_OBJECT = new Object();
    public static final Object SYNC_REQUEST_OBJECT_DIM_CHANGE = new Object();
    public static final Object START_PRIORITY = new Object();

    private class ChunkUpdateQueue {
        private static final Object2ObjectOpenHashMap<Class<?>, Object2ObjectFunction<Object, ChunkPos>> chunkUpdatePackets = new Object2ObjectOpenHashMap<>();
        private static final Object2ObjectOpenHashMap<Class<?>, Object2ObjectFunction<Object, BlockPos>> packet2BlockPos = new Object2ObjectOpenHashMap<>();

        static {
            chunkUpdatePackets.put(JigsawGeneratingC2SPacket.class, packet -> new ChunkPos(((JigsawGeneratingC2SPacket) packet).getPos()));
            chunkUpdatePackets.put(SignEditorOpenS2CPacket.class, packet -> new ChunkPos(((SignEditorOpenS2CPacket) packet).getPos()));
            chunkUpdatePackets.put(UpdateSignC2SPacket.class, packet -> new ChunkPos(((UpdateSignC2SPacket) packet).getPos()));
            chunkUpdatePackets.put(BlockEventS2CPacket.class, packet -> new ChunkPos(((BlockEventS2CPacket) packet).getPos()));
            chunkUpdatePackets.put(BlockUpdateS2CPacket.class, packet -> new ChunkPos(((BlockUpdateS2CPacket) packet).getPos()));
            chunkUpdatePackets.put(ChunkDeltaUpdateS2CPacket.class, packet -> ((IChunkDeltaUpdateS2CPacket) packet).getSectionPos().toChunkPos());
            chunkUpdatePackets.put(WorldEventS2CPacket.class, packet -> new ChunkPos(((WorldEventS2CPacket) packet).getPos()));
            chunkUpdatePackets.put(BlockBreakingProgressS2CPacket.class, packet -> new ChunkPos(((BlockBreakingProgressS2CPacket) packet).getPos()));
            chunkUpdatePackets.put(BlockEntityUpdateS2CPacket.class, packet -> new ChunkPos(((BlockEntityUpdateS2CPacket) packet).getPos()));
            chunkUpdatePackets.put(LightUpdateS2CPacket.class, packet -> new ChunkPos(((LightUpdateS2CPacket) packet).getChunkX(), ((LightUpdateS2CPacket) packet).getChunkZ()));

            packet2BlockPos.put(JigsawGeneratingC2SPacket.class, packet -> ((JigsawGeneratingC2SPacket) packet).getPos());
            packet2BlockPos.put(SignEditorOpenS2CPacket.class, packet -> ((SignEditorOpenS2CPacket) packet).getPos());
            packet2BlockPos.put(UpdateSignC2SPacket.class, packet -> ((UpdateSignC2SPacket) packet).getPos());
            packet2BlockPos.put(BlockEventS2CPacket.class, packet -> ((BlockEventS2CPacket) packet).getPos());
            packet2BlockPos.put(WorldEventS2CPacket.class, packet -> ((WorldEventS2CPacket) packet).getPos());
            packet2BlockPos.put(BlockBreakingProgressS2CPacket.class, packet -> ((BlockBreakingProgressS2CPacket) packet).getPos());
            packet2BlockPos.put(BlockEntityUpdateS2CPacket.class, packet -> ((BlockEntityUpdateS2CPacket) packet).getPos());
        }

        private static ChunkPos identifyChunkUpdatePacket(Object msg) {
            final Object2ObjectFunction<Object, ChunkPos> function = chunkUpdatePackets.get(msg.getClass());
            return function != null ? function.get(msg) : null;
        }

        private record SimplePendingPacket(Object packet, ChannelPromise promise) {
        }

        private final Int2ReferenceLinkedOpenHashMap<PacketPriorityHandler.SectionUpdateQueue> sectionQueues = new Int2ReferenceLinkedOpenHashMap<>();
        private final Object2ObjectLinkedOpenHashMap<Class<?>, SimplePendingPacket> sequencedQueuedPackets = new Object2ObjectLinkedOpenHashMap<>();
        private final ObjectArrayList<SimplePendingPacket> queuedPackets = new ObjectArrayList<>();

        private ChunkPos chunkPos;

        public ChunkUpdateQueue submitChunk(ChunkPos chunkPos) {
            Preconditions.checkState(this.chunkPos == null);
            this.chunkPos = chunkPos;
            return this;
        }

        public void consumePacket(Object msg, ChannelPromise promise) {
            if (msg instanceof ChunkDeltaUpdateS2CPacket packet) {
                packet.visitUpdates(this::handleBlockUpdate);
            } else if (msg instanceof BlockUpdateS2CPacket packet) {
                this.handleBlockUpdate(packet.getPos(), packet.getState());
            } else if (msg instanceof LightUpdateS2CPacket packet) {
                // TODO handle light update packets
//                sequencedQueuedPackets.put(LightUpdateS2CPacket.class, pendingPacket);
            } else if (packet2BlockPos.containsKey(msg.getClass())) {
                final BlockPos pos = packet2BlockPos.get(msg.getClass()).apply(msg);

                this.sectionQueues
                        .computeIfAbsent(ChunkSectionPos.getSectionCoord(pos.getY()), unused -> sectionUpdateQueueSimpleObjectPool.alloc())
                        .perBlockSequencedQueuedPackets
                        .computeIfAbsent(msg.getClass(), unused -> (Short2ObjectLinkedOpenHashMap<SimplePendingPacket>) short2ObjectLinkedOpenHashMapPool.alloc())
                        .putAndMoveToLast(ChunkSectionPos.packLocal(pos), new SimplePendingPacket(msg, promise));
            } else {
                queuedPackets.add(new SimplePendingPacket(msg, promise));
            }
        }

        private void handleBlockUpdate(BlockPos pos, BlockState state) {
            this.sectionQueues.computeIfAbsent(ChunkSectionPos.getSectionCoord(pos.getY()), unused -> sectionUpdateQueueSimpleObjectPool.alloc())
                    .blockUpdates.putAndMoveToLast(ChunkSectionPos.packLocal(pos), state);
        }

        public PendingPacket producePacket(ChannelHandlerContext ctx) {
            if (!this.sectionQueues.isEmpty()) {
                final ObjectBidirectionalIterator<Int2ReferenceMap.Entry<PacketPriorityHandler.SectionUpdateQueue>> iterator = this.sectionQueues.int2ReferenceEntrySet().fastIterator();
                while (iterator.hasNext()) {
                    final Int2ReferenceMap.Entry<PacketPriorityHandler.SectionUpdateQueue> entry = iterator.next();
                    final SimplePendingPacket simplePendingPacket = entry.getValue().tryProduce(ctx, ChunkSectionPos.from(this.chunkPos, entry.getIntKey()));
                    if (simplePendingPacket != null) {
                        return makePacket(simplePendingPacket);
                    }
                    sectionUpdateQueueSimpleObjectPool.release(entry.getValue());
                    iterator.remove();
                }
            }

            if (!sequencedQueuedPackets.isEmpty()) {
                return makePacket(this.sequencedQueuedPackets.removeFirst());
            }

            if (!this.queuedPackets.isEmpty()) {
                return makePacket(this.queuedPackets.remove(0));
            }

            return null;
        }

        private PendingPacket makePacket(SimplePendingPacket packet) {
            return new PendingRawPacket(packet.packet, packet.promise, currentOrderIndex ++, 6);
        }

        public void clear() {
            this.chunkPos = null;
            for (Int2ReferenceMap.Entry<SectionUpdateQueue> entry : this.sectionQueues.int2ReferenceEntrySet()) {
                entry.getValue().clear();
                sectionUpdateQueueSimpleObjectPool.release(entry.getValue());
            }
            this.sectionQueues.clear();
            for (Object2ObjectMap.Entry<Class<?>, SimplePendingPacket> entry : this.sequencedQueuedPackets.object2ObjectEntrySet()) {
                entry.getValue().promise.trySuccess();
                ReferenceCountUtil.release(entry.getValue().packet);
            }
            this.sequencedQueuedPackets.clear();
            for (SimplePendingPacket packet : this.queuedPackets) {
                packet.promise.trySuccess();
                ReferenceCountUtil.release(packet.packet);
            }
            this.queuedPackets.clear();
        }
    }

    private final class SectionUpdateQueue {
        private final ObjectArrayList<ChannelPromise> blockUpdatePromises;
        private final Short2ObjectLinkedOpenHashMap<BlockState> blockUpdates;
        private final Object2ObjectOpenHashMap<Class<?>, Short2ObjectLinkedOpenHashMap<ChunkUpdateQueue.SimplePendingPacket>> perBlockSequencedQueuedPackets;

        private SectionUpdateQueue() {
            this.blockUpdatePromises = new ObjectArrayList<>();
            this.blockUpdates = new Short2ObjectLinkedOpenHashMap<>();
            this.perBlockSequencedQueuedPackets = new Object2ObjectOpenHashMap<>();
        }

        public void clear() {
            for (ChannelPromise promise : blockUpdatePromises) {
                promise.trySuccess();
            }
            blockUpdatePromises.clear();
            blockUpdates.clear();
            for (Object2ObjectMap.Entry<Class<?>, Short2ObjectLinkedOpenHashMap<ChunkUpdateQueue.SimplePendingPacket>> entry : perBlockSequencedQueuedPackets.object2ObjectEntrySet()) {
                for (Short2ObjectMap.Entry<ChunkUpdateQueue.SimplePendingPacket> packetEntry : entry.getValue().short2ObjectEntrySet()) {
                    packetEntry.getValue().promise.trySuccess();
                    ReferenceCountUtil.release(packetEntry.getValue().packet);
                }
                short2ObjectLinkedOpenHashMapPool.release(entry.getValue());
            }
            perBlockSequencedQueuedPackets.clear();
        }

        private ChunkUpdateQueue.SimplePendingPacket tryProduce(ChannelHandlerContext ctx, ChunkSectionPos chunkSectionPos) {
            if (!this.blockUpdates.isEmpty()) {
                // create promise
                final ChannelPromise[] futures = this.blockUpdatePromises.toArray(ChannelPromise[]::new);
                this.blockUpdatePromises.clear();
                final ChannelPromise promise = ctx.newPromise();
                promise.addListener(future -> {
                    for (ChannelPromise channelPromise : futures) {
                        if (future.isSuccess()) {
                            channelPromise.trySuccess();
                        } else {
                            channelPromise.tryFailure(future.cause());
                        }
                    }
                });

                // create packet
                empty.setIndex(0, empty.capacity());
                final ChunkDeltaUpdateS2CPacket msg = new ChunkDeltaUpdateS2CPacket(empty);
                empty.setIndex(0, empty.capacity());
                final int size = this.blockUpdates.size();
                short[] positions = new short[size];
                BlockState[] blockStates = new BlockState[size];
                final ObjectBidirectionalIterator<Short2ObjectMap.Entry<BlockState>> iterator = this.blockUpdates.short2ObjectEntrySet().fastIterator();
                int i = 0;
                while (iterator.hasNext()) {
                    final Short2ObjectMap.Entry<BlockState> entry = iterator.next();
                    positions[i] = entry.getShortKey();
                    blockStates[i] = entry.getValue();
                    i++;
                }
                this.blockUpdates.clear();
                ((IChunkDeltaUpdateS2CPacket) msg).setSectionPos(chunkSectionPos);
                ((IChunkDeltaUpdateS2CPacket) msg).setPositions(positions);
                ((IChunkDeltaUpdateS2CPacket) msg).setBlockStates(blockStates);
//                ((IChunkDeltaUpdateS2CPacket) msg).setNoLightingUpdates(false);

                return new ChunkUpdateQueue.SimplePendingPacket(msg, promise);
            }

            if (!this.perBlockSequencedQueuedPackets.isEmpty()) {
                final ObjectIterator<Object2ObjectMap.Entry<Class<?>, Short2ObjectLinkedOpenHashMap<ChunkUpdateQueue.SimplePendingPacket>>> iterator = this.perBlockSequencedQueuedPackets.object2ObjectEntrySet().fastIterator();
                while (iterator.hasNext()) {
                    final Object2ObjectMap.Entry<Class<?>, Short2ObjectLinkedOpenHashMap<ChunkUpdateQueue.SimplePendingPacket>> entry = iterator.next();
                    final Short2ObjectLinkedOpenHashMap<ChunkUpdateQueue.SimplePendingPacket> map = entry.getValue();
                    if (!map.isEmpty()) {
                        return map.removeFirst();
                    } else {
                        short2ObjectLinkedOpenHashMapPool.release(map);
                        iterator.remove();
                    }
                }
            }

            return null;
        }

        @Override
        public String toString() {
            return "SectionUpdateQueue[" +
                    "blockUpdatePromises=" + blockUpdatePromises + ", " +
                    "blockUpdates=" + blockUpdates + ", " +
                    "perBlockSequencedQueuedPackets=" + perBlockSequencedQueuedPackets + ']';
        }

    }

    private static final Comparator<PendingPacket> cmp =
            Comparator.comparingInt(PendingPacket::priority)
                    .thenComparingLong(PendingPacket::orderIndex);

    private static final IntOpenHashSet channelToIgnoreWhenSync = new IntOpenHashSet(new int[]{1});

    private final PacketByteBuf empty = new PacketByteBuf(Unpooled.wrappedBuffer(new byte[64]));
    private final PriorityQueue<PendingPacket> queue = new PriorityQueue<>(cmp);
    private boolean isEnabled = false;
    private long currentOrderIndex = 0;

    private final Long2IntOpenHashMap sentChunkPacketHashes = new Long2IntOpenHashMap();
    private final LongOpenHashSet actuallySentChunks = new LongOpenHashSet();
    private final Long2ObjectLinkedOpenHashMap<ChunkUpdateQueue> chunkUpdateQueues = new Long2ObjectLinkedOpenHashMap<>();
    private final SimpleObjectPool<Short2ObjectLinkedOpenHashMap<?>> short2ObjectLinkedOpenHashMapPool =
            new SimpleObjectPool<>(unused -> new Short2ObjectLinkedOpenHashMap<>(), Short2ObjectLinkedOpenHashMap::clear, Short2ObjectLinkedOpenHashMap::clear, 256);
    private final SimpleObjectPool<ChunkUpdateQueue> chunkUpdateQueueSimpleObjectPool =
            new SimpleObjectPool<>(unused -> new ChunkUpdateQueue(), ChunkUpdateQueue::clear, ChunkUpdateQueue::clear, 256);
    private final SimpleObjectPool<SectionUpdateQueue> sectionUpdateQueueSimpleObjectPool =
            new SimpleObjectPool<>(unused -> new SectionUpdateQueue(), SectionUpdateQueue::clear, SectionUpdateQueue::clear, 256);
    private int serverViewDistance = 3;
    private int chunkCenterX = 0;
    private int chunkCenterZ = 0;
    private Identifier dimension = null;

    private boolean shouldDropPacketEarly(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof PlayerRespawnS2CPacket packet) {
            final Identifier changedDimension = packet.getDimension().getValue();
            if (changedDimension.equals(this.dimension)) {
                this.userEventTriggered(ctx, SYNC_REQUEST_OBJECT);
            } else {
                this.dimension = changedDimension;
                this.userEventTriggered(ctx, SYNC_REQUEST_OBJECT_DIM_CHANGE);
            }
        } else if (msg instanceof GameJoinS2CPacket packet) {
            this.dimension = packet.dimensionId().getValue();
            int lastViewDistance = this.serverViewDistance;
            this.serverViewDistance = packet.viewDistance() + 1;
            if (lastViewDistance != this.serverViewDistance) ensureChunkInVD(ctx);
        } else if (msg instanceof ChunkDataS2CPacket packet) {
            if (isInRange(packet.getX(), packet.getZ())) {
                final long pos = ChunkPos.toLong(packet.getX(), packet.getZ());
                sentChunkPacketHashes.put(pos, System.identityHashCode(packet));
                clearChunkUpdateQueue(pos);
            } else {
                System.err.println("Not sending chunk [%d, %d] as it is not in view distance".formatted(packet.getX(), packet.getZ()));
            }
        } else if (msg instanceof UnloadChunkS2CPacket packet) {
            final long pos = ChunkPos.toLong(packet.getX(), packet.getZ());
            sentChunkPacketHashes.remove(pos);
            actuallySentChunks.remove(pos);
            clearChunkUpdateQueue(pos);
        } else if (msg instanceof ChunkLoadDistanceS2CPacket packet) {
            int lastViewDistance = this.serverViewDistance;
            this.serverViewDistance = packet.getDistance() + 1;
            if (lastViewDistance != this.serverViewDistance) ensureChunkInVD(ctx);
        } else if (msg instanceof ChunkRenderDistanceCenterS2CPacket packet) {
            this.chunkCenterX = packet.getChunkX();
            this.chunkCenterZ = packet.getChunkZ();
            ensureChunkInVD(ctx);
        } else if (Config.USE_PACKET_PRIORITY_SYSTEM_BLOCK_UPDATE_CONSOLIDATION && ChunkUpdateQueue.chunkUpdatePackets.containsKey(msg.getClass())) {
            final ChunkPos chunkPos = ChunkUpdateQueue.chunkUpdatePackets.get(msg.getClass()).apply(msg);
            if (sentChunkPacketHashes.containsKey(chunkPos.toLong())) {
                this.chunkUpdateQueues.computeIfAbsent(chunkPos.toLong(), pos -> chunkUpdateQueueSimpleObjectPool.alloc().submitChunk(new ChunkPos(pos)))
                        .consumePacket(ReferenceCountUtil.retain(msg), promise);
                return true;
            }
        }
        return false;
    }

    private void clearChunkUpdateQueue(long pos) {
        if (chunkUpdateQueues.containsKey(pos)) {
            final ChunkUpdateQueue chunkUpdateQueue = chunkUpdateQueues.remove(pos);
            chunkUpdateQueue.clear();
            chunkUpdateQueueSimpleObjectPool.release(chunkUpdateQueue);
        }
    }

    private boolean shouldDropPacket(Object msg) {
        if (msg instanceof ChunkDataS2CPacket packet) {
            final long coord = ChunkPos.toLong(packet.getX(), packet.getZ());
            final int hash = sentChunkPacketHashes.getOrDefault(coord, Integer.MIN_VALUE);
            boolean isValidHash = hash != Integer.MIN_VALUE || sentChunkPacketHashes.containsKey(coord);
            if (!isValidHash)
                return true; // chunk unloaded, no need to send packet
            if (hash != System.identityHashCode(packet))
                return true; // there is a newer packet containing the same chunk data, dropping
            actuallySentChunks.add(coord);
        }
//        else if (msg instanceof UnloadChunkS2CPacket packet) {
//            final long coord = ChunkPos.toLong(packet.getX(), packet.getZ());
//            final int hash = sentChunkPacketHashes.getOrDefault(coord, Integer.MIN_VALUE);
//            boolean isValidHash = hash != Integer.MIN_VALUE || sentChunkPacketHashes.containsKey(coord);
//            if (isValidHash)
//                return true; // don't unload chunk if its going to be sent again later
//        }
        return false;
    }

    private void ensureChunkInVD(ChannelHandlerContext ctx) {
        final ObjectIterator<Long2IntMap.Entry> iterator = this.sentChunkPacketHashes.long2IntEntrySet().fastIterator();
        while (iterator.hasNext()) {
            final Long2IntMap.Entry entry = iterator.next();
            final long pos = entry.getLongKey();
            final int x = ChunkPos.getPackedX(pos);
            final int z = ChunkPos.getPackedZ(pos);
            if (!isInRange(x, z)) {
                iterator.remove();
                ctx.write(new UnloadChunkS2CPacket(x, z));
                actuallySentChunks.remove(pos);
                clearChunkUpdateQueue(pos);
            }
        }
    }

    private boolean isInRange(int x, int z) {
        return Math.max(Math.abs(x - this.chunkCenterX), Math.abs(z - this.chunkCenterZ)) <= this.serverViewDistance;
    }

    private void adjustSendBuffer(ChannelHandlerContext ctx) {
        final int sendBuffer;
        if (ctx.channel().isWritable()) {
            sendBuffer = DEFAULT_SNDBUF; // back to lowlatency
        } else {
            sendBuffer = (int) (DEFAULT_SNDBUF * 2 + ctx.channel().bytesBeforeWritable());
        }
        ctx.channel().config().setOption(ChannelOption.SO_SNDBUF, sendBuffer);
        ctx.flush();
    }

    @Override
    public void channelActive(@NotNull ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        adjustSendBuffer(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        final long orderIndex = currentOrderIndex++;
        if (shouldDropPacketEarly(ctx, msg, promise)) {
            ReferenceCountUtil.release(msg);
            return;
        }
        if (this.isEnabled) {
            if (writesAllowed(ctx) && this.queue.isEmpty()) {
                if (!shouldDropPacket(msg)) {
                    ctx.write(msg, promise);
                } else {
                    ReferenceCountUtil.release(msg);
                    promise.trySuccess();
                }
            } else {
                this.queue.add(new PendingRawPacket(msg, promise, orderIndex,
                        RakNetMultiChannel.getPacketChannelOverride(msg.getClass())));
            }
        } else {
            super.write(ctx, msg, promise);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt == SYNC_REQUEST_OBJECT || evt == SYNC_REQUEST_OBJECT_DIM_CHANGE) {
            this.isEnabled = false;

            if (evt == SYNC_REQUEST_OBJECT_DIM_CHANGE) {
                for (Long2ObjectMap.Entry<ChunkUpdateQueue> entry : this.chunkUpdateQueues.long2ObjectEntrySet()) {
                    entry.getValue().clear();
                    chunkUpdateQueueSimpleObjectPool.release(entry.getValue());
                }
                this.chunkUpdateQueues.clear();
                this.actuallySentChunks.clear();
                this.sentChunkPacketHashes.clear();
            }

            ObjectArrayList<PendingPacket> retainedPackets = new ObjectArrayList<>(this.queue.size() / 12);
            PendingPacket pendingPacket;
            while ((pendingPacket = this.queue.poll()) != null) {
                if (channelToIgnoreWhenSync.contains(pendingPacket.priority()))
                    retainedPackets.add(pendingPacket);
            }
            this.queue.addAll(retainedPackets);
            System.out.println("VMP: Stopped priority handler, retained %d packets".formatted(retainedPackets.size()));
            tryFlushPackets(ctx, true);
        } else if (evt == START_PRIORITY) {
            this.isEnabled = true;
            System.out.println("VMP: Started priority handler");
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
        if (!ctx.channel().isOpen()) return;

        adjustSendBuffer(ctx);
        if (ctx.channel().isWritable()) tryFlushPackets(ctx, false);
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        tryFlushPackets(ctx, false);
    }

    private void tryFlushPackets(ChannelHandlerContext ctx, boolean ignoreWritability) {
        PendingPacket pendingPacket;
        if (queue.isEmpty()) {
            tryFlushBlockUpdates(ctx, ignoreWritability);
        }
        while ((ignoreWritability || writesAllowed(ctx)) && (pendingPacket = this.queue.peek()) != null) {
            final boolean flushedBlockUpdates = pendingPacket.orderIndex() > 6 && tryFlushBlockUpdates(ctx, ignoreWritability);
            if (!flushedBlockUpdates) {
                this.queue.poll();
                final Object msg = pendingPacket.msg();
                if (shouldDropPacket(msg)) {
                    pendingPacket.release();
                } else {
                    pendingPacket.dispatch(ctx);
                }
            }
        }
        ctx.flush();
    }

    private boolean tryFlushBlockUpdates(ChannelHandlerContext ctx, boolean ignoreWritability) {
        final ObjectBidirectionalIterator<Long2ObjectMap.Entry<ChunkUpdateQueue>> iterator = this.chunkUpdateQueues.long2ObjectEntrySet().fastIterator();
        boolean hasWork = false;
        while ((ignoreWritability || writesAllowed(ctx)) && iterator.hasNext()) {
            final Long2ObjectMap.Entry<ChunkUpdateQueue> entry = iterator.next();
            if (!actuallySentChunks.contains(entry.getLongKey())) continue;
            final ChunkUpdateQueue queue = entry.getValue();
            final PendingPacket pendingPacket = queue.producePacket(ctx);
            if (pendingPacket == null) { // nothing in queue
                iterator.remove();
                chunkUpdateQueueSimpleObjectPool.release(queue);
            } else {
                final Object msg = pendingPacket.msg();
                if (shouldDropPacket(msg)) {
                    pendingPacket.release();
                } else {
                    pendingPacket.dispatch(ctx);
                    hasWork = true;
                }
            }
        }
        return hasWork;
    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception {
        if (this.isEnabled) tryFlushPackets(ctx, true);
        super.channelInactive(ctx);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        if (this.isEnabled) tryFlushPackets(ctx, true);
        super.close(ctx, promise);
    }

    private boolean writesAllowed(ChannelHandlerContext ctx) {
        return ctx.channel().isWritable();
    }

    private interface PendingPacket {
        @Nullable
        Object msg();

        void dispatch(ChannelHandlerContext ctx);

        void release();

        long orderIndex();

        int priority();
    }

    private record PendingRawPacket(Object msg, ChannelPromise promise, long orderIndex,
                                    int priority) implements PendingPacket {

        @Override
        public void dispatch(ChannelHandlerContext ctx) {
            ctx.write(msg, promise);
        }

        @Override
        public void release() {
            ReferenceCountUtil.release(msg);
            promise.trySuccess();
        }

    }

}
