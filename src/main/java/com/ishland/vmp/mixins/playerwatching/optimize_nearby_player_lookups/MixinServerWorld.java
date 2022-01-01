package com.ishland.vmp.mixins.playerwatching.optimize_nearby_player_lookups;

import com.ishland.vmp.common.chunkwatching.AreaPlayerChunkWatchingManager;
import com.ishland.vmp.mixins.access.IThreadedAnvilChunkStorage;
import io.papermc.paper.util.MCUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld extends World implements StructureWorldAccess {

    @Shadow public abstract ServerChunkManager getChunkManager();

    protected MixinServerWorld(MutableWorldProperties properties, RegistryKey<World> registryRef, DimensionType dimensionType, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, dimensionType, profiler, isClient, debugWorld, seed);
    }

    @Nullable
    @Override
    public PlayerEntity getClosestPlayer(double x, double y, double z, double maxDistance, @Nullable Predicate<Entity> targetPredicate) {
        final ThreadedAnvilChunkStorage threadedAnvilChunkStorage = this.getChunkManager().threadedAnvilChunkStorage;
        final AreaPlayerChunkWatchingManager playerChunkWatchingManager = (AreaPlayerChunkWatchingManager) ((IThreadedAnvilChunkStorage) threadedAnvilChunkStorage).getPlayerChunkWatchingManager();
        final int chunkX = ChunkSectionPos.getSectionCoord(x);
        final int chunkZ = ChunkSectionPos.getSectionCoord(z);

        if (playerChunkWatchingManager.getWatchDistance() * 16 < maxDistance) // too far away for this to handle
            return super.getClosestPlayer(x, y, z, maxDistance, targetPredicate);

        final Set<ServerPlayerEntity> playersWatchingChunkArray = playerChunkWatchingManager.getPlayersWatchingChunk(MCUtil.getCoordinateKey(chunkX, chunkZ));

        ServerPlayerEntity nearestPlayer = null;
        double nearestDistance = maxDistance < 0.0 ? Double.MAX_VALUE : maxDistance * maxDistance;
        for (ServerPlayerEntity player : playersWatchingChunkArray) {
            if (targetPredicate == null || targetPredicate.test(player)) {
                final double distance = player.squaredDistanceTo(x, y, z);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestPlayer = player;
                }
            }
        }

        return nearestPlayer;
    }

    @Nullable
    @Override
    public PlayerEntity getClosestPlayer(TargetPredicate targetPredicate, LivingEntity entity) {
        return this.getClosestPlayer(targetPredicate, entity, entity.getX(), entity.getY(), entity.getZ());
    }

    @Nullable
    @Override
    public PlayerEntity getClosestPlayer(TargetPredicate targetPredicate, LivingEntity entity, double x, double y, double z) {
        final ThreadedAnvilChunkStorage threadedAnvilChunkStorage = this.getChunkManager().threadedAnvilChunkStorage;
        final AreaPlayerChunkWatchingManager playerChunkWatchingManager = (AreaPlayerChunkWatchingManager) ((IThreadedAnvilChunkStorage) threadedAnvilChunkStorage).getPlayerChunkWatchingManager();
        final int chunkX = ChunkSectionPos.getSectionCoord(x);
        final int chunkZ = ChunkSectionPos.getSectionCoord(z);

        // no maxDistance here so just search within the range,
        // and hopefully it works

        final Set<ServerPlayerEntity> playersWatchingChunkArray = playerChunkWatchingManager.getPlayersWatchingChunk(MCUtil.getCoordinateKey(chunkX, chunkZ));

        ServerPlayerEntity nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;
        for (ServerPlayerEntity player : playersWatchingChunkArray) {
            if (targetPredicate == null || targetPredicate.test(entity, player)) {
                final double distance = player.squaredDistanceTo(x, y, z);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestPlayer = player;
                }
            }
        }

        return nearestPlayer;
    }

    @Nullable
    @Override
    public PlayerEntity getClosestPlayer(TargetPredicate targetPredicate, double x, double y, double z) {
        return this.getClosestPlayer(targetPredicate, null, x, y, z);
    }

    @Override
    public boolean isPlayerInRange(double x, double y, double z, double range) {
        final ThreadedAnvilChunkStorage threadedAnvilChunkStorage = this.getChunkManager().threadedAnvilChunkStorage;
        final AreaPlayerChunkWatchingManager playerChunkWatchingManager = (AreaPlayerChunkWatchingManager) ((IThreadedAnvilChunkStorage) threadedAnvilChunkStorage).getPlayerChunkWatchingManager();
        final int chunkX = ChunkSectionPos.getSectionCoord(x);
        final int chunkZ = ChunkSectionPos.getSectionCoord(z);

        if (playerChunkWatchingManager.getWatchDistance() * 16 < range) // too far away for this to handle
            return super.isPlayerInRange(x, y, z, range);

        final Set<ServerPlayerEntity> playerWatchingChunkSet = playerChunkWatchingManager.getPlayersWatchingChunk(MCUtil.getCoordinateKey(chunkX, chunkZ));
        return playerWatchingChunkSet != null && !playerWatchingChunkSet.isEmpty();
    }
}
