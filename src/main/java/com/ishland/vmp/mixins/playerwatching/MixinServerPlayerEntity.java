package com.ishland.vmp.mixins.playerwatching;

import com.ishland.vmp.common.chunkwatching.PlayerClientVDTracking;
import net.minecraft.network.packet.c2s.play.ClientSettingsC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity implements PlayerClientVDTracking {

    @Unique
    private boolean vdChanged = false;

    @Unique
    private int clientVD = -1;

    @Inject(method = "setClientSettings", at = @At("HEAD"))
    private void onClientSettingsChanged(ClientSettingsC2SPacket packet, CallbackInfo ci) {
        final int currentVD = packet.viewDistance();
        if (currentVD != this.clientVD) this.vdChanged = true;
        this.clientVD = Math.max(2, currentVD);
    }

    @Unique
    @Override
    public boolean isClientViewDistanceChanged() {
        return this.vdChanged;
    }

    @Unique
    @Override
    public int getClientViewDistance() {
        this.vdChanged = false;
        return this.clientVD;
    }
}
