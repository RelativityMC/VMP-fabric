package com.ishland.vmp.mixins.access;

import net.minecraft.entity.TrackedPosition;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TrackedPosition.class)
public interface ITrackedPosition {

    @Accessor
    Vec3d getPos();

}
