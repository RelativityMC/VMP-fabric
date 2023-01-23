package com.ishland.vmp.mixins.general.biome_access;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BiomeAccess.class)
public class MixinBiomeAccess {

    @Shadow @Final private long seed;

    @Shadow @Final private BiomeAccess.Storage storage;

    /**
     * @author ishland
     * @reason optimize
     */
    @Overwrite
    public RegistryEntry<Biome> getBiome(BlockPos pos) {
        final int var0 = pos.getX() - 2;
        final int var1 = pos.getY() - 2;
        final int var2 = pos.getZ() - 2;
        final int var3 = var0 >> 2;
        final int var4 = var1 >> 2;
        final int var5 = var2 >> 2;
        final double var6 = (double) (var0 & 3) / 4.0;
        final double var7 = (double) (var1 & 3) / 4.0;
        final double var8 = (double) (var2 & 3) / 4.0;
        int var9 = 0;
        double var10 = Double.POSITIVE_INFINITY;

        for (int var11 = 0; var11 < 8; ++var11) {
            boolean var12 = (var11 & 4) == 0;
            boolean var13 = (var11 & 2) == 0;
            boolean var14 = (var11 & 1) == 0;
            long var15 = var12 ? var3 : var3 + 1;
            long var16 = var13 ? var4 : var4 + 1;
            long var17 = var14 ? var5 : var5 + 1;
            double var18 = var12 ? var6 : var6 - 1.0;
            double var19 = var13 ? var7 : var7 - 1.0;
            double var20 = var14 ? var8 : var8 - 1.0;
            long var21 = this.seed * (this.seed * 6364136223846793005L + 1442695040888963407L) + var15;
            var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + var16;
            var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + var17;
            var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + var15;
            var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + var16;
            var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + var17;
            double var22 = (double)((var21 >> 24) & 1023) / 1024.0;
            double var23 = (var22 - 0.5) * 0.9;
            var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + this.seed;
            double var24 = (double)((var21 >> 24) & 1023) / 1024.0;
            double var25 = (var24 - 0.5) * 0.9;
            var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + this.seed;
            double var26 = (double)((var21 >> 24) & 1023) / 1024.0;
            double var27 = (var26 - 0.5) * 0.9;
            double var28 = MathHelper.square(var20 + var27) + MathHelper.square(var19 + var25) + MathHelper.square(var18 + var23);
            if (var10 > var28) {
                var9 = var11;
                var10 = var28;
            }
        }

        int resX = (var9 & 4) == 0 ? var3 : var3 + 1;
        int resY = (var9 & 2) == 0 ? var4 : var4 + 1;
        int resZ = (var9 & 1) == 0 ? var5 : var5 + 1;

        return this.storage.getBiomeForNoiseGen(resX, resY, resZ);
    }

}
