package com.ishland.vmp.common.chunkloading.async_chunks_on_player_login;

import net.minecraft.nbt.NbtCompound;

public interface IAsyncChunkPlayer {

    void markPlayerForAsyncChunkLoad();

    void setPlayerData(NbtCompound nbtCompound);

    NbtCompound getPlayerData();

    boolean isChunkLoadCompleted();

    void onChunkLoadComplete();

}
