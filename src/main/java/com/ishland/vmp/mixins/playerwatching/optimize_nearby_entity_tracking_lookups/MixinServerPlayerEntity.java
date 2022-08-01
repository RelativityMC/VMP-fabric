package com.ishland.vmp.mixins.playerwatching.optimize_nearby_entity_tracking_lookups;

import com.ishland.vmp.common.playerwatching.ServerPlayerEntityExtension;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements ServerPlayerEntityExtension {

    public MixinServerPlayerEntity(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }

    private double vmpTracking$prevX = Double.NaN;
    private double vmpTracking$prevY = Double.NaN;
    private double vmpTracking$prevZ = Double.NaN;

    @Override
    public boolean vmpTracking$isPositionUpdated() {
        final Vec3d pos = this.getPos();
        return pos.x != this.vmpTracking$prevX || pos.y != this.vmpTracking$prevY || pos.z != this.vmpTracking$prevZ;
    }

    @Override
    public void vmpTracking$updatePosition() {
        final Vec3d pos = this.getPos();
        this.vmpTracking$prevX = pos.x;
        this.vmpTracking$prevY = pos.y;
        this.vmpTracking$prevZ = pos.z;
    }
}
