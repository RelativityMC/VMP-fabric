package com.ishland.vmp.mixins.ticketsystem.ticketpropagator;

import com.ishland.vmp.mixins.access.IChunkHolder;
import com.ishland.vmp.mixins.access.IChunkTicket;
import io.papermc.paper.util.misc.Delayed8WayDistancePropagator2D;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTaskPrioritySystem;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.collection.SortedArraySet;
import net.minecraft.util.thread.MessageListener;
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

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

@Mixin(ChunkTicketManager.class)
public abstract class MixinChunkTicketManager {

    @Mutable
    @Shadow @Final private ChunkTicketManager.TicketDistanceLevelPropagator distanceFromTicketTracker;

    @Shadow protected @Nullable abstract ChunkHolder getChunkHolder(long pos);

    @Shadow protected @Nullable abstract ChunkHolder setLevel(long pos, int level, @Nullable ChunkHolder holder, int i);

    @Shadow @Final private ChunkTicketManager.NearbyChunkTicketUpdater nearbyChunkTicketUpdater;
    @Shadow @Final private Set<ChunkHolder> chunkHolders;
    @Shadow @Final private Executor mainThreadExecutor;
    @Shadow @Final private LongSet chunkPositions;

    @Shadow protected abstract SortedArraySet<ChunkTicket<?>> getTicketSet(long position);

    @Shadow @Final private MessageListener<ChunkTaskPrioritySystem.UnblockingMessage> playerTicketThrottlerUnblocker;
    @Shadow private long age;
    @Shadow @Final private Long2ObjectOpenHashMap<SortedArraySet<ChunkTicket<?>>> ticketsByPosition;

    @Shadow
    protected static int getLevel(SortedArraySet<ChunkTicket<?>> sortedArraySet) {
        throw new AbstractMethodError();
    }

    @Unique
    protected Long2IntLinkedOpenHashMap ticketLevelUpdates;

    @Unique
    protected io.papermc.paper.util.misc.Delayed8WayDistancePropagator2D ticketLevelPropagator;

    @Unique
    private ArrayList<ChunkHolder> pendingChunkHolderUpdates;

    // Paper distance map propagates level from max to 0 while vanilla
    // one propagate from 0 to max
    // So there need a conversion between these values

    @Unique
    private static int convertBetweenTicketLevels(final int level) {
        return ThreadedAnvilChunkStorage.MAX_LEVEL - level + 1;
    }

    @Unique
    protected final void updateTicketLevel(final long coordinate, final int ticketLevel) {
        if (ticketLevel > ThreadedAnvilChunkStorage.MAX_LEVEL) {
            this.ticketLevelPropagator.removeSource(coordinate);
        } else {
            this.ticketLevelPropagator.setSource(coordinate, convertBetweenTicketLevels(ticketLevel));
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(Executor workerExecutor, Executor mainThreadExecutor, CallbackInfo ci) {
        this.distanceFromTicketTracker = null; // fail-fast incompatibility

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
        this.pendingChunkHolderUpdates = new ArrayList<>();
    }

    @Redirect(method = {"purge", "addTicket(JLnet/minecraft/server/world/ChunkTicket;)V", "removeTicket(JLnet/minecraft/server/world/ChunkTicket;)V", "method_39995"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ChunkTicketManager$TicketDistanceLevelPropagator;updateLevel(JIZ)V"), require = 3, expect = 3)
    private void redirectUpdate(ChunkTicketManager.TicketDistanceLevelPropagator instance, long l, int i, boolean b) {
        this.updateTicketLevel(l, i);
    }

    /**
     * @author ishland
     * @reason workaround for lithium compat
     */
    @Overwrite
    public void purge() {
        ++this.age;

        final Predicate<ChunkTicket<?>> predicate = chunkTicket -> ((IChunkTicket) chunkTicket).invokeIsExpired1(this.age);
        ObjectIterator<Long2ObjectMap.Entry<SortedArraySet<ChunkTicket<?>>>> objectIterator = this.ticketsByPosition.long2ObjectEntrySet().fastIterator();

        while(objectIterator.hasNext()) {
            Long2ObjectMap.Entry<SortedArraySet<ChunkTicket<?>>> entry = objectIterator.next();
            if (entry.getValue().removeIf(predicate)) {
                this.distanceFromTicketTracker.updateLevel(entry.getLongKey(), getLevel(entry.getValue()), false); // modified
            }

            if (entry.getValue().isEmpty()) {
                objectIterator.remove();
            }
        }

    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ChunkTicketManager$TicketDistanceLevelPropagator;update(I)I"))
    public int tickTickets(ChunkTicketManager.TicketDistanceLevelPropagator __, int distance, ThreadedAnvilChunkStorage threadedAnvilChunkStorage) {
        boolean hasUpdates = this.ticketLevelPropagator.propagateUpdates();
        if (hasUpdates) {
        }

        while (!this.ticketLevelUpdates.isEmpty()) {
            hasUpdates = true;

            long key = this.ticketLevelUpdates.firstLongKey();
            int newLevel = this.ticketLevelUpdates.removeFirstInt();

            ChunkHolder holder = this.getChunkHolder(key);
            int currentLevel = holder == null ? ThreadedAnvilChunkStorage.MAX_LEVEL + 1 : holder.getLevel();
            if (newLevel == currentLevel) continue;

            holder = this.setLevel(key, newLevel, holder, currentLevel);

            if (holder == null) {
                if (newLevel <= ThreadedAnvilChunkStorage.MAX_LEVEL) {
                    throw new IllegalStateException("Chunk holder not created");
                }
                continue;
            }

            this.pendingChunkHolderUpdates.add(holder);
        }

        for (ChunkHolder holder : this.pendingChunkHolderUpdates) {
            ((IChunkHolder) holder).invokeTick1(threadedAnvilChunkStorage, this.mainThreadExecutor);
        }
        this.pendingChunkHolderUpdates.clear();

        return hasUpdates ? distance - 1 : distance;
    }

}
