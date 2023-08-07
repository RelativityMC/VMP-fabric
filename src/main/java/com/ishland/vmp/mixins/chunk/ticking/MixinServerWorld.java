package com.ishland.vmp.mixins.chunk.ticking;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerWorld.class)
public class MixinServerWorld {

//    @Redirect(method = "tickChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;getTopPosition(Lnet/minecraft/world/Heightmap$Type;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/BlockPos;"))
//    private BlockPos redirectGetTopPosition(ServerWorld instance, Heightmap.Type type, BlockPos blockPos, WorldChunk chunk, int randomTickSpeed) {
//        return blockPos.withY(chunk.sampleHeightmap(type, blockPos.getX(), blockPos.getZ()) + 1);
//    }

}
