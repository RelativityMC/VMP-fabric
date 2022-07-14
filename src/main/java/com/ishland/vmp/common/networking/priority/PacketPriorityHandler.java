package com.ishland.vmp.common.networking.priority;

import com.ishland.raknetify.fabric.common.connection.RakNetMultiChannel;
import com.ishland.vmp.common.config.Config;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.ChunkPos;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.PriorityQueue;

public class PacketPriorityHandler extends ChannelDuplexHandler {

    private static final int IP_TOS_LOWDELAY = 0b00010000;
    private static final int IP_TOS_THROUGHPUT = 0b00001000;
    private static final int IP_TOS_RELIABILITY = 0b00000100;

    public static void setupPacketPriority(Channel channel) {
        if (Config.USE_PACKET_PRIORITY_SYSTEM) {
            if (channel instanceof SocketChannel) {
                channel.pipeline().addLast("vmp_packet_priority", new PacketPriorityHandler());
                channel.config().setOption(ChannelOption.SO_SNDBUF, 32 * 1024); // reduce latency
                channel.config().setOption(ChannelOption.IP_TOS, IP_TOS_LOWDELAY | IP_TOS_THROUGHPUT); // reduce latency
            }
        }
    }

    public static final Object SYNC_REQUEST_OBJECT = new Object();
    public static final Object START_PRIORITY = new Object();

    private static final Comparator<PendingPacket> cmp =
            Comparator.comparingInt(PendingPacket::priority)
                    .thenComparingLong(PendingPacket::orderIndex);

    private static final IntOpenHashSet channelToIgnoreWhenSync = new IntOpenHashSet(new int[]{1});

    private final PriorityQueue<PendingPacket> queue = new PriorityQueue<>(cmp);
    private boolean isEnabled = false;
    private long currentOrderIndex = 0;

    private final Long2IntOpenHashMap sentChunkPacketHashes = new Long2IntOpenHashMap();

    private void handlePacket(Object msg) {
        if (msg instanceof ChunkDataS2CPacket packet) {
            sentChunkPacketHashes.put(ChunkPos.toLong(packet.getX(), packet.getZ()), System.identityHashCode(packet));
        } else if (msg instanceof UnloadChunkS2CPacket packet) {
            sentChunkPacketHashes.remove(ChunkPos.toLong(packet.getX(), packet.getZ()));
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
        } else if (msg instanceof UnloadChunkS2CPacket packet) {
            final long coord = ChunkPos.toLong(packet.getX(), packet.getZ());
            final int hash = sentChunkPacketHashes.getOrDefault(coord, Integer.MIN_VALUE);
            boolean isValidHash = hash != Integer.MIN_VALUE || sentChunkPacketHashes.containsKey(coord);
            if (isValidHash)
                return true; // don't unload chunk if its going to be sent again later
        }
        return false;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        final long orderIndex = currentOrderIndex++;
        handlePacket(msg);
        if (this.isEnabled) {
            if (writesAllowed(ctx) && this.queue.isEmpty()) {
                ctx.write(msg, promise);
            } else {
                this.queue.add(new PendingPacket(msg, promise, orderIndex,
                        RakNetMultiChannel.getPacketChannelOverride(msg.getClass())));
            }
        } else {
            super.write(ctx, msg, promise);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt == SYNC_REQUEST_OBJECT) {
            this.isEnabled = false;
            ObjectArrayList<PendingPacket> retainedPackets = new ObjectArrayList<>(this.queue.size() / 12);
            PendingPacket pendingPacket;
            while ((pendingPacket = this.queue.poll()) != null) {
                if (channelToIgnoreWhenSync.contains(pendingPacket.priority))
                    retainedPackets.add(pendingPacket);
            }
            this.queue.addAll(retainedPackets);
            this.sentChunkPacketHashes.clear();
            System.out.println("VMP: Stopped priority handler, retained %d packets".formatted(retainedPackets.size()));
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
        if (ctx.channel().isWritable()) tryFlushPackets(ctx, false);
    }

//    @Override
//    public void flush(ChannelHandlerContext ctx) throws Exception {
//        if (ctx.channel().isWritable()) ctx.flush();
//    }

    private void tryFlushPackets(ChannelHandlerContext ctx, boolean ignoreWritability) {
        PendingPacket pendingPacket;
        while ((ignoreWritability || writesAllowed(ctx)) && (pendingPacket = this.queue.poll()) != null) {
            if (shouldDropPacket(pendingPacket.msg())) {
                ReferenceCountUtil.release(pendingPacket.msg);
                pendingPacket.promise.trySuccess();
            } else {
                ctx.write(pendingPacket.msg, pendingPacket.promise);
            }
        }
        ctx.flush();
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

    private record PendingPacket(Object msg, ChannelPromise promise, long orderIndex, int priority) {
    }

}
