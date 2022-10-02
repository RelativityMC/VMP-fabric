package com.ishland.vmp.mixins.playerwatching.optimize_nearby_player_lookups;

import com.ishland.vmp.common.chunkwatching.AreaPlayerChunkWatchingManager;
import com.ishland.vmp.mixins.access.IThreadedAnvilChunkStorage;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.SpawnDensityCapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Mixin(value = SpawnDensityCapper.class, priority = 950)
public abstract class MixinSpawnDensityCapper {

    @Shadow
    @Final
    private ThreadedAnvilChunkStorage threadedAnvilChunkStorage;

    @Mutable
    @Shadow @Final private Map<ServerPlayerEntity, SpawnDensityCapper.DensityCap> playersToDensityCap;
    private static final Function<ServerPlayerEntity, SpawnDensityCapper.DensityCap> newDensityCap = ignored -> new SpawnDensityCapper.DensityCap();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {
        this.playersToDensityCap = new Object2ReferenceOpenHashMap<>();
    }

    @Unique
    private Set<ServerPlayerEntity> getMobSpawnablePlayersSet(ChunkPos chunkPos) {
        final AreaPlayerChunkWatchingManager manager = (AreaPlayerChunkWatchingManager) ((IThreadedAnvilChunkStorage) this.threadedAnvilChunkStorage).getPlayerChunkWatchingManager();
        return manager.getPlayersWatchingChunk(chunkPos.toLong());
    }

    /**
     * @author ishland
     * @reason optimize & reduce allocations
     */
    @Overwrite
    public void increaseDensity(ChunkPos chunkPos, SpawnGroup spawnGroup) {
        for(ServerPlayerEntity serverPlayerEntity : this.getMobSpawnablePlayersSet(chunkPos)) {
            this.playersToDensityCap.computeIfAbsent(serverPlayerEntity, newDensityCap).increaseDensity(spawnGroup);
        }
    }

    /**
     * @author ishland
     * @reason optimize & reduce allocations
     */
    @Overwrite
    public boolean canSpawn(SpawnGroup spawnGroup, ChunkPos chunkPos) {
        for(ServerPlayerEntity serverPlayerEntity : this.getMobSpawnablePlayersSet(chunkPos)) {
            SpawnDensityCapper.DensityCap densityCap = this.playersToDensityCap.get(serverPlayerEntity);
            if (densityCap == null || densityCap.canSpawn(spawnGroup)) {
                return true;
            }
        }

        return false;
    }

}
