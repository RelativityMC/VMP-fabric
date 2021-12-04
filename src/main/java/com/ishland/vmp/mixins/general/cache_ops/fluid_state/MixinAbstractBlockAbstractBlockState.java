package com.ishland.vmp.mixins.general.cache_ops.fluid_state;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class MixinAbstractBlockAbstractBlockState {

    @Shadow
    public abstract Block getBlock();

    @Shadow
    protected abstract BlockState asBlockState();

    @Unique
    private FluidState cachedFluidState;

    /**
     * @author ishland
     * @reason cache fluid state
     */
    @SuppressWarnings("deprecation")
    @Overwrite
    public FluidState getFluidState() {
        if (this.cachedFluidState == null) {
            return this.cachedFluidState = this.getBlock().getFluidState(this.asBlockState());
        }
        return this.cachedFluidState;
    }

}
