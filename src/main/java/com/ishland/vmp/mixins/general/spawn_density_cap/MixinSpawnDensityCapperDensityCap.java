package com.ishland.vmp.mixins.general.spawn_density_cap;

import com.ishland.vmp.common.general.spawn_density_cap.SpawnDensityCapperDensityCapDelegate;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.SpawnDensityCapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpawnDensityCapper.DensityCap.class)
public class MixinSpawnDensityCapperDensityCap {
    @Mutable
    @Shadow @Final private Object2IntMap<SpawnGroup> spawnGroupsToDensity;
    private final int[] spawnGroupDensities = new int[SpawnGroup.values().length];

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.spawnGroupsToDensity = SpawnDensityCapperDensityCapDelegate.delegateSpawnGroupDensities(spawnGroupDensities);
    }

    /**
     * @author ishland
     * @reason opt: replace with array access
     */
    @Overwrite
    public void increaseDensity(SpawnGroup spawnGroup) {
        this.spawnGroupDensities[spawnGroup.ordinal()] ++;
    }

    /**
     * @author ishland
     * @reason opt: replace with array access
     */
    @Overwrite
    public boolean canSpawn(SpawnGroup spawnGroup) {
        return this.spawnGroupDensities[spawnGroup.ordinal()] < spawnGroup.getCapacity();
    }
}
