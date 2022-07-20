package com.ishland.vmp.mixins.chunkloading.portals;

import com.ibm.asyncutil.iteration.AsyncIterator;
import com.ishland.vmp.common.chunkloading.IEntityPortalInterface;
import com.ishland.vmp.common.chunkloading.IPOIAsyncPreload;
import com.ishland.vmp.common.chunkloading.async_chunks_on_player_login.AsyncChunkLoadUtil;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.BlockLocating;
import net.minecraft.world.Heightmap;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.dimension.AreaHelper;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Mixin(Entity.class)
public abstract class MixinEntity implements IEntityPortalInterface {

    @Unique
    private static final CompletableFuture<TeleportTarget> TARGET_COMPLETED_FUTURE = CompletableFuture.completedFuture(null);

    @Shadow
    protected boolean inNetherPortal;

    @Shadow
    public World world;

    @Shadow
    public abstract boolean hasVehicle();

    @Shadow
    public abstract double getX();

    @Shadow
    public abstract double getY();

    @Shadow
    public abstract double getZ();

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    protected abstract void tickNetherPortal();

    @Shadow
    protected int netherPortalTime;

    @Shadow
    public abstract int getMaxNetherPortalTime();

    @Shadow
    @Nullable
    protected abstract TeleportTarget getTeleportTarget(ServerWorld destination);

    @Shadow
    public abstract @Nullable Entity moveToWorld(ServerWorld destination);

    @Shadow
    public abstract void resetNetherPortalCooldown();

    @Shadow
    protected abstract void tickNetherPortalCooldown();

    @Shadow
    protected BlockPos lastNetherPortalPosition;

    @Shadow
    protected abstract Vec3d positionInPortal(Direction.Axis portalAxis, BlockLocating.Rectangle portalRect);

    @Shadow
    public abstract EntityDimensions getDimensions(EntityPose pose);

    @Shadow
    public abstract EntityPose getPose();

    @Shadow
    public abstract Vec3d getVelocity();

    @Shadow
    public abstract float getYaw();

    @Shadow
    public abstract float getPitch();

    @Shadow
    public abstract boolean isRemoved();

    @Shadow
    public abstract void detach();

    @Shadow
    public abstract EntityType<?> getType();

    @Unique
    private CompletableFuture<TeleportTarget> vmp$locatePortalFuture;

    @Unique
    private CompletableFuture<TeleportTarget> vmp$lastLocateFuture = TARGET_COMPLETED_FUTURE;

    @Unique
    private long vmp$locateIndex = 0;

    @Inject(method = "tickNetherPortal", at = @At("HEAD"))
    private void onTickNetherPortal(CallbackInfo ci) {
        if (this.world.isClient) return;
        //noinspection ConstantConditions
        if ((Object) this instanceof ServerPlayerEntity) {
            if (this.inNetherPortal && this.netherPortalTime >= this.getMaxNetherPortalTime() - 50) {
                if (vmp$locatePortalFuture == null && vmp$lastLocateFuture.isDone()) {
                    MinecraftServer minecraftServer = this.world.getServer();
                    RegistryKey<World> registryKey = this.world.getRegistryKey() == World.NETHER ? World.OVERWORLD : World.NETHER;
                    ServerWorld destination = minecraftServer.getWorld(registryKey);
                    long currentLocateIndex = ++vmp$locateIndex;
                    long startTime = System.nanoTime();
                    if ((Object) this instanceof ServerPlayerEntity player) {
                        player.sendMessage(Text.of("Locating portal destination..."), true);
                    }
                    vmp$lastLocateFuture = vmp$locatePortalFuture =
                            getTeleportTargetAtAsync(destination)
                                    .thenComposeAsync(target -> {
                                        if (target != null) {
                                            return AsyncChunkLoadUtil.scheduleChunkLoadWithRadius(destination, new ChunkPos(new BlockPos(target.position)), 3)
                                                    .thenApply(unused -> target);
                                        } else {
                                            return CompletableFuture.completedFuture(null);
                                        }
                                    }, destination.getServer())
                                    .whenCompleteAsync((target, throwable) -> {
                                        if (currentLocateIndex != vmp$locateIndex) return;
                                        if (throwable != null) {
                                            LOGGER.error("Error occurred for entity {} while locating portal", this, throwable);
                                            if ((Object) this instanceof ServerPlayerEntity player) {
                                                player.sendMessage(Text.of("Error occurred while locating portal"), true);
                                            }
                                        } else if (target != null) {
                                            LOGGER.info("Portal located for entity {} at {}", this, target);
                                            final BlockPos blockPos = new BlockPos(target.position);
                                            if ((Object) this instanceof ServerPlayerEntity player) {
                                                player.sendMessage(Text.of("Portal located after %.1fms, waiting for portal teleportation...".formatted((System.nanoTime() - startTime) / 1_000_000.0)), true);
                                            }
                                        } else {
                                            LOGGER.info("Portal not located for entity {} at {}", this, target);
                                            if ((Object) this instanceof ServerPlayerEntity player) {
                                                player.sendMessage(Text.of("Portal not located"), true);
                                            }
                                        }
                                    }, destination.getServer())
                                    .toCompletableFuture();
                }
            } else {
                if (vmp$locatePortalFuture != null) {
                    final boolean done = vmp$locatePortalFuture.isDone();
                    vmp$locatePortalFuture = null;
                    vmp$locateIndex++;
                    if (!done) {
                        if ((Object) this instanceof ServerPlayerEntity player) {
                            player.sendMessage(Text.of("Portal location cancelled"), true);
                        }
                    }
                }
            }
        }
    }

