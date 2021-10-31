package com.ishland.leafticket.mixins;

import com.ishland.leafticket.mixins.access.IChunkHolder;
import com.ishland.leafticket.mixins.access.IThreadedAnvilChunkStorage;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTaskPrioritySystem;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.collection.SortedArraySet;
import net.minecraft.util.thread.MessageListener;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ChunkTicketManager.class)
public abstract class MixinChunkTicketManager {

    @Mutable
    @Shadow @Final private ChunkTicketManager.TicketDistanceLevelPropagator distanceFromTicketTracker;

    @Shadow protected @Nullable abstract ChunkHolder getChunkHolder(long pos);

    @Shadow protected @Nullable abstract ChunkHolder setLevel(long pos, int level, @Nullable ChunkHolder holder, int i);

    @Shadow @Final private ChunkTicketManager.DistanceFromNearestPlayerTracker distanceFromNearestPlayerTracker;
    @Shadow @Final private ChunkTicketManager.NearbyChunkTicketUpdater nearbyChunkTicketUpdater;
    @Shadow @Final private Set<ChunkHolder> chunkHolders;
    @Shadow @Final private Executor mainThreadExecutor;
    @Shadow @Final private LongSet chunkPositions;

    @Shadow protected abstract SortedArraySet<ChunkTicket<?>> getTicketSet(long position);

    @Shadow @Final private MessageListener<ChunkTaskPrioritySystem.UnblockingMessage> playerTicketThrottlerUnblocker;
    // Paper start - replace ticket level propagator
    @Unique
    protected Long2IntLinkedOpenHashMap ticketLevelUpdates;

    @Unique
    protected io.papermc.paper.util.misc.Delayed8WayDistancePropagator2D ticketLevelPropagator;

    // function for converting between ticket levels and propagator levels and vice versa
    // the problem is the ticket level propagator will propagate from a set source down to zero, whereas mojang expects
    // levels to propagate from a set value up to a maximum value. so we need to convert the levels we put into the propagator
    // and the levels we get out of the propagator

    // this maps so that GOLDEN_TICKET + 1 will be 0 in the propagator, GOLDEN_TICKET will be 1, and so on
    // we need GOLDEN_TICKET+1 as 0 because anything >= GOLDEN_TICKET+1 should be unloaded
    @Unique
    private static int convertBetweenTicketLevels(final int level) {
        return ThreadedAnvilChunkStorage.MAX_LEVEL - level + 1;
    }

    @Unique
    protected final int getPropagatedTicketLevel(final long coordinate) {
        return convertBetweenTicketLevels(this.ticketLevelPropagator.getLevel(coordinate));
    }

    @Unique
    protected final void updateTicketLevel(final long coordinate, final int ticketLevel) {
        if (ticketLevel > ThreadedAnvilChunkStorage.MAX_LEVEL) {
            this.ticketLevelPropagator.removeSource(coordinate);
        } else {
            this.ticketLevelPropagator.setSource(coordinate, convertBetweenTicketLevels(ticketLevel));
        }
    }
    // Paper end - replace ticket level propagator

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(Executor workerExecutor, Executor mainThreadExecutor, CallbackInfo ci) {
        this.distanceFromTicketTracker = null; // fail-fast incompatibility

        // Paper start - replace ticket level propagator
        this.ticketLevelUpdates = new Long2IntLinkedOpenHashMap() {
            @Override
            protected void rehash(int newN) {
                // no downsizing allowed
                if (newN < this.n) {
                    return;
                }
                super.rehash(newN);
            }
        };
        this.ticketLevelPropagator = new io.papermc.paper.util.misc.Delayed8WayDistancePropagator2D(
                (long coordinate, byte oldLevel, byte newLevel) -> {
                    this.ticketLevelUpdates.putAndMoveToLast(coordinate, convertBetweenTicketLevels(newLevel));
                }
        );
        // Paper end - replace ticket level propagator
    }

