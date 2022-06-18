package com.ishland.vmp.mixins.networking;

import com.ishland.vmp.common.networking.NetworkingThreadExecutor;
import com.ishland.vmp.common.networking.NetworkingTasksInterface;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.ThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer implements NetworkingTasksInterface {

    @Unique
    private NetworkingThreadExecutor networkingTasksQueue;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.networkingTasksQueue = new NetworkingThreadExecutor((MinecraftServer) (Object) this);
    }

    @Override
    public ThreadExecutor<Runnable> getNetworkingTasksExecutor() {
        return Objects.requireNonNull(this.networkingTasksQueue);
    }

    @Inject(method = "runTasksTillTickEnd", at = @At("HEAD"))
    private void onWaitingForNextTick(CallbackInfo ci) {
        while (this.networkingTasksQueue.runTask());
    }

}
