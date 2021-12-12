package com.ishland.vmp.mixins.entity.move_zero_velocity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class MixinEntity {

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void onMove(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        if (movement.equals(Vec3d.ZERO)) ci.cancel();
    }

}
