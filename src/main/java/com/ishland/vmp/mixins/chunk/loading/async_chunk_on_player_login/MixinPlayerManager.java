package com.ishland.vmp.mixins.chunk.loading.async_chunk_on_player_login;

import com.ishland.vmp.common.chunk.loading.async_chunks_on_player_login.AsyncChunkLoadUtil;
import com.ishland.vmp.common.chunk.loading.async_chunks_on_player_login.IAsyncChunkPlayer;
import com.ishland.vmp.mixins.access.IServerChunkManager;
import com.mojang.datafixers.util.Either;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerTask;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Mixin(PlayerManager.class)
public abstract class MixinPlayerManager {

    @Shadow @Final private static Logger LOGGER;

    @Shadow public abstract void sendWorldInfo(ServerPlayerEntity player, ServerWorld world);

    @Redirect(
            method = "onPlayerConnect",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;requestTeleport(DDDFF)V")
    )
    private void redirectRequestTeleport(ServerPlayNetworkHandler instance, double x, double y, double z, float yaw, float pitch) {
        final ServerChunkManager chunkManager = ((ServerWorld) instance.player.world).getChunkManager();
        final ChunkTicketManager ticketManager = ((IServerChunkManager) chunkManager).getTicketManager();

        ((IAsyncChunkPlayer) instance.player).markPlayerForAsyncChunkLoad();
        final ChunkPos pos = new ChunkPos(new BlockPos(x, y, z));
        instance.player.notInAnyWorld = true; // suppress move packets

        final MinecraftServer server = instance.player.server;
        final BiConsumer<Either<Chunk, ChunkHolder.Unloaded>, Throwable> action = (worldChunkUnloadedEither, throwable) -> {
            if (throwable != null) {
                LOGGER.error("Error while loading chunks", throwable);
                return;
            }
            if (!instance.connection.isOpen()) {
                return;
            }

            instance.player.notInAnyWorld = false;
            instance.requestTeleport(x, y, z, yaw, pitch);
            ((ServerWorld) instance.player.world).onPlayerConnected(instance.player);
            this.sendWorldInfo(instance.player, (ServerWorld) instance.player.world);
            instance.player.onSpawn();

            final NbtCompound playerData = ((IAsyncChunkPlayer) instance.player).getPlayerData();
            ((IAsyncChunkPlayer) instance.player).setPlayerData(null);
            vmp$mountSavedVehicles(instance.player, playerData);

            ((IAsyncChunkPlayer) instance.player).onChunkLoadComplete();
            LOGGER.info("Async chunk loading for player {} completed", instance.player.getName().getString());
        };

        if (instance.player.getClass() != ServerPlayerEntity.class) {
            action.accept(null, null);
            return;
        }

        AsyncChunkLoadUtil.scheduleChunkLoad((ServerWorld) instance.player.world, pos).whenCompleteAsync(action, runnable -> server.send(new ServerTask(0, runnable)));
    }

    @Unique
    private void vmp$mountSavedVehicles(ServerPlayerEntity player, NbtCompound playerData) {
        // TODO [VanillaCopy]
        if (playerData != null && playerData.contains("RootVehicle", NbtElement.COMPOUND_TYPE)) {
            NbtCompound nbtCompound2 = playerData.getCompound("RootVehicle");
            ServerWorld world = (ServerWorld) player.world;
            Entity entity = EntityType.loadEntityWithPassengers(
                    nbtCompound2.getCompound("Entity"), world, vehicle -> !world.tryLoadEntity(vehicle) ? null : vehicle
            );
            if (entity != null) {
                UUID uUID;
                if (nbtCompound2.containsUuid("Attach")) {
                    uUID = nbtCompound2.getUuid("Attach");
                } else {
                    uUID = null;
                }

                if (entity.getUuid().equals(uUID)) {
                    player.startRiding(entity, true);
                } else {
                    for(Entity entity2 : entity.getPassengersDeep()) {
                        if (entity2.getUuid().equals(uUID)) {
                            player.startRiding(entity2, true);
                            break;
                        }
                    }
                }

                if (!player.hasVehicle()) {
                    LOGGER.warn("Couldn't reattach entity to player");
                    entity.discard();

                    for(Entity entity2 : entity.getPassengersDeep()) {
                        entity2.discard();
                    }
                }
            }
        }
    }

    @Redirect(method = "onPlayerConnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;onPlayerConnected(Lnet/minecraft/server/network/ServerPlayerEntity;)V"))
    private void delayAddToWorld(ServerWorld instance, ServerPlayerEntity player) {
        // no-op
    }

    @Redirect(method = "onPlayerConnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;sendWorldInfo(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/world/ServerWorld;)V"))
    private void delaySendWorldInfo(PlayerManager instance, ServerPlayerEntity player, ServerWorld world) {
        // no-op
    }

    @Redirect(method = "onPlayerConnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;onSpawn()V"))
    private void delayPlayerSpawn(ServerPlayerEntity instance) {
        // no-op
    }

    @Redirect(method = "onPlayerConnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityType;loadEntityWithPassengers(Lnet/minecraft/nbt/NbtCompound;Lnet/minecraft/world/World;Ljava/util/function/Function;)Lnet/minecraft/entity/Entity;"))
    private @Nullable Entity delayPassengerMount(NbtCompound nbt, World world, Function<Entity, Entity> entityProcessor) {
        return null; // no-op
    }

    @Inject(method = "loadPlayerData", at = @At(value = "RETURN"))
    private void onLoadPlayerData(ServerPlayerEntity player, CallbackInfoReturnable<NbtCompound> cir) {
        ((IAsyncChunkPlayer) player).setPlayerData(cir.getReturnValue());
    }

    @Inject(method = "savePlayerData", at = @At(value = "HEAD"), cancellable = true)
    private void beforeSavePlayerData(ServerPlayerEntity player, CallbackInfo ci) {
        if (!((IAsyncChunkPlayer) player).isChunkLoadCompleted()) ci.cancel();
    }

}
