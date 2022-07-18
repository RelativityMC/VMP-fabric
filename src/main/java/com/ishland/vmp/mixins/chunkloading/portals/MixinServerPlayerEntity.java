package com.ishland.vmp.mixins.chunkloading.portals;

import com.ishland.vmp.common.chunkloading.IEntityPortalInterface;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.CompletionStage;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements IEntityPortalInterface {

    public MixinServerPlayerEntity(World world, BlockPos pos, float yaw, GameProfile gameProfile, @Nullable PlayerPublicKey publicKey) {
        super(world, pos, yaw, gameProfile, publicKey);
    }

//    @Unique
//    @Nullable
//    // actually overrides
//    public CompletionStage<TeleportTarget> getTeleportTargetAtAsync(ServerWorld destination) {
//        return IEntityPortalInterface.super.getTeleportTargetAtAsync(destination).thenApply(teleportTarget -> {
//            if (teleportTarget != null && this.world.getRegistryKey() == World.OVERWORLD && destination.getRegistryKey() == World.END) {
//                Vec3d vec3d = teleportTarget.position.add(0.0, -1.0, 0.0);
//                return new TeleportTarget(vec3d, Vec3d.ZERO, 90.0F, 0.0F);
//            } else {
//                return teleportTarget;
//            }
//        });
//    }

}
