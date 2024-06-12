package com.ishland.vmp.mixins.networking.avoid_deadlocks;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    @Shadow private Channel channel;
    @Unique
    private volatile boolean isClosing = false;

    @Redirect(method = "disconnect(Lnet/minecraft/network/DisconnectionInfo;)V", at = @At(value = "INVOKE", target = "Lio/netty/channel/ChannelFuture;awaitUninterruptibly()Lio/netty/channel/ChannelFuture;", remap = false))
    private ChannelFuture noDisconnectWait(ChannelFuture instance) {
        isClosing = true;
//        if (instance.channel().eventLoop().inEventLoop()) {
//            return instance; // no-op
//        } else {
//            return instance.awaitUninterruptibly();
//        }
        return instance;
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lio/netty/channel/Channel;isOpen()Z", remap = false))
    private boolean redirectIsOpen(Channel instance) {
        return this.channel != null && (this.channel.isOpen() && !this.isClosing);
    }

}
