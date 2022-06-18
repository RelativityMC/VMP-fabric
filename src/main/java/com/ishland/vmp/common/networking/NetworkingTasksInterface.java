package com.ishland.vmp.common.networking;

import net.minecraft.util.thread.ThreadExecutor;

public interface NetworkingTasksInterface {

    ThreadExecutor<Runnable> getNetworkingTasksExecutor();

}
