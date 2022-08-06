package com.ishland.vmp.mixins.access;

import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityPositionS2CPacket.class)
public interface IEntityPositionS2CPacket {

    @Accessor("x")
    @Mutable
    void setX(double x);

    @Accessor("y")
    @Mutable
    void setY(double y);

    @Accessor("z")
    @Mutable
    void setZ(double z);

    @Accessor("yaw")
    @Mutable
    void setYaw(byte yaw);

    @Accessor("pitch")
    @Mutable
    void setPitch(byte pitch);

}
