package com.ishland.vmp.mixins.carpet;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "carpet.patches.EntityPlayerMPFake")
public abstract class MixinEntityPlayerMPFake extends ServerPlayerEntity {

    public MixinEntityPlayerMPFake(MinecraftServer server, ServerWorld world, GameProfile profile, SyncedClientOptions arg) {
        super(server, world, profile, arg);
    }

    @Unique private double vmp_lastX = Double.NaN;
    @Unique private double vmp_lastY = Double.NaN;
    @Unique private double vmp_lastZ = Double.NaN;

    @SuppressWarnings("DefaultAnnotationParam")
    @Dynamic
    @Redirect(method = {"tick", "method_5773"}, remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkManager;updatePosition(Lnet/minecraft/server/network/ServerPlayerEntity;)V", remap = true))
    private void redirectUpdatePosition(ServerChunkManager serverChunkManager, ServerPlayerEntity __unused) {
        final Vec3d pos = this.getPos();
        if (pos.x != vmp_lastX || pos.y != vmp_lastY || pos.z != vmp_lastZ) { // only do update when position changes
            vmp_lastX = pos.x;
            vmp_lastY = pos.y;
            vmp_lastZ = pos.z;
            serverChunkManager.updatePosition(this);
        }
    }

}
