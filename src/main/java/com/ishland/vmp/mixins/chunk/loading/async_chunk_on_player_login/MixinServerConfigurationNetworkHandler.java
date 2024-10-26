package com.ishland.vmp.mixins.chunk.loading.async_chunk_on_player_login;

import com.google.common.base.Stopwatch;
import com.ishland.vmp.common.chunk.loading.async_chunks_on_player_login.AsyncChunkLoadUtil;
import com.ishland.vmp.common.config.Config;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ServerConfigurationPacketListener;
import net.minecraft.network.listener.TickablePacketListener;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerConfigurationNetworkHandler.class)
public abstract class MixinServerConfigurationNetworkHandler extends ServerCommonNetworkHandler implements ServerConfigurationPacketListener, TickablePacketListener {

    @Shadow @Final private static Logger LOGGER;

    @Shadow public abstract boolean isConnectionOpen();

    @Shadow @Final private GameProfile profile;

    public MixinServerConfigurationNetworkHandler(MinecraftServer server, ClientConnection connection, ConnectedClientData clientData) {
        super(server, connection, clientData);
    }

    @WrapOperation(method = "onReady", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V"))
    private void wrapOnPlayerConnect(PlayerManager instance, ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, Operation<Void> original) {
        RegistryKey<World> registryKey = instance.loadPlayerData(player)
                .flatMap(nbt -> DimensionType.worldFromDimensionNbt(new Dynamic<>(NbtOps.INSTANCE, nbt.get("Dimension"))).resultOrPartial(LOGGER::error))
                .orElse(World.OVERWORLD);
        ServerWorld storedWorld = instance.getServer().getWorld(registryKey);
        ServerWorld actualWorld;
        if (storedWorld == null) {
            LOGGER.warn("Unknown respawn dimension {}, defaulting to overworld", registryKey);
            actualWorld = instance.getServer().getOverworld();
        } else {
            actualWorld = storedWorld;
        }

        Stopwatch timing = Stopwatch.createStarted();
        AsyncChunkLoadUtil.scheduleChunkLoad(actualWorld, new ChunkPos(player.getBlockPos()))
                .thenRunAsync(() -> {
                    if (!this.isConnectionOpen()) {
                        return;
                    }

                    if (instance.getPlayer(this.profile.getId()) != null) {
                        this.disconnect(PlayerManager.DUPLICATE_LOGIN_TEXT);
                        return;
                    }

                    if (Config.SHOW_ASYNC_LOADING_MESSAGES) {
                        LOGGER.info("Async chunk loading for player {} completed after {}", profile.getName(), timing);
                    }

                    original.call(instance, connection, player, clientData);
                }, instance.getServer());
    }

}
