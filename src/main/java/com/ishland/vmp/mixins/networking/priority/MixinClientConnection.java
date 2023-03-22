package com.ishland.vmp.mixins.networking.priority;

import com.ishland.vmp.common.networking.priority.PacketPriorityHandler;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    @Inject(method = "addHandlers", at = @At("RETURN"))
    private static void postAddHandlers(ChannelPipeline pipeline, NetworkSide side, CallbackInfo ci) {
        PacketPriorityHandler.setupPacketPriority(pipeline);
    }
}
