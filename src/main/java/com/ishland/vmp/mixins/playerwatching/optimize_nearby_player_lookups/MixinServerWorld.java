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
import net.minecraft.util.registry.RegistryEntry;
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

    protected MixinServerWorld(MutableWorldProperties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> dimension, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) {
        super(properties, registryRef, dimension, profiler, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }

    @Shadow public abstract ServerChunkManager getChunkManager();

    @Nullable
    @Override
    public PlayerEntity getClosestPlayer(double x, double y, double z, double maxDistance, @Nullable Predicate<Entity> targetPredicate) {
        final ThreadedAnvilChunkStorage threadedAnvilChunkStorage = this.getChunkManager().threadedAnvilChunkStorage;
        final AreaPlayerChunkWatchingManager playerChunkWatchingManager = (AreaPlayerChunkWatchingManager) ((IThreadedAnvilChunkStorage) threadedAnvilChunkStorage).getPlayerChunkWatchingManager();
        final int chunkX = ChunkSectionPos.getSectionCoord(x);
        final int chunkZ = ChunkSectionPos.getSectionCoord(z);

        if (AreaPlayerChunkWatchingManager.GENERAL_PLAYER_AREA_MAP_DISTANCE * 16 < maxDistance || maxDistance < 0.0D) // too far away for this to handle
            return super.getClosestPlayer(x, y, z, maxDistance, targetPredicate);

        final Object[] playersWatchingChunkArray = playerChunkWatchingManager.getPlayersInGeneralAreaMap(MCUtil.getCoordinateKey(chunkX, chunkZ));

        ServerPlayerEntity nearestPlayer = null;
        double nearestDistance = maxDistance * maxDistance; // maxDistance < 0.0D handled above
        for (Object __player : playersWatchingChunkArray) {
            if (__player instanceof ServerPlayerEntity player) {
                if (targetPredicate == null || targetPredicate.test(player)) {
                    final double distance = player.squaredDistanceTo(x, y, z);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestPlayer = player;
                    }
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

        final Object[] playersWatchingChunkArray = playerChunkWatchingManager.getPlayersInGeneralAreaMap(MCUtil.getCoordinateKey(chunkX, chunkZ));

        ServerPlayerEntity nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Object __player : playersWatchingChunkArray) {
            if (__player instanceof ServerPlayerEntity player) {
                if (targetPredicate == null || targetPredicate.test(entity, player)) {
                    final double distance = player.squaredDistanceTo(x, y, z);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestPlayer = player;
                    }
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

        if (AreaPlayerChunkWatchingManager.GENERAL_PLAYER_AREA_MAP_DISTANCE * 16 < range) // too far away for this to handle
            return super.isPlayerInRange(x, y, z, range);

        final Object[] playersWatchingChunkArray = playerChunkWatchingManager.getPlayersWatchingChunkArray(MCUtil.getCoordinateKey(chunkX, chunkZ));

        double rangeSquared = range * range;

        for (Object __player : playersWatchingChunkArray) {
            if (__player instanceof ServerPlayerEntity player) {
                if (!player.isSpectator() && player.isAlive()) {
                    final double distance = player.squaredDistanceTo(x, y, z);
                    if (range < 0.0 || distance < rangeSquared) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
