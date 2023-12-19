package com.ishland.vmp.mixins.chunk.loading.async_chunk_on_player_login;

import com.ishland.vmp.common.chunk.loading.async_chunks_on_player_login.IAsyncChunkPlayer;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity implements IAsyncChunkPlayer {

    @Unique
    private Optional<NbtCompound> playerData = Optional.empty();

    @Unique
    private boolean chunkLoadCompleted = true;

    @Override
    public void markPlayerForAsyncChunkLoad() {
        this.chunkLoadCompleted = false;
    }

    @Override
    public void setPlayerData(Optional<NbtCompound> nbtCompound) {
        this.playerData = nbtCompound;
    }

    @Override
    public Optional<NbtCompound> getPlayerData() {
        return this.playerData;
    }

    @Override
    public boolean isChunkLoadCompleted() {
        return this.chunkLoadCompleted;
    }

    @Override
    public void onChunkLoadComplete() {
        this.chunkLoadCompleted = true;
    }

    @Inject(
            method = {
                    "playerTick"
            },
            at = @At("HEAD"),
            cancellable = true
    )
    private void suppressActionsDuringChunkLoad(CallbackInfo ci) {
        if (!this.chunkLoadCompleted) ci.cancel();
    }
}
