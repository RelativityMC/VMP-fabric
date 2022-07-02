package com.ishland.vmp;

import com.ishland.raknetify.fabric.common.connection.RakNetMultiChannel;
import net.fabricmc.api.ModInitializer;

public class VMPMod implements ModInitializer {
    @Override
    public void onInitialize() {
        RakNetMultiChannel.init();
    }
}
