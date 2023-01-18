package com.ishland.vmp.mixins.networking.priority.sync;

import com.ishland.vmp.common.networking.priority.PacketPriorityHandler;
import com.ishland.vmp.mixins.access.IClientConnection;
import com.ishland.vmp.mixins.access.IServerPlayNetworkHandler;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PlayerManager.class)
public class MixinPlayerManager {

    @Inject(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;onSpawn()V", shift = At.Shift.AFTER))
    private void afterMoveToAnotherWorld(ServerPlayerEntity player, boolean alive, CallbackInfoReturnable<ServerPlayerEntity> cir) {
        final Channel channel = ((IClientConnection) ((IServerPlayNetworkHandler) player.networkHandler).getConnection()).getChannel();
        if (channel == null) {
            //noinspection RedundantStringFormatCall
            System.err.println("VMP: Warning: %s don't have valid channel when teleporting to another dimension, not starting priority handling".formatted(this));
            return;
        }
        if (channel instanceof SocketChannel) {
            channel.pipeline().fireUserEventTriggered(PacketPriorityHandler.START_PRIORITY);
        }
    }

    @Inject(method = "onPlayerConnect", at = @At("RETURN"))
    private void postJoin(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        final Channel channel = ((IClientConnection) connection).getChannel();
        if (channel == null) {
            //noinspection RedundantStringFormatCall
            System.err.println("VMP: Warning: %s don't have valid channel when logged in, not sending sync packet".formatted(this));
            return;
        }
        if (channel instanceof SocketChannel) {
            channel.pipeline().fireUserEventTriggered(PacketPriorityHandler.START_PRIORITY);
        }
    }

}
