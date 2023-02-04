package com.ishland.vmp.common.playerwatching.compat;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4dc;
import org.joml.Vector3d;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class ValkyrienSkies2ShipPositionTransformer extends EntityPositionTransformer {

    private static final MethodHandle methodVSGameUtilsKt$getShipManagingPos;
    private static final MethodHandle methodShip$getShipToWorld;

    static {
        try {
            final Class<?> clazzVSGameUtilsKt = Class.forName("org.valkyrienskies.mod.common.VSGameUtilsKt");
            final Class<?> clazzShip = Class.forName("org.valkyrienskies.core.api.ships.Ship");
            methodVSGameUtilsKt$getShipManagingPos = MethodHandles.lookup().findStatic(clazzVSGameUtilsKt,
                    "getShipManagingPos", MethodType.methodType(clazzShip, World.class, int.class, int.class));
            methodShip$getShipToWorld = MethodHandles.lookup().findVirtual(clazzShip,
                    "getShipToWorld", MethodType.methodType(Matrix4dc.class));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public Vec3d transform0(Entity entity, Vec3d pos) {
        try {
            final Object ship = methodVSGameUtilsKt$getShipManagingPos.invoke(entity.world, ChunkSectionPos.getSectionCoord(pos.x), ChunkSectionPos.getSectionCoord(pos.z));
            if (ship != null) {
                final Matrix4dc shipToWorld = (Matrix4dc) methodShip$getShipToWorld.invoke(ship);
                final Vector3d transformedPosition = shipToWorld.transformPosition(new Vector3d(pos.x, pos.y, pos.z));
                return new Vec3d(transformedPosition.x, transformedPosition.y, transformedPosition.z);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        return pos;
    }
}
