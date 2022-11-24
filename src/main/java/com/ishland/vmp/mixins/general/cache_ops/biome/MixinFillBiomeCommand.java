package com.ishland.vmp.mixins.general.cache_ops.biome;

import com.ishland.vmp.common.general.cache_ops.biome.PreloadingBiome;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.FillBiomeCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ReadOnlyChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Mixin(FillBiomeCommand.class)
public abstract class MixinFillBiomeCommand {

    @Shadow
    private static BlockPos convertPos(BlockPos pos) {
        throw new AbstractMethodError();
    }

    @Shadow
    @Final
    public static SimpleCommandExceptionType UNLOADED_EXCEPTION;

    @Inject(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/command/ServerCommandSource;sendFeedback(Lnet/minecraft/text/Text;Z)V"))
    private static void scheduleRefreshBiome(ServerCommandSource source, BlockPos from, BlockPos to, RegistryEntry.Reference<Biome> biome, Predicate<RegistryEntry<Biome>> filter, CallbackInfoReturnable<Integer> cir) {
        BlockPos blockPos = convertPos(from);
        BlockPos blockPos2 = convertPos(to);
        BlockBox blockBox = BlockBox.create(blockPos, blockPos2);

        ServerWorld serverWorld = source.getWorld();
        for (int j = ChunkSectionPos.getSectionCoord(blockBox.getMinZ()); j <= ChunkSectionPos.getSectionCoord(blockBox.getMaxZ()); ++j) {
            for (int k = ChunkSectionPos.getSectionCoord(blockBox.getMinX()); k <= ChunkSectionPos.getSectionCoord(blockBox.getMaxX()); ++k) {
                Chunk chunk = serverWorld.getChunk(k, j, ChunkStatus.BIOMES, false);
                if (chunk == null) {
                    throw new RuntimeException();
                }

                if (chunk instanceof ReadOnlyChunk readOnlyChunk) chunk = readOnlyChunk.getWrappedChunk();

                if (chunk instanceof PreloadingBiome preloadingBiome) {
                    try {
                        final List<Chunk> chunks = new ArrayList<>();
                        for (int x = -1; x <= 1; x++) {
                            for (int z = -1; z <= 1; z++) {
                                chunks.add(serverWorld.getChunk(k + x, j + z, ChunkStatus.BIOMES, false));
                            }
                        }
                        preloadingBiome.vmp$tryReloadBiome(new ChunkRegion(
                                serverWorld,
                                chunks,
                                ChunkStatus.FEATURES,
                                0
                        ));
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        }
    }

}