    @Redirect(method = "tickNetherPortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getMaxNetherPortalTime()I"))
    private int redirectMaxPortalTime(Entity instance) {
        if (instance instanceof ServerPlayerEntity) {
            return (vmp$locatePortalFuture != null && vmp$locatePortalFuture.isDone()) ? instance.getMaxNetherPortalTime() : Integer.MAX_VALUE;
        }
        return instance.getMaxNetherPortalTime();
    }

    @Inject(method = "getTeleportTarget", at = @At("HEAD"), cancellable = true)
    private void beforeGetTeleportTarget(ServerWorld destination, CallbackInfoReturnable<TeleportTarget> cir) {
        if (this.vmp$locatePortalFuture != null && this.vmp$locatePortalFuture.isDone()) {
            cir.setReturnValue(this.vmp$locatePortalFuture.join());
        }
    }

    @Unique
    public CompletionStage<TeleportTarget> getTeleportTargetAtAsync(ServerWorld destination) {
        // TODO [VanillaCopy]
        boolean bl = this.world.getRegistryKey() == World.END && destination.getRegistryKey() == World.OVERWORLD;
        boolean bl2 = destination.getRegistryKey() == World.END;
        if (!bl && !bl2) {
            boolean bl3 = destination.getRegistryKey() == World.NETHER;
            if (this.world.getRegistryKey() != World.NETHER && !bl3) {
                return CompletableFuture.completedFuture(null);
            } else {
                WorldBorder worldBorder = destination.getWorldBorder();
                double d = DimensionType.getCoordinateScaleFactor(this.world.getDimension(), destination.getDimension());
                BlockPos destPos = worldBorder.clamp(this.getX() * d, this.getY(), this.getZ() * d);
                return this.getPortalRectAtAsync(destination, destPos, bl3, worldBorder)
                        .thenComposeAsync((Optional<BlockLocating.Rectangle> optional) -> {
                            if ((Object) this instanceof ServerPlayerEntity && optional.isEmpty()) {
                                return AsyncChunkLoadUtil.scheduleChunkLoadWithRadius(destination, new ChunkPos(destPos), 3)
                                        .thenApply(unused1 -> {
                                            Direction.Axis axis = this.world.getBlockState(this.lastNetherPortalPosition).getOrEmpty(NetherPortalBlock.AXIS).orElse(Direction.Axis.X);
                                            Optional<BlockLocating.Rectangle> optional2 = destination.getPortalForcer().createPortal(destPos, axis);
                                            if (!optional2.isPresent()) {
                                                LOGGER.error("Unable to create a portal, likely target out of worldborder");
                                            }
                                            return optional2;
                                        });
                            } else {
                                return CompletableFuture.completedFuture(optional);
                            }
                        }, destination.getServer())
                        .thenComposeAsync(optional -> optional.map(rect ->
                                        AsyncChunkLoadUtil.scheduleChunkLoadWithRadius(destination, new ChunkPos(this.lastNetherPortalPosition), 3)
                                                .thenComposeAsync(unused -> {
                                                    BlockState blockState = this.world.getBlockState(this.lastNetherPortalPosition);
                                                    Direction.Axis axis;
                                                    Vec3d vec3d;
                                                    if (blockState.contains(Properties.HORIZONTAL_AXIS)) {
                                                        axis = blockState.get(Properties.HORIZONTAL_AXIS);
                                                        BlockLocating.Rectangle rectangle = BlockLocating.getLargestRectangle(
                                                                this.lastNetherPortalPosition, axis, 21, Direction.Axis.Y, 21, pos -> this.world.getBlockState(pos) == blockState
                                                        );
                                                        vec3d = this.positionInPortal(axis, rectangle);
                                                    } else {
                                                        axis = Direction.Axis.X;
                                                        vec3d = new Vec3d(0.5, 0.0, 0.0);
                                                    }

                                                    return AsyncChunkLoadUtil.scheduleChunkLoadWithRadius(destination, new ChunkPos(rect.lowerLeft), 3)
                                                            .thenApplyAsync(unused1 -> AreaHelper.getNetherTeleportTarget(
                                                                            destination, rect, axis, vec3d, this.getDimensions(this.getPose()), this.getVelocity(), this.getYaw(), this.getPitch()),
                                                                    destination.getServer());
                                                }, destination.getServer()))
                                .orElse(CompletableFuture.completedFuture(null)), destination.getServer());
            }
        } else {
            BlockPos blockPos;
            if (bl2) {
                blockPos = ServerWorld.END_SPAWN_POS;
            } else {
                blockPos = destination.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, destination.getSpawnPos());
            }

            return CompletableFuture.completedFuture(new TeleportTarget(
                    new Vec3d((double) blockPos.getX() + 0.5, blockPos.getY(), (double) blockPos.getZ() + 0.5), this.getVelocity(), this.getYaw(), this.getPitch()
            ));
        }
    }

    @Unique
    public CompletionStage<Optional<BlockLocating.Rectangle>> getPortalRectAtAsync(ServerWorld destination, BlockPos destPos, boolean destIsNether, WorldBorder worldBorder) {
        PointOfInterestStorage pointOfInterestStorage = destination.getPointOfInterestStorage();
        int i = destIsNether ? 16 : 128;
        return ((CompletionStage<Void>) ((IPOIAsyncPreload) pointOfInterestStorage).preloadChunksAtAsync(destination, destPos, i))
                .thenComposeAsync(unused -> {
                    final Iterator<PointOfInterest> iterator = pointOfInterestStorage.getInSquare(
                                    poiType -> poiType == PointOfInterestType.NETHER_PORTAL,
                                    destPos, i, PointOfInterestStorage.OccupationStatus.ANY
                            )
                            .filter(poi -> worldBorder.contains(poi.getPos()))
                            .sorted(Comparator.comparingDouble((PointOfInterest poi) -> poi.getPos().getSquaredDistance(destPos)).thenComparingInt(poi -> poi.getPos().getY()))
                            .toList().iterator();
                    return AsyncIterator
                            .fromIterator(iterator)
                            .filterCompose(poi -> AsyncChunkLoadUtil.scheduleChunkLoadWithRadius(destination, new ChunkPos(poi.getPos()), 0)
                                    .thenApplyAsync(either -> either.map(
                                            chunk -> chunk.getBlockState(poi.getPos()).contains(Properties.HORIZONTAL_AXIS) ? Optional.of(poi) : Optional.empty(),
                                            unloaded -> {
                                                throw new IllegalStateException();
                                            }
                                    ), destination.getServer())
                            )
                            .take(1)
                            .thenComposeAsync(poi -> {
                                BlockPos blockPos = poi.getPos();
                                return AsyncChunkLoadUtil.scheduleChunkLoadWithRadius(destination, new ChunkPos(blockPos), 3)
                                        .thenApplyAsync(unused1 -> {
                                            destination.getChunkManager().addTicket(ChunkTicketType.PORTAL, new ChunkPos(blockPos), 3, blockPos); // for vanilla behavior
                                            BlockState blockState = destination.getBlockState(blockPos);
                                            return BlockLocating.getLargestRectangle(
                                                    blockPos, blockState.get(Properties.HORIZONTAL_AXIS), 21, Direction.Axis.Y, 21, posx -> destination.getBlockState(posx) == blockState
                                            );
                                        }, destination.getServer());
                            }, destination.getServer())
                            .collect(Collectors.toCollection(() -> new ReferenceArrayList<>(1)))
                            .thenApply(list -> list.isEmpty() ? Optional.empty() : Optional.of(list.get(0)));
                }, destination.getServer());
    }

}
