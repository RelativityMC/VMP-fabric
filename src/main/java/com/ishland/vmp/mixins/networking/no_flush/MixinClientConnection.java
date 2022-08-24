package com.ishland.vmp.mixins.networking.no_flush;

import io.netty.channel.Channel;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lio/netty/channel/Channel;flush()Lio/netty/channel/Channel;"))
    private Channel dontFlush(Channel instance) {
        return instance; // no-op
    }

}
