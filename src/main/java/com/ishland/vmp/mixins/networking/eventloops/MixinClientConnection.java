package com.ishland.vmp.mixins.networking.eventloops;

import com.ishland.vmp.common.networking.eventloops.VMPEventLoops;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    @Shadow private Channel channel;

    @Redirect(method = "setState", at = @At(value = "INVOKE", target = "Lio/netty/channel/ChannelConfig;setAutoRead(Z)Lio/netty/channel/ChannelConfig;"))
    private ChannelConfig onSetState(ChannelConfig instance, boolean b, NetworkState state) {
        if (this.channel.config() == instance) {
            final EventLoopGroup group = VMPEventLoops.getEventLoopGroup(this.channel, state);
            if (group != null) {
                ChannelPromise promise = this.channel.newPromise();
                this.channel.config().setAutoRead(false);
                this.channel.deregister().addListener(future -> {
                    if (future.isSuccess()) {
                        group.register(promise);
                    } else {
                        promise.setFailure(new RuntimeException("Failed to deregister channel", future.cause()));
                    }
                });
                promise.addListener(future -> {
                    if (future.isSuccess()) {
                        this.channel.config().setAutoRead(true);
                    } else {
                        this.channel.pipeline().fireExceptionCaught(future.cause());
                    }
                });
                return instance;
            } else {
                return instance.setAutoRead(b);
            }
        } else {
            return instance.setAutoRead(b);
        }
    }

}
