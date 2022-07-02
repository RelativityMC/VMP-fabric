package com.ishland.vmp.mixins.networking.priority;

import com.ishland.raknetify.fabric.common.connection.RakNetFabricConnectionUtil;
import com.ishland.vmp.common.networking.priority.PacketPriorityHandler;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/server/ServerNetworkIo$1")
public class MixinServerNetworkIo1 {

    @Inject(method = "initChannel(Lio/netty/channel/Channel;)V", at = @At("RETURN"))
    private void postChannelInit(Channel channel, CallbackInfo ci) {
        if (channel instanceof SocketChannel) {
            channel.pipeline().addLast("vmp_packet_priority", new PacketPriorityHandler());
        }
    }

}
