package com.ishland.vmp.mixins.networking;

import com.ishland.vmp.common.networking.NetworkingTasksInterface;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.util.thread.ThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(NetworkThreadUtils.class)
public class MixinNetworkThreadUtils {

    @ModifyArg(
            method = "forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V")
    )
    private static ThreadExecutor<?> modifyServerExecutor(ThreadExecutor<?> engine) {
        if (engine instanceof NetworkingTasksInterface networkingTasksInterface) {
            return networkingTasksInterface.getNetworkingTasksExecutor();
        } else {
            return engine;
        }
    }

}
