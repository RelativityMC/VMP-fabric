package com.ishland.vmp.mixins.chunk.loading.commands;

import com.ishland.vmp.common.chunk.loading.async_chunks_on_player_login.AsyncChunkLoadUtil;
import com.ishland.vmp.mixins.access.IServerCommandSource;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TeleportCommand;
import net.minecraft.server.rcon.RconCommandOutput;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

@Mixin(TeleportCommand.class)
public abstract class MixinTeleportCommand {

    @Shadow
    protected static String formatFloat(double d) {
        throw new AbstractMethodError();
    }

    @Shadow
    protected static void teleport(ServerCommandSource source, Entity target, ServerWorld world, double x, double y, double z, Set<PositionFlag> movementFlags, float yaw, float pitch, TeleportCommand.@Nullable LookTarget facingLocation) throws CommandSyntaxException {
    }

    /**
     * @author ishland
     * @reason async teleport
     */
    @Overwrite
    private static int execute(ServerCommandSource source, Collection<? extends Entity> targets, Entity destination) {
        final Runnable action = () -> {
            int successCount = 0;
            Entity last = null;
            for (Entity entity : targets) {
                if (!entity.isAlive()) continue;
                try {
                    teleport(
                            source,
                            entity,
                            (ServerWorld) destination.world,
                            destination.getX(),
                            destination.getY(),
                            destination.getZ(),
                            EnumSet.noneOf(PositionFlag.class),
                            destination.getYaw(),
                            destination.getPitch(),
                            null
                    );
                    successCount++;
                    last = entity;
                } catch (CommandSyntaxException e) {
                    source.sendError(Texts.toText(e.getRawMessage()));
                } catch (Throwable t) {
                    t.printStackTrace();
                    source.sendError(Text.literal("Error occurred while teleporting entity"));
                }
            }
            if (successCount == 1) {
                source.sendFeedback(
                        Text.translatable("commands.teleport.success.entity.single", last.getDisplayName(), destination.getDisplayName()), true
                );
            } else {
                source.sendFeedback(Text.translatable("commands.teleport.success.entity.multiple", successCount, destination.getDisplayName()), true);
            }
        };

        final CommandOutput output = ((IServerCommandSource) source).getOutput();
        if (output instanceof PlayerEntity || output instanceof MinecraftServer || output instanceof RconCommandOutput) {
            AsyncChunkLoadUtil.scheduleChunkLoadWithRadius((ServerWorld) destination.world, destination.getChunkPos(), 2)
                    .thenRunAsync(action, destination.world.getServer());
        } else {
            action.run();
        }

        return targets.size();
    }

    /**
     * @author ishland
     * @reason async teleport
     */
    @Overwrite
    private static int execute(
            ServerCommandSource source,
            Collection<? extends Entity> targets,
            ServerWorld world,
            PosArgument location,
            @Nullable PosArgument rotation,
            @Nullable TeleportCommand.LookTarget facingLocation
    ) throws CommandSyntaxException {
        Vec3d vec3d = location.toAbsolutePos(source);
        Vec2f vec2f = rotation == null ? null : rotation.toAbsoluteRotation(source);
        Set<PositionFlag> set = EnumSet.noneOf(PositionFlag.class);
        if (location.isXRelative()) {
            set.add(PositionFlag.X);
        }

        if (location.isYRelative()) {
            set.add(PositionFlag.Y);
        }

        if (location.isZRelative()) {
            set.add(PositionFlag.Z);
        }

        if (rotation == null) {
            set.add(PositionFlag.X_ROT);
            set.add(PositionFlag.Y_ROT);
        } else {
            if (rotation.isXRelative()) {
                set.add(PositionFlag.X_ROT);
            }

            if (rotation.isYRelative()) {
                set.add(PositionFlag.Y_ROT);
            }
        }

        final Runnable action = () -> {
            int successCount = 0;
            Entity last = null;

            for (Entity entity : targets) {
                if (!entity.isAlive()) continue;
                try {
                    if (rotation == null) {
                        teleport(source, entity, world, vec3d.x, vec3d.y, vec3d.z, set, entity.getYaw(), entity.getPitch(), facingLocation);
                    } else {
                        teleport(source, entity, world, vec3d.x, vec3d.y, vec3d.z, set, vec2f.y, vec2f.x, facingLocation);
                    }
                    successCount++;
                    last = entity;
                } catch (CommandSyntaxException e) {
                    source.sendError(Texts.toText(e.getRawMessage()));
                } catch (Throwable t) {
                    t.printStackTrace();
                    source.sendError(Text.literal("Error occurred while teleporting entity"));
                }
            }

            if (successCount == 1) {
                source.sendFeedback(
                        Text.translatable(
                                "commands.teleport.success.location.single",
                                last.getDisplayName(),
                                formatFloat(vec3d.x),
                                formatFloat(vec3d.y),
                                formatFloat(vec3d.z)
                        ),
                        true
                );
            } else {
                source.sendFeedback(
                        Text.translatable("commands.teleport.success.location.multiple", successCount, formatFloat(vec3d.x), formatFloat(vec3d.y), formatFloat(vec3d.z)), true
                );
            }
        };

        final CommandOutput output = ((IServerCommandSource) source).getOutput();
        if (output instanceof PlayerEntity || output instanceof MinecraftServer || output instanceof RconCommandOutput) {
            AsyncChunkLoadUtil.scheduleChunkLoadWithRadius(world, new ChunkPos(new BlockPos(vec3d)), 2)
                    .thenRunAsync(action, world.getServer());
        } else {
            action.run();
        }

        return targets.size();
    }

}
