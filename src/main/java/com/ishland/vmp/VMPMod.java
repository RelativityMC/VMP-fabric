package com.ishland.vmp;

import com.ishland.raknetify.fabric.common.connection.RakNetMultiChannel;
import com.ishland.vmp.mixins.access.INetworkState;
import com.ishland.vmp.mixins.access.INetworkStatePacketHandler;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.api.ModInitializer;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.packet.Packet;
import java.util.Map;

public class VMPMod implements ModInitializer {
    @Override
    public void onInitialize() {
        RakNetMultiChannel.init();

        for (Map.Entry<NetworkSide, ? extends NetworkState.PacketHandler<?>> entry : ((INetworkState) (Object) NetworkState.PLAY).getPacketHandlers().entrySet()) {
            for (Object2IntMap.Entry<Class<? extends Packet<?>>> type : ((INetworkStatePacketHandler) entry.getValue()).getPacketIds().object2IntEntrySet()) {
                RakNetMultiChannel.getPacketChannelOverride(type.getKey());
            }
        }
    }
}
