package com.ishland.vmp.common.networking.priority;

import com.ishland.raknetify.fabric.common.connection.RakNetMultiChannel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.PriorityQueue;

public class PacketPriorityHandler extends ChannelDuplexHandler {

    private static final Comparator<PendingPacket> cmp =
            Comparator.comparingInt(PendingPacket::priority)
                    .thenComparingLong(PendingPacket::orderIndex);

    private final PriorityQueue<PendingPacket> queue = new PriorityQueue<>(cmp);
    private long currentOrderIndex = 0;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        final long orderIndex = currentOrderIndex++;
        if (writesAllowed(ctx)) {
            ctx.write(msg, promise);
            return;
        } else {
            this.queue.add(new PendingPacket(msg, promise, orderIndex,
                    RakNetMultiChannel.getPacketChannelOverride(msg.getClass())));
            return;
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
            ctx.write(pendingPacket.msg, pendingPacket.promise);
        }
        ctx.flush();
    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception {
        tryFlushPackets(ctx, true);
        super.channelInactive(ctx);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        tryFlushPackets(ctx, true);
        super.close(ctx, promise);
    }

    private boolean writesAllowed(ChannelHandlerContext ctx) {
        return ctx.channel().isWritable();
    }

    private record PendingPacket(Object msg, ChannelPromise promise, long orderIndex, int priority) {
    }

}
