package com.ishland.vmp.common.networking;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.ThreadExecutor;

public class NetworkingThreadExecutor extends ThreadExecutor<Runnable> {

    private final MinecraftServer server;

    public NetworkingThreadExecutor(MinecraftServer server) {
        super("Networking task main thread executor for server " + server);
        this.server = server;
    }

    @Override
    protected Runnable createTask(Runnable runnable) {
        return runnable;
    }

    @Override
    protected boolean canExecute(Runnable task) {
        return true;
    }

    @Override
    protected Thread getThread() {
        return this.server.getThread();
    }

}
