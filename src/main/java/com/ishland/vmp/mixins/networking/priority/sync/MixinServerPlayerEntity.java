package com.ishland.vmp.mixins.networking.priority.sync;

import com.ishland.vmp.common.networking.priority.PacketPriorityHandler;
import com.ishland.vmp.mixins.access.IClientConnection;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity {

    @Shadow public ServerPlayNetworkHandler networkHandler;

    @Inject(method = "teleport", at = @At(value = "NEW", target = "net/minecraft/network/packet/s2c/play/PlayerRespawnS2CPacket", shift = At.Shift.BEFORE))
    private void beforeTeleportToAnotherDimension(CallbackInfo ci) {
        vmp$doPrioritySync();
    }

    @Inject(method = "teleport", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;sendPlayerStatus(Lnet/minecraft/server/network/ServerPlayerEntity;)V", shift = At.Shift.AFTER))
    private void afterTeleportToAnotherDimension(CallbackInfo ci) {
        vmp$startPriorityHandler();
    }

    @Inject(method = "moveToWorld", at = @At(value = "NEW", target = "net/minecraft/network/packet/s2c/play/PlayerRespawnS2CPacket", shift = At.Shift.BEFORE))
    private void beforeMoveToAnotherWorld(CallbackInfoReturnable<Entity> cir) {
        vmp$doPrioritySync();
    }

    @Inject(method = "moveToWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;sendPlayerStatus(Lnet/minecraft/server/network/ServerPlayerEntity;)V", shift = At.Shift.AFTER))
    private void afterMoveToAnotherWorld(CallbackInfoReturnable<Entity> cir) {
        vmp$startPriorityHandler();
    }

    @Unique
    private void vmp$doPrioritySync() {
        final Channel channel = ((IClientConnection) this.networkHandler.connection).getChannel();
        if (channel == null) {
            //noinspection RedundantStringFormatCall
            System.err.println("VMP: Warning: %s don't have valid channel when teleporting to another dimension, not sending sync packet".formatted(this));
            return;
        }
        if (channel instanceof SocketChannel) {
            channel.pipeline().fireUserEventTriggered(PacketPriorityHandler.SYNC_REQUEST_OBJECT);
        }
    }

    private void vmp$startPriorityHandler() {
        final Channel channel = ((IClientConnection) this.networkHandler.connection).getChannel();
        if (channel == null) {
            //noinspection RedundantStringFormatCall
            System.err.println("VMP: Warning: %s don't have valid channel when teleporting to another dimension, not starting priority handling".formatted(this));
            return;
        }
        if (channel instanceof SocketChannel) {
            channel.pipeline().fireUserEventTriggered(PacketPriorityHandler.START_PRIORITY);
        }
    }

}
