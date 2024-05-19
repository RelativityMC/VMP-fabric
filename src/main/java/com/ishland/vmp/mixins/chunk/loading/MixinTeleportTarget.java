package com.ishland.vmp.mixins.chunk.loading;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TeleportTarget.class)
public class MixinTeleportTarget {

    @Shadow @Final public float pitch;

    @Shadow @Final public Vec3d pos;

    @Shadow @Final public Vec3d velocity;

    @Shadow @Final public float yaw;

    @Override
    public String toString() {
        return "TeleportTarget{" +
               "pitch=" + pitch +
               ", pos=" + pos +
               ", velocity=" + velocity +
               ", yaw=" + yaw +
               '}';
    }
}
