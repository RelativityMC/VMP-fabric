package com.ishland.vmp.mixins.chunk.loading.async_chunk_on_player_login;

import com.google.common.base.Stopwatch;
import com.ishland.vmp.common.chunk.loading.async_chunks_on_player_login.AsyncChunkLoadUtil;
import com.ishland.vmp.common.config.Config;
import com.ishland.vmp.mixins.access.IClientConnection;
import com.ishland.vmp.mixins.access.IServerChunkManager;
import com.ishland.vmp.mixins.access.IThreadedAnvilChunkStorage;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Dynamic;
import io.netty.channel.Channel;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.listener.ServerConfigurationPacketListener;
import net.minecraft.network.listener.TickablePacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.JoinWorldTask;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayerConfigurationTask;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Mixin(ServerConfigurationNetworkHandler.class)
public abstract class MixinServerConfigurationNetworkHandler extends ServerCommonNetworkHandler implements ServerConfigurationPacketListener, TickablePacketListener {

    @Shadow @Final private static Logger LOGGER;

    @Shadow public abstract boolean isConnectionOpen();

    @Shadow @Final private GameProfile profile;

    @Shadow private SyncedClientOptions syncedOptions;

    @Shadow @Final private static Text INVALID_PLAYER_DATA_TEXT;

    @Unique
    private static final ChunkTicketType VMP_PLAYER_ASYNC_CHUNKS = new ChunkTicketType(0L, false, ChunkTicketType.Use.LOADING);

    @Unique
    private ChunkPos vmp$ticketHeld;

    @Unique
    private ServerWorld vmp$ticketHeldWorld;

    @Unique
    private ServerPlayerEntity vmp$heldPlayer;

    public MixinServerConfigurationNetworkHandler(MinecraftServer server, ClientConnection connection, ConnectedClientData clientData) {
        super(server, connection, clientData);
    }

    @Inject(method = "onDisconnected", at = @At("RETURN"))
    private void onDisconnect(DisconnectionInfo info, CallbackInfo ci) {
        vmp$dropTicket();
    }

    @Unique
    private void vmp$dropTicket() {
        if (this.vmp$ticketHeld != null && this.vmp$ticketHeldWorld != null) {
            ((IServerChunkManager) this.vmp$ticketHeldWorld.getChunkManager()).getTicketManager().removeTicket(VMP_PLAYER_ASYNC_CHUNKS, this.vmp$ticketHeld, 2);
            this.vmp$ticketHeld = null;
            this.vmp$ticketHeldWorld = null;
        }
    }

    @WrapOperation(method = "onReady", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;createPlayer(Lcom/mojang/authlib/GameProfile;Lnet/minecraft/network/packet/c2s/common/SyncedClientOptions;)Lnet/minecraft/server/network/ServerPlayerEntity;"))
    private ServerPlayerEntity replacePlayer(PlayerManager instance, GameProfile profile, SyncedClientOptions syncedOptions, Operation<ServerPlayerEntity> original) {
        if (this.vmp$heldPlayer != null) {
            this.vmp$dropTicket();
            return this.vmp$heldPlayer;
        } else {
            return original.call(instance, profile, syncedOptions);
        }
    }

    @WrapOperation(method = "pollTask", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerConfigurationTask;sendPacket(Ljava/util/function/Consumer;)V"))
    private void delayJoinWorld(ServerPlayerConfigurationTask instance, Consumer<Packet<?>> packetConsumer, Operation<Void> original) {
        if (instance instanceof JoinWorldTask) {
            PlayerManager playerManager = this.server.getPlayerManager();
            if (playerManager.getPlayer(this.profile.getId()) != null) {
                this.disconnect(PlayerManager.DUPLICATE_LOGIN_TEXT);
                return;
            }

            Text text = playerManager.checkCanJoin(this.connection.getAddress(), this.profile);
            if (text != null) {
                this.disconnect(text);
                return;
            }

            ServerPlayerEntity player = playerManager.createPlayer(this.profile, this.syncedOptions);
            this.vmp$heldPlayer = player;

            RegistryKey<World> registryKey = playerManager.loadPlayerData(player)
                    .flatMap(nbt -> DimensionType.worldFromDimensionNbt(new Dynamic<>(NbtOps.INSTANCE, nbt.get("Dimension"))).resultOrPartial(LOGGER::error))
                    .orElse(World.OVERWORLD);
            ServerWorld storedWorld = playerManager.getServer().getWorld(registryKey);
            ServerWorld actualWorld;
            if (storedWorld == null) {
                LOGGER.warn("Unknown respawn dimension {}, defaulting to overworld", registryKey);
                actualWorld = playerManager.getServer().getOverworld();
            } else {
                actualWorld = storedWorld;
            }

            ChunkPos chunkPos = new ChunkPos(player.getBlockPos());
            this.vmp$dropTicket();
            this.vmp$ticketHeld = chunkPos;
            this.vmp$ticketHeldWorld = actualWorld;
            Stopwatch timing = Stopwatch.createStarted();
            AsyncChunkLoadUtil.SEMAPHORE.acquire().thenApplyAsync(unused -> {
                try {
                    ((IServerChunkManager) actualWorld.getChunkManager()).getTicketManager().addTicket(VMP_PLAYER_ASYNC_CHUNKS, chunkPos, 2);
                    ((IServerChunkManager) actualWorld.getChunkManager()).invokeUpdateChunks();
                    final ChunkHolder chunkHolder = ((IThreadedAnvilChunkStorage) actualWorld.getChunkManager().chunkLoadingManager).invokeGetCurrentChunkHolder(chunkPos.toLong());
                    if (chunkHolder == null) {
                        throw new IllegalStateException("Chunk not there when requested");
                    }
                    return chunkHolder.getEntityTickingFuture().whenCompleteAsync((worldChunkOptionalChunk, throwable) -> {
                        if (Config.SHOW_ASYNC_LOADING_MESSAGES) {
                            LOGGER.info("Async chunk loading for player {} completed after {}", profile.getName(), timing);
                        }
                        Channel channel = ((IClientConnection) this.connection).getChannel();

                        if (channel == null || !channel.isOpen()) {
                            return;
                        }

                        try {
                            original.call(instance, packetConsumer);
                        } catch (Throwable t1) {
                            LOGGER.error("Couldn't place player in world", t1);
                            this.connection.send(new DisconnectS2CPacket(INVALID_PLAYER_DATA_TEXT));
                            this.connection.disconnect(INVALID_PLAYER_DATA_TEXT);
                        }
                    }, ((IThreadedAnvilChunkStorage) actualWorld.getChunkManager().chunkLoadingManager).getMainThreadExecutor());
                } catch (Throwable t) {
                    LOGGER.warn("Failed to schedule chunkload for {} at {}", profile.getName(), chunkPos, t);
                    try {
                        original.call(instance, packetConsumer);
                    } catch (Throwable t1) {
                        LOGGER.error("Couldn't place player in world", t1);
                        this.connection.send(new DisconnectS2CPacket(INVALID_PLAYER_DATA_TEXT));
                        this.connection.disconnect(INVALID_PLAYER_DATA_TEXT);
                    }
                    return CompletableFuture.completedFuture(null);
                }
            }, ((IThreadedAnvilChunkStorage) actualWorld.getChunkManager().chunkLoadingManager).getMainThreadExecutor())
                    .whenComplete((completableFuture, throwable) -> AsyncChunkLoadUtil.SEMAPHORE.release());
        } else {
            original.call(instance, packetConsumer);
        }
    }

}
