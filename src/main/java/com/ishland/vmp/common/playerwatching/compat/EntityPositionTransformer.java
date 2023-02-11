package com.ishland.vmp.common.playerwatching.compat;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

public abstract class EntityPositionTransformer {

    protected abstract Vec3d transform0(Entity entity, Vec3d pos);

    public final Vec3d transform(Entity entity, Vec3d pos) {
        final Vec3d pos1;
        try {
            pos1 = transform0(entity, pos);
        } catch (Throwable t) {
            System.err.println("EntityPositionTransformer %s threw an exception for %s at %s".formatted(getClass().getName(), entity, pos));
            t.printStackTrace();
            return pos;
        }
        if (pos1 == null) {
            System.err.println("EntityPositionTransformer %s returned null for %s at %s".formatted(getClass().getName(), entity, pos));
            return pos;
        }
        return pos1;
    }

}
