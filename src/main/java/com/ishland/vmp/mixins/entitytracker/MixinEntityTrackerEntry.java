package com.ishland.vmp.mixins.entitytracker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.TrackedPosition;
import net.minecraft.server.network.EntityTrackerEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityTrackerEntry.class)
public class MixinEntityTrackerEntry {
    @Shadow private int updatesWithoutVehicle;

    @Shadow @Final private TrackedPosition trackedPos;

    @Shadow @Final private Entity entity;

    @Inject(method = "startTracking", at = @At("RETURN"))
    private void onStartTracking(CallbackInfo ci) {
        this.updatesWithoutVehicle = Integer.MAX_VALUE - 1;
        this.entity.velocityDirty = true;
    }

}
