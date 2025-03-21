package com.ishland.vmp.mixins.ticketsystem.ticketpropagator;

import com.ishland.vmp.mixins.access.IAbstractChunkHolder;
import com.ishland.vmp.mixins.access.IChunkHolder;
import com.ishland.vmp.mixins.access.IThreadedAnvilChunkStorage;
import io.papermc.paper.util.misc.Delayed8WayDistancePropagator2D;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkLevelManager;
import net.minecraft.server.world.ChunkLevels;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.TicketDistanceLevelPropagator;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.concurrent.Executor;

@Mixin(ChunkLevelManager.class)
public abstract class MixinChunkLevelManager {

    @Mutable
    @Shadow @Final private TicketDistanceLevelPropagator ticketDistanceLevelPropagator;

    @Shadow protected @Nullable abstract ChunkHolder getChunkHolder(long pos);

    @Shadow protected @Nullable abstract ChunkHolder setLevel(long pos, int level, @Nullable ChunkHolder holder, int i);

    @Shadow @Final private Executor mainThreadExecutor;

    @Unique
    protected Long2IntLinkedOpenHashMap ticketLevelUpdates;

    @Unique
    protected io.papermc.paper.util.misc.Delayed8WayDistancePropagator2D ticketLevelPropagator;

    @Unique
    private ObjectArrayFIFOQueue<ChunkHolder> pendingChunkHolderUpdates;

    // Paper distance map propagates level from max to 0 while vanilla
    // one propagate from 0 to max
    // So there need a conversion between these values

    @Unique
    private static int convertBetweenTicketLevels(final int level) {
        return ChunkLevels.INACCESSIBLE - level + 1;
    }

    @Unique
    protected final void updateTicketLevel(final long coordinate, final int ticketLevel) {
        if (ticketLevel > ChunkLevels.INACCESSIBLE) {
            this.ticketLevelPropagator.removeSource(coordinate);
        } else {
            this.ticketLevelPropagator.setSource(coordinate, convertBetweenTicketLevels(ticketLevel));
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(ChunkTicketManager ticketManager, Executor executor, Executor mainThreadExecutor, CallbackInfo ci) {
        this.ticketDistanceLevelPropagator = null; // fail-fast incompatibility

        this.ticketLevelUpdates = new Long2IntLinkedOpenHashMap() {
            @Override
            protected void rehash(int newN) {
                if (newN < this.n) {
                    return;
                }
                super.rehash(newN);
            }
        };
        this.ticketLevelPropagator = new Delayed8WayDistancePropagator2D(
                (long coordinate, byte oldLevel, byte newLevel) -> {
                    this.ticketLevelUpdates.putAndMoveToLast(coordinate, convertBetweenTicketLevels(newLevel));
                }
        );
        this.pendingChunkHolderUpdates = new ObjectArrayFIFOQueue<>();

        ticketManager.setLoadingLevelUpdater((pos, level, added) -> this.updateTicketLevel(pos, level));
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/TicketDistanceLevelPropagator;update(I)I"))
    public int tickTickets(TicketDistanceLevelPropagator instance, int distance, ServerChunkLoadingManager threadedAnvilChunkStorage) {
        if (!((IThreadedAnvilChunkStorage) threadedAnvilChunkStorage).getMainThreadExecutor().isOnThread()) {
            throw new ConcurrentModificationException("Attempted to tick tickets asynchronously");
        }

        boolean hasUpdates = this.ticketLevelPropagator.propagateUpdates();
        if (hasUpdates) {
        }

        while (!this.ticketLevelUpdates.isEmpty()) {
            hasUpdates = true;

            long key = this.ticketLevelUpdates.firstLongKey();
            int newLevel = this.ticketLevelUpdates.removeFirstInt();

            ChunkHolder holder = this.getChunkHolder(key);
            int currentLevel = holder == null ? ChunkLevels.INACCESSIBLE + 1 : holder.getLevel();
            if (newLevel == currentLevel) continue;

            holder = this.setLevel(key, newLevel, holder, currentLevel);

            if (holder == null) {
                if (newLevel <= ChunkLevels.INACCESSIBLE) {
                    throw new IllegalStateException("Chunk holder not created");
                }
                continue;
            }

            this.pendingChunkHolderUpdates.enqueue(holder);
        }

        ArrayList<ChunkHolder> pending = new ArrayList<>(this.pendingChunkHolderUpdates.size());
        while (!this.pendingChunkHolderUpdates.isEmpty()) {
            pending.add(this.pendingChunkHolderUpdates.dequeue());
        }
        this.pendingChunkHolderUpdates.clear();
        for (ChunkHolder element : pending) {
            ((IAbstractChunkHolder) element).invokeUpdateStatus(threadedAnvilChunkStorage);
        }
        for (ChunkHolder element : pending) {
            ((IChunkHolder) element).invokeUpdateFutures(threadedAnvilChunkStorage, this.mainThreadExecutor);
        }

        return hasUpdates ? distance - 1 : distance;
    }

}
