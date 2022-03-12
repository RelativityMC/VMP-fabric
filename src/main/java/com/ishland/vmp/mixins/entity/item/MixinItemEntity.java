package com.ishland.vmp.mixins.entity.item;

import com.ishland.vmp.common.entity.item.CachingWaterState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemEntity.class)
public abstract class MixinItemEntity extends Entity {

    public MixinItemEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ItemEntity;updateWaterState()Z"))
    private boolean redirectUpdateWaterState(ItemEntity instance) {
        return ((CachingWaterState) this).getLastWaterState();
    }
}
