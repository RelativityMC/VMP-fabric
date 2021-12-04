package com.ishland.vmp.mixins.general.spawn_density_cap;

import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.SpawnDensityCapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(SpawnDensityCapper.DensityCap.class)
public class MixinSpawnDensityCapperDensityCap {
    private final int[] spawnGroupDensities = new int[SpawnGroup.values().length];

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
