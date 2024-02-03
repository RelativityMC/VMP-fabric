package com.ishland.vmp.common.networking.eventloops;

import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.FastThreadLocalThread;
import net.minecraft.network.NetworkState;

import java.util.function.Supplier;

public class VMPEventLoops {

    public static final Supplier<NioEventLoopGroup> NIO_LOGIN_EVENT_LOOP_GROUP =
            Suppliers.memoize(() -> new NioEventLoopGroup(
                    2,
                    new ThreadFactoryBuilder()
                            .setThreadFactory(FastThreadLocalThread::new)
                            .setNameFormat("Netty Server Login IO #%d")
                            .setDaemon(true)
                            .build()
                    )
            );

    public static final Supplier<NioEventLoopGroup> NIO_PLAY_EVENT_LOOP_GROUP =
            Suppliers.memoize(() -> new NioEventLoopGroup(
                            0,
                            new ThreadFactoryBuilder()
                                    .setThreadFactory(FastThreadLocalThread::new)
                                    .setNameFormat("Netty Server Play IO #%d")
                                    .setDaemon(true)
                                    .build()
                    )
            );

    public static final Supplier<EpollEventLoopGroup> EPOLL_LOGIN_EVENT_LOOP_GROUP =
            Suppliers.memoize(() -> new EpollEventLoopGroup(
                            2,
                            new ThreadFactoryBuilder()
                                    .setThreadFactory(FastThreadLocalThread::new)
                                    .setNameFormat("Netty Epoll Server Login IO #%d")
                                    .setDaemon(true)
                                    .build()
                    )
            );

    public static final Supplier<EpollEventLoopGroup> EPOLL_PLAY_EVENT_LOOP_GROUP =
            Suppliers.memoize(() -> new EpollEventLoopGroup(
                            0,
                            new ThreadFactoryBuilder()
                                    .setThreadFactory(FastThreadLocalThread::new)
                                    .setNameFormat("Netty Epoll Server Play IO #%d")
                                    .setDaemon(true)
                                    .build()
                    )
            );

    public static EventLoopGroup getEventLoopGroup(Channel channel, NetworkState state) {
        if (channel instanceof NioSocketChannel) {
            if (state == NetworkState.LOGIN) {
                return NIO_LOGIN_EVENT_LOOP_GROUP.get();
            } else if (state == NetworkState.PLAY) {
                return NIO_PLAY_EVENT_LOOP_GROUP.get();
            }
        } else if (channel instanceof EpollSocketChannel) {
            if (state == NetworkState.LOGIN) {
                return EPOLL_LOGIN_EVENT_LOOP_GROUP.get();
            } else if (state == NetworkState.PLAY) {
                return EPOLL_PLAY_EVENT_LOOP_GROUP.get();
            }
        }
        return null;
    }

}