    @Redirect(method = {"purge", "addTicket(JLnet/minecraft/server/world/ChunkTicket;)V", "removeTicket(JLnet/minecraft/server/world/ChunkTicket;)V"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ChunkTicketManager$TicketDistanceLevelPropagator;updateLevel(JIZ)V"), require = 3, expect = 3)
    private void redirectUpdate(ChunkTicketManager.TicketDistanceLevelPropagator instance, long l, int i, boolean b) {
        this.updateTicketLevel(l, i); // Paper - replace ticket level propagator
    }

    @Unique
    protected long ticketLevelUpdateCount; // Paper - replace ticket level propagator

    /**
     * @author ishland
     * @reason reimplement
     */
    @Overwrite
    public boolean tick(ThreadedAnvilChunkStorage threadedAnvilChunkStorage) {
        this.distanceFromNearestPlayerTracker.updateLevels();
        this.nearbyChunkTicketUpdater.updateLevels();
//        int i = Integer.MAX_VALUE - this.distanceFromTicketTracker.update(Integer.MAX_VALUE);
        boolean flag = this.ticketLevelPropagator.propagateUpdates();
        if (flag) {
        }

        // Paper start - replace level propagator - modified
        while (!this.ticketLevelUpdates.isEmpty()) {
            flag = true;

//            boolean oldPolling = this.pollingPendingChunkUpdates;
//            this.pollingPendingChunkUpdates = true;
            try {
//                long recursiveCheck = ++this.ticketLevelUpdateCount;
                while (!this.ticketLevelUpdates.isEmpty()) {
                    long key = this.ticketLevelUpdates.firstLongKey();
                    int newLevel = this.ticketLevelUpdates.removeFirstInt();

                    ChunkHolder holder = this.getChunkHolder(key);
                    int currentLevel = holder == null ? ThreadedAnvilChunkStorage.MAX_LEVEL + 1 : holder.getLevel();

                    if (newLevel == currentLevel) {
                        continue; // nothing to do
                    }

                    holder = this.setLevel(key, newLevel, holder, currentLevel);

                    if (holder == null) {
                        if (newLevel <= ThreadedAnvilChunkStorage.MAX_LEVEL) {
                            throw new IllegalStateException("Expected chunk holder to be created");
                        }
                        // not loaded and it shouldn't be loaded!
                        continue;
                    }

                    this.chunkHolders.add(holder);
//                    if (recursiveCheck != this.ticketLevelUpdateCount) {
//                        // back to the start, we must create player chunks and update the ticket level fields before
//                        // processing the actual level updates
//                        continue ticket_update_loop;
//                    }
                }
            } finally {
//                this.pollingPendingChunkUpdates = oldPolling;
            }
        }

        if (!this.chunkHolders.isEmpty()) {
            this.chunkHolders.forEach(holder -> ((IChunkHolder) holder).invokeTick(threadedAnvilChunkStorage, this.mainThreadExecutor));
            this.chunkHolders.clear();
        } else {
            if (!this.chunkPositions.isEmpty()) {
                LongIterator longIterator = this.chunkPositions.iterator();

                while(longIterator.hasNext()) {
                    long l = longIterator.nextLong();
                    if (this.getTicketSet(l).stream().anyMatch(chunkTicket -> chunkTicket.getType() == ChunkTicketType.PLAYER)) {
                        ChunkHolder chunkHolder = ((IThreadedAnvilChunkStorage) threadedAnvilChunkStorage).invokeGetCurrentChunkHolder(l);
                        if (chunkHolder == null) {
                            throw new IllegalStateException();
                        }

                        CompletableFuture<Either<WorldChunk, ChunkHolder.Unloaded>> completableFuture = chunkHolder.getEntityTickingFuture();
                        completableFuture.thenAccept(either -> this.mainThreadExecutor.execute(() -> this.playerTicketThrottlerUnblocker.send(ChunkTaskPrioritySystem.createUnblockingMessage(() -> {
                        }, l, false))));
                    }
                }

                this.chunkPositions.clear();
            }
        }

        return flag;
        // Paper end - replace level propagator
    }

}
