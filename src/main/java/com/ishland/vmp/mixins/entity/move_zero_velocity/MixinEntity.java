package com.ishland.vmp.mixins.entity.move_zero_velocity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class MixinEntity {

    @Shadow private Box boundingBox;
    @Unique
    private boolean boundingBoxChanged = false;

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void onMove(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        if (!boundingBoxChanged && movement.equals(Vec3d.ZERO)) {
            ci.cancel();
            boundingBoxChanged = false;
        }
    }

    @Inject(method = "setBoundingBox", at = @At("HEAD"))
    private void onBoundingBoxChanged(Box boundingBox, CallbackInfo ci) {
        if (!this.boundingBox.equals(boundingBox)) boundingBoxChanged = true;
    }

}
