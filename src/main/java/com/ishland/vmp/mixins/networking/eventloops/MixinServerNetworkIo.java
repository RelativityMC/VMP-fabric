package com.ishland.vmp.mixins.networking.eventloops;

import net.minecraft.server.ServerNetworkIo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ServerNetworkIo.class)
public class MixinServerNetworkIo {

    @ModifyConstant(method = {"method_14348", "method_14349"}, constant = @Constant(intValue = 0), require = 2)
    private static int modifyAcceptorThreadCount(int constant) {
        if (constant == 0) {
            return 1; // only 1 thread for acceptor
        } else {
            return constant;
        }
    }

    @ModifyConstant(method = "method_14348", constant = @Constant(stringValue = "Netty Server IO #%d"))
    private static String modifyNioThreadName(String constant) {
        if (constant.equals("Netty Server IO #%d")) {
            return "Netty Acceptor IO Thread";
        } else {
            return constant;
        }
    }

    @ModifyConstant(method = "method_14349", constant = @Constant(stringValue = "Netty Epoll Server IO #%d"))
    private static String modifyEpollThreadName(String constant) {
        if (constant.equals("Netty Epoll Server IO #%d")) {
            return "Netty Epoll Acceptor IO Thread";
        } else {
            return constant;
        }
    }

}
