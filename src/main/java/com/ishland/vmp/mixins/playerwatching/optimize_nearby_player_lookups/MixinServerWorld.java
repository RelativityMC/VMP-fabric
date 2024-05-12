package com.ishland.vmp.mixins.playerwatching.optimize_nearby_player_lookups;

import com.ishland.vmp.common.chunkwatching.AreaPlayerChunkWatchingManager;
import com.ishland.vmp.common.playerwatching.TACSExtension;
import com.ishland.vmp.mixins.access.IThreadedAnvilChunkStorage;
import io.papermc.paper.util.MCUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.profiler.Profiler;
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

    protected MixinServerWorld(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long biomeAccess, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
    }

    @Shadow public abstract ServerChunkManager getChunkManager();

    @Nullable
    @Override
    public PlayerEntity getClosestPlayer(double x, double y, double z, double maxDistance, @Nullable Predicate<Entity> targetPredicate) {
        final ServerChunkLoadingManager threadedAnvilChunkStorage = this.getChunkManager().chunkLoadingManager;
        final AreaPlayerChunkWatchingManager playerChunkWatchingManager = ((TACSExtension) threadedAnvilChunkStorage).getAreaPlayerChunkWatchingManager();
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
        final Object[] playersWatchingChunkArray = getPlayersWatchingChunkArray(x, z);

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

    private Object[] getPlayersWatchingChunkArray(double x, double z) {
        final ServerChunkLoadingManager threadedAnvilChunkStorage = this.getChunkManager().chunkLoadingManager;
        final AreaPlayerChunkWatchingManager playerChunkWatchingManager = ((TACSExtension) threadedAnvilChunkStorage).getAreaPlayerChunkWatchingManager();
        final int chunkX = ChunkSectionPos.getSectionCoord(x);
        final int chunkZ = ChunkSectionPos.getSectionCoord(z);

        // no maxDistance here so just search within the range,
        // and hopefully it works

        final Object[] playersWatchingChunkArray = playerChunkWatchingManager.getPlayersInGeneralAreaMap(MCUtil.getCoordinateKey(chunkX, chunkZ));
        return playersWatchingChunkArray;
    }

    @Nullable
    @Override
    public PlayerEntity getClosestPlayer(TargetPredicate targetPredicate, double x, double y, double z) {
        return this.getClosestPlayer(targetPredicate, null, x, y, z);
    }

    @Override
    public boolean isPlayerInRange(double x, double y, double z, double range) {
        final ServerChunkLoadingManager threadedAnvilChunkStorage = this.getChunkManager().chunkLoadingManager;
        final AreaPlayerChunkWatchingManager playerChunkWatchingManager = ((TACSExtension) threadedAnvilChunkStorage).getAreaPlayerChunkWatchingManager();
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
