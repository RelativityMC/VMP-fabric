package com.ishland.vmp.common.chunk.loading.async_chunks_on_player_login;

import net.minecraft.nbt.NbtCompound;

public interface IAsyncChunkPlayer {

    void markPlayerForAsyncChunkLoad();

    void setPlayerData(NbtCompound nbtCompound);

    NbtCompound getPlayerData();

    boolean isChunkLoadCompleted();

    void onChunkLoadComplete();

}
