package com.ishland.vmp.mixins.entity.item;

import com.ishland.vmp.common.entity.item.CachingWaterState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity implements CachingWaterState {

    @Shadow protected abstract boolean updateWaterState();

    @Unique
    protected boolean lastWaterState = false;

    @Inject(method = "updateWaterState", at = @At(value = "RETURN"))
    private void onUpdateWaterState(CallbackInfoReturnable<Boolean> cir) {
        this.lastWaterState = cir.getReturnValueZ();
    }

    @Override
    public boolean getLastWaterState() {
        return lastWaterState;
    }
}
