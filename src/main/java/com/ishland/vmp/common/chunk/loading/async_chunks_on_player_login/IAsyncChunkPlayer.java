package com.ishland.vmp.common.chunk.loading.async_chunks_on_player_login;

import net.minecraft.nbt.NbtCompound;

import java.util.Optional;

public interface IAsyncChunkPlayer {

    void markPlayerForAsyncChunkLoad();

    void setPlayerData(Optional<NbtCompound> nbtCompound);

    Optional<NbtCompound> getPlayerData();

    boolean isChunkLoadCompleted();

    void onChunkLoadComplete();

}
