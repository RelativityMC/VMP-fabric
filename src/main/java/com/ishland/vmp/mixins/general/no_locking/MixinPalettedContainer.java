package com.ishland.vmp.mixins.general.no_locking;

import net.minecraft.world.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(PalettedContainer.class)
public class MixinPalettedContainer {

    /**
     * @author ishland
     * @reason removes locking
     */
    @Overwrite
    public void lock() {
        // no-op
    }

    /**
     * @author ishland
     * @reason removes locking
     */
    @Overwrite
    public void unlock() {
        // no-op
    }

}
