package com.ishland.vmp.mixins.access;

import net.minecraft.entity.Entity;
import net.minecraft.entity.TrackedPosition;
import net.minecraft.server.network.EntityTrackerEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(EntityTrackerEntry.class)
public interface IEntityTrackerEntry {

    @Invoker
    void invokeSyncEntityData();

    @Accessor
    Entity getEntity();

    @Accessor
    List<Entity> getLastPassengers();

    @Accessor
    void setLastPassengers(List<Entity> lastPassengers);

    @Accessor
    TrackedPosition getTrackedPos();

}
