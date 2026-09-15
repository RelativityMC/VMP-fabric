package com.ishland.vmp;

import com.ibm.asyncutil.util.Combinators;
import com.ishland.vmp.common.config.Config;
import com.ishland.vmp.common.playerwatching.NearbyEntityTracking;
import net.fabricmc.api.ModInitializer;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.List;

public class VMPMod implements ModInitializer {
    @Override
    public void onInitialize() {
        Combinators.allOf(List.of()).toCompletableFuture(); // check asyncutil existence

        if (Config.USE_OPTIMIZED_ENTITY_TRACKING) {
            NearbyEntityTracking.init();
        }

//        MixinEnvironment.getCurrentEnvironment().audit();
    }
}
