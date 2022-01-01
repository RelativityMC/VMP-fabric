package com.ishland.vmp.mixins.entity.iteration;

import com.ishland.vmp.common.general.collections.ITypeFilterableList;
import net.minecraft.util.collection.TypeFilterableList;
import net.minecraft.util.math.Box;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Consumer;

@Mixin(EntityTrackingSection.class)
public class MixinEntityTrackingSection<T extends EntityLike> {

    @Shadow
    @Final
    private TypeFilterableList<T> collection;

    /**
     * @author ishland
     * @reason use array for iteration & inline math
     */
    @Overwrite
    public void forEach(Box box, Consumer<T> action) {
        if (this.collection instanceof ITypeFilterableList iTypeFilterableList) { // use array for iteration
            for (Object _entityLike : iTypeFilterableList.getBackingArray()) {
                if (_entityLike != null) {
                    @SuppressWarnings("unchecked") T entityLike = (T) _entityLike;
                    Box box1 = entityLike.getBoundingBox();
                    if (box1.minX < box.maxX && box1.maxX > box.minX && box1.minY < box.maxY && box1.maxY > box.minY && box1.minZ < box.maxZ && box1.maxZ > box.minZ) { // inline math
                        action.accept(entityLike);
                    }
                }
            }

        } else { // fallback
            for (T entityLike : this.collection) {
                Box box1 = entityLike.getBoundingBox();
                if (box1.minX < box.maxX && box1.maxX > box.minX && box1.minY < box.maxY && box1.maxY > box.minY && box1.minZ < box.maxZ && box1.maxZ > box.minZ) { // inline math
                    action.accept(entityLike);
                }
            }
        }
    }

}
