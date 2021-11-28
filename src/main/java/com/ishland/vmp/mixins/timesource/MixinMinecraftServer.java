package com.ishland.vmp.mixins.timesource;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {

    @Redirect(method = "shouldKeepTicking", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMeasuringTimeMs()J"))
    private long redirectTimeSource() {
        return System.nanoTime() / 1000000L;
    }

}
