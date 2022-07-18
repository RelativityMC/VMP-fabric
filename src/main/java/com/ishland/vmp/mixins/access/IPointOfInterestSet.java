package com.ishland.vmp.mixins.access;

import net.minecraft.world.poi.PointOfInterestSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PointOfInterestSet.class)
public interface IPointOfInterestSet {

    @Invoker
    boolean invokeIsValid();

}
