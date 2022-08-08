package com.ishland.vmp.mixins.chunkloading.commands;

import com.google.common.collect.Maps;
import com.ishland.vmp.common.chunkloading.async_chunks_on_player_login.AsyncChunkLoadUtil;
import com.ishland.vmp.mixins.access.IServerCommandSource;
import com.ishland.vmp.mixins.access.ISpreadPlayersCommandPile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic4CommandExceptionType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.SpreadPlayersCommand;
import net.minecraft.server.rcon.RconCommandOutput;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

@Mixin(SpreadPlayersCommand.class)
public abstract class MixinSpreadPlayersCommand {

    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            1, 1,
            10L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(8)
    );

    @Shadow
    @Final
    private static Dynamic2CommandExceptionType INVALID_HEIGHT_EXCEPTION;

    @Shadow
    protected static int getPileCountRespectingTeams(Collection<? extends Entity> entities) {
        throw new AbstractMethodError();
    }

    @Shadow
    protected static SpreadPlayersCommand.Pile[] makePiles(Random random, int count, double minX, double minZ, double maxX, double maxZ) {
        throw new AbstractMethodError();
    }

    @Shadow
    @Final
    private static Dynamic4CommandExceptionType FAILED_TEAMS_EXCEPTION;

    @Shadow
    @Final
    private static Dynamic4CommandExceptionType FAILED_ENTITIES_EXCEPTION;

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private static void execute(
            ServerCommandSource source, Vec2f center, float spreadDistance, float maxRange, int maxY, boolean respectTeams, Collection<? extends Entity> players,
            CallbackInfoReturnable<Integer> cir
    ) throws CommandSyntaxException {
        final CommandOutput output = ((IServerCommandSource) source).getOutput();
        if (!(output instanceof PlayerEntity) && !(output instanceof MinecraftServer) && !(output instanceof RconCommandOutput)) {
            return;
        }
        cir.cancel();
        cir.setReturnValue(0);
        ServerWorld serverWorld = source.getWorld();
        int i = serverWorld.getBottomY();
        if (maxY < i) {
            throw INVALID_HEIGHT_EXCEPTION.create(maxY, i);
        } else {
            Random random = new Random();
            double d = center.x - maxRange;
            double e = center.y - maxRange;
            double f = center.x + maxRange;
            double g = center.y + maxRange;
            SpreadPlayersCommand.Pile[] piles = makePiles(random, respectTeams ? getPileCountRespectingTeams(players) : players.size(), d, e, f, g);
            EXECUTOR.execute(() -> {
                try {
                    vmp$spread(center, spreadDistance, serverWorld, random, d, e, f, g, maxY, piles, respectTeams);
                } catch (CommandSyntaxException ex) {
                    source.getServer().send(new ServerTask(0, () -> {
                        source.sendError(Texts.toText(ex.getRawMessage()));
                    }));
                } catch (Throwable t) {
                    source.getServer().execute(() -> {
                        source.sendError(Text.of("An error occurred while spreading players, check console for details"));
                        t.printStackTrace();
                    });
                }
                double h = vmp$getMinDistance(players, serverWorld, piles, maxY, respectTeams);
                source.getServer().execute(() -> {
                    source.sendFeedback(
                            new TranslatableText(
                                    "commands.spreadplayers.success." + (respectTeams ? "teams" : "entities"), piles.length, center.x, center.y, String.format(Locale.ROOT, "%.2f", h)
                            ),
                            true
                    );
                });
            });
            cir.setReturnValue(piles.length);
        }
    }

    @Unique
    private static void vmp$spread(
            Vec2f center,
            double spreadDistance,
            ServerWorld world,
            Random random,
            double minX,
            double minZ,
            double maxX,
            double maxZ,
            int maxY,
            SpreadPlayersCommand.Pile[] piles,
            boolean respectTeams
    ) throws CommandSyntaxException {
        boolean bl = true;
        double d = Float.MAX_VALUE;

        int i;
        for (i = 0; i < 10000 && bl; ++i) {
            bl = false;
            d = Float.MAX_VALUE;

            for (int j = 0; j < piles.length; ++j) {
                SpreadPlayersCommand.Pile pile = piles[j];
                int k = 0;
                SpreadPlayersCommand.Pile pile2 = new SpreadPlayersCommand.Pile();

                for (int l = 0; l < piles.length; ++l) {
                    if (j != l) {
                        SpreadPlayersCommand.Pile pile3 = piles[l];
                        double e = ((ISpreadPlayersCommandPile) pile).invokeGetDistance(pile3);
                        d = Math.min(e, d);
                        if (e < spreadDistance) {
                            ++k;
                            ((ISpreadPlayersCommandPile) pile2).setX(((ISpreadPlayersCommandPile) pile2).getX() + (((ISpreadPlayersCommandPile) pile3).getX() - ((ISpreadPlayersCommandPile) pile).getX()));
                            ((ISpreadPlayersCommandPile) pile2).setZ(((ISpreadPlayersCommandPile) pile2).getZ() + (((ISpreadPlayersCommandPile) pile3).getZ() - ((ISpreadPlayersCommandPile) pile).getZ()));
                        }
                    }
                }

                if (k > 0) {
                    ((ISpreadPlayersCommandPile) pile2).setX(((ISpreadPlayersCommandPile) pile2).getX() / k);
                    ((ISpreadPlayersCommandPile) pile2).setZ(((ISpreadPlayersCommandPile) pile2).getZ() / k);
                    double f = ((ISpreadPlayersCommandPile) pile2).invokeAbsolute();
                    if (f > 0.0) {
                        ((ISpreadPlayersCommandPile) pile2).invokeNormalize();
                        pile.subtract(pile2);
                    } else {
                        pile.setPileLocation(random, minX, minZ, maxX, maxZ);
                    }

                    bl = true;
                }

                if (pile.clamp(minX, minZ, maxX, maxZ)) {
                    bl = true;
                }
            }

            if (!bl) {
                List<CompletableFuture<Void>> futures = new ArrayList<>(piles.length);
                AtomicBoolean result = new AtomicBoolean(false);
                for (SpreadPlayersCommand.Pile pile2 : piles) {
                    ChunkPos pos = new ChunkPos(new BlockPos(((ISpreadPlayersCommandPile) pile2).getX(), 0.0, ((ISpreadPlayersCommandPile) pile2).getZ()));
                    final CompletableFuture<Void> future =
                            CompletableFuture.supplyAsync(() -> AsyncChunkLoadUtil.scheduleChunkLoad(world, pos), world.getServer())
                                    .thenCompose(Function.identity())
                                    .whenCompleteAsync((unused, throwable) -> {
                                        if (!pile2.isSafe(world, maxY)) {
                                            pile2.setPileLocation(random, minX, minZ, maxX, maxZ);
                                            result.set(true);
                                        }
                                    }, world.getServer())
                                    .exceptionally(throwable -> null)
                                    .thenRun(() -> {
                                    });
                    futures.add(future);
                }
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
                bl = result.get();
            }
        }

        if (d == Float.MAX_VALUE) {
            d = 0.0;
        }

        if (i >= 10000) {
            if (respectTeams) {
                throw FAILED_TEAMS_EXCEPTION.create(piles.length, center.x, center.y, String.format(Locale.ROOT, "%.2f", d));
            } else {
                throw FAILED_ENTITIES_EXCEPTION.create(piles.length, center.x, center.y, String.format(Locale.ROOT, "%.2f", d));
            }
        }
    }

    @Unique
    private static double vmp$getMinDistance(
            Collection<? extends Entity> entities, ServerWorld world, SpreadPlayersCommand.Pile[] piles, int maxY, boolean respectTeams
    ) {
        double d = 0.0;
        int i = 0;
        Map<AbstractTeam, SpreadPlayersCommand.Pile> map = Maps.newHashMap();

        List<CompletableFuture<Void>> futures = new ArrayList<>(piles.length);

        for (Entity entity : entities) {
            if (!entity.isAlive()) continue;
            SpreadPlayersCommand.Pile pile;
            if (respectTeams) {
                AbstractTeam abstractTeam = entity instanceof PlayerEntity ? entity.getScoreboardTeam() : null;
                if (!map.containsKey(abstractTeam)) {
                    map.put(abstractTeam, piles[i++]);
                }

                pile = map.get(abstractTeam);
            } else {
                pile = piles[i++];
            }

            ChunkPos pos = new ChunkPos(new BlockPos(((ISpreadPlayersCommandPile) pile).getX(), 0.0, ((ISpreadPlayersCommandPile) pile).getZ()));
            final CompletableFuture<Void> future =
                    CompletableFuture.supplyAsync(() -> AsyncChunkLoadUtil.scheduleChunkLoad(world, pos), world.getServer())
                            .thenCompose(Function.identity())
                            .whenCompleteAsync((unused, throwable) -> {
                                entity.teleport(Math.floor(((ISpreadPlayersCommandPile) pile).getX()) + 0.5, pile.getY(world, maxY), Math.floor(((ISpreadPlayersCommandPile) pile).getZ()) + 0.5);
                            }, world.getServer())
                            .exceptionally(throwable -> null)
                            .thenRun(() -> {
                            });
            futures.add(future);

            double e = Double.MAX_VALUE;

            for (SpreadPlayersCommand.Pile pile2 : piles) {
                if (pile != pile2) {
                    double f = ((ISpreadPlayersCommandPile) pile).invokeGetDistance(pile2);
                    e = Math.min(f, e);
                }
            }

            d += e;
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        return entities.size() < 2 ? 0.0 : d / (double) entities.size();
    }

}
