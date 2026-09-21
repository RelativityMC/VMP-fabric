package com.ishland.vmp.mixins.access;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PlayerSaveHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;

@Mixin(PlayerSaveHandler.class)
public interface IPlayerSaveHandler {

    @Invoker("loadPlayerData")
    Optional<NbtCompound> invokeLoadPlayerData(PlayerEntity player, String extension);

}
