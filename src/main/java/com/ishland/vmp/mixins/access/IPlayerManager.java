package com.ishland.vmp.mixins.access;

import net.minecraft.server.PlayerManager;
import net.minecraft.world.PlayerSaveHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerManager.class)
public interface IPlayerManager {

    @Accessor("saveHandler")
    PlayerSaveHandler getSaveHandler();

}
