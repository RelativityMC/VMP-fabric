package com.ishland.vmp.mixins.playerwatching.optimize_nearby_entity_tracking_lookups;

import com.ishland.vmp.common.playerwatching.EntityTrackerEntryExtension;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityTrackerEntry.class)
public abstract class MixinEntityTrackerEntry implements EntityTrackerEntryExtension {


    @Shadow private int trackingTick;

    @Shadow @Final private int tickInterval;

    @Shadow public abstract void tick();

    @Shadow protected abstract void syncEntityData();

    @Shadow @Final private Entity entity;

    @Shadow private int updatesWithoutVehicle;

    @Override
    public void vmp$tickAlways() {
        this.trackingTick = MathHelper.roundUpToMultiple(this.trackingTick, this.tickInterval);
        this.updatesWithoutVehicle = Integer.MAX_VALUE - 1;
        this.entity.velocityDirty = true;
        this.tick();
    }

    @Override
    public void vmp$syncEntityData() {
        if (this.trackingTick % this.tickInterval == 0) { // [VanillaCopy]
            this.syncEntityData();
        }
    }
}
