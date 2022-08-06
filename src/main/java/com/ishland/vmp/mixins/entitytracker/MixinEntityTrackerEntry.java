package com.ishland.vmp.mixins.entitytracker;

import com.ishland.vmp.mixins.access.IEntityPositionS2CPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TrackedPosition;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(EntityTrackerEntry.class)
public class MixinEntityTrackerEntry {

    @Shadow @Final private Entity entity;

    @Shadow @Final private TrackedPosition field_39019;

    @Shadow private int lastYaw;

    @Shadow private int lastPitch;

    @Inject(method = "sendPackets", at = @At("RETURN"))
    private void afterInitialPacket(Consumer<Packet<?>> sender, CallbackInfo ci) {
        final EntityPositionS2CPacket packet = new EntityPositionS2CPacket(this.entity);
        final Vec3d vec3d = this.field_39019.withDelta(0, 0, 0);
        ((IEntityPositionS2CPacket) packet).setX(vec3d.x);
        ((IEntityPositionS2CPacket) packet).setY(vec3d.y);
        ((IEntityPositionS2CPacket) packet).setZ(vec3d.z);
        ((IEntityPositionS2CPacket) packet).setYaw((byte) this.lastYaw);
        ((IEntityPositionS2CPacket) packet).setPitch((byte) this.lastPitch);
        sender.accept(packet);
    }

}
