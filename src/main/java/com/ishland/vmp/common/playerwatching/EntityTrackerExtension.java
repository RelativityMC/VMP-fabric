package com.ishland.vmp.common.playerwatching;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.EntityTrackingListener;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.util.Set;

public interface EntityTrackerExtension {

    boolean isPositionUpdated();

    void updatePosition();

    Vec3d getPreviousLocation();

    long getPreviousChunkPos();

    void updateListeners(Set<ServerPlayerEntity> triedPlayers);

    void tryTick();

}
