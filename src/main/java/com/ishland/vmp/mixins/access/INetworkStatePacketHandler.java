package com.ishland.vmp.mixins.access;

import net.minecraft.network.NetworkState;
import net.minecraft.network.listener.PacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NetworkState.PacketHandler.class)
public interface INetworkStatePacketHandler<T extends PacketListener> {

    @Accessor
    NetworkState.InternalPacketHandler<T> getBackingHandler();

}
