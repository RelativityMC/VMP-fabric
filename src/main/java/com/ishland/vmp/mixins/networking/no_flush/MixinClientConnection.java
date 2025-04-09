package com.ishland.vmp.mixins.networking.no_flush;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.AbstractEventExecutor;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lio/netty/channel/Channel;flush()Lio/netty/channel/Channel;"))
    private Channel dontFlush(Channel instance) {
        return instance; // no-op
    }

    @WrapOperation(method = "sendImmediately", at = @At(value = "INVOKE", target = "Lio/netty/channel/EventLoop;execute(Ljava/lang/Runnable;)V"))
    private void avoidImmediateExecute(EventLoop instance, Runnable runnable, Operation<Void> original, Packet<?> packet, @Nullable PacketCallbacks callbacks, boolean flush) {
        if (!flush && instance instanceof AbstractEventExecutor executor) {
            executor.lazyExecute(runnable);
        } else {
            original.call(instance, runnable);
        }
    }

}
