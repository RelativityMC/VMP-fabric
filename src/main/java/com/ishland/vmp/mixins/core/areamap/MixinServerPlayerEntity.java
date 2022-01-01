package com.ishland.vmp.mixins.core.areamap;

import com.destroystokyo.paper.util.misc.PlayerAreaMap;
import com.destroystokyo.paper.util.misc.PooledLinkedHashSets;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity implements PlayerAreaMap.PlayerCachedSingleHashSetAccessor {

    @Unique
    private PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayerEntity> cachedSingleHashSet; // Paper

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.cachedSingleHashSet = new PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<>((ServerPlayerEntity) (Object) this);
    }

    @Override
    public PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayerEntity> getCachedSingleHashSet() {
        return this.cachedSingleHashSet;
    }
}
