package com.ishland.vmp.mixins.chunkloading.async_chunk_on_player_login;

import com.ishland.vmp.common.chunkloading.async_chunks_on_player_login.AsyncChunkLoadUtil;
import com.ishland.vmp.common.chunkloading.async_chunks_on_player_login.IAsyncChunkPlayer;
import com.ishland.vmp.common.config.Config;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class MixinServerPlayNetworkHandler {

    @Shadow public ServerPlayerEntity player;

    @Shadow @Final private MinecraftServer server;

    @Shadow public abstract void onClientStatus(ClientStatusC2SPacket packet);

    @Shadow @Final private static Logger LOGGER;

    @Shadow @Final public ClientConnection connection;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;updatePositionAndAngles(DDDFF)V"))
    private void suppressUpdatePositionDuringChunkLoad(ServerPlayerEntity instance, double x, double y, double z, float yaw, float pitch) {
        if (((IAsyncChunkPlayer) instance).isChunkLoadCompleted()) {
            instance.updatePositionAndAngles(x, y, z, yaw, pitch);
        }
    }

    @Unique
    private boolean isPerformingRespawn = false;

    @Inject(method = "onClientStatus",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void performAsyncRespawn(ClientStatusC2SPacket packet, CallbackInfo ci) {
        if (packet.getMode() == ClientStatusC2SPacket.Mode.PERFORM_RESPAWN) {
            if (AsyncChunkLoadUtil.isRespawnChunkLoadFinished()) return;
            if (!this.player.notInAnyWorld && this.player.getHealth() > 0.0F) return; // no need to respawn

            ci.cancel();
            if (this.isPerformingRespawn) return;

            this.isPerformingRespawn = true;
            ServerWorld spawnPointWorld = this.server.getWorld(player.getSpawnPointDimension());
            spawnPointWorld = spawnPointWorld != null ? spawnPointWorld : this.server.getOverworld();
            BlockPos spawnPointPosition = this.player.getSpawnPointPosition();
            spawnPointPosition = spawnPointPosition != null ? spawnPointPosition : spawnPointWorld.getSpawnPos();
            final ChunkPos pos = new ChunkPos(spawnPointPosition);
            if (Config.SHOW_ASYNC_LOADING_MESSAGES) {
                this.player.sendMessage(Text.literal("Performing respawn..."), true);
            }
            long startTime = System.nanoTime();
            AsyncChunkLoadUtil.scheduleChunkLoad(spawnPointWorld, pos).whenCompleteAsync((unused, throwable) -> {
                if (!this.connection.isOpen()) return; // player disconnected, no need to continue

                if (Config.SHOW_ASYNC_LOADING_MESSAGES) {
                    LOGGER.info("Async chunk loading for player {} completed", this.player.getName().getString());
                }
                this.isPerformingRespawn = false;
                try {
                    AsyncChunkLoadUtil.setIsRespawnChunkLoadFinished(true);
                    this.onClientStatus(packet);
                } finally {
                    AsyncChunkLoadUtil.setIsRespawnChunkLoadFinished(false);
                }
                if (Config.SHOW_ASYNC_LOADING_MESSAGES) {
                    this.player.sendMessage(Text.literal("Respawn finished after %.1fms".formatted((System.nanoTime() - startTime) / 1_000_000.0)), true);
                }
            }, runnable -> server.send(new ServerTask(0, runnable)));
        }
    }

}
