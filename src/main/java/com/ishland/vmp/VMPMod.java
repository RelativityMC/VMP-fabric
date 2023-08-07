package com.ishland.vmp;

import com.ishland.raknetify.fabric.common.connection.RakNetMultiChannel;
import com.ishland.vmp.common.config.Config;
import com.ishland.vmp.common.playerwatching.NearbyEntityTracking;
import com.ishland.vmp.mixins.access.INetworkState;
import com.ishland.vmp.mixins.access.INetworkStateInternalPacketHandler;
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

        for (Map.Entry<NetworkSide, ? extends NetworkState.InternalPacketHandler<?>> entry : ((INetworkState) (Object) NetworkState.PLAY).getPacketHandlers().entrySet()) {
            for (Object2IntMap.Entry<Class<? extends Packet<?>>> type : ((INetworkStateInternalPacketHandler) ((INetworkStatePacketHandler<?>) entry.getValue()).getBackingHandler()).getPacketIds().object2IntEntrySet()) {
                RakNetMultiChannel.getPacketChannelOverride(type.getKey());
            }
        }

        if (Config.USE_OPTIMIZED_ENTITY_TRACKING) {
            NearbyEntityTracking.init();
        }
    }
}
