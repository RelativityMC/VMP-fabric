package com.ishland.vmp.mixins.entity.iteration;

import com.ishland.vmp.common.general.collections.ITypeFilterableList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.collection.TypeFilterableList;
import net.minecraft.util.math.Box;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;
import java.util.function.Consumer;

@Mixin(value = EntityTrackingSection.class, priority = 990)
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
        final TypeFilterableList<T> collection = this.collection;
        if (collection instanceof ITypeFilterableList iTypeFilterableList) { // use array for iteration
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
            for (T entityLike : collection) {
                Box box1 = entityLike.getBoundingBox();
                if (box1.minX < box.maxX && box1.maxX > box.minX && box1.minY < box.maxY && box1.maxY > box.minY && box1.minZ < box.maxZ && box1.maxZ > box.minZ) { // inline math
                    action.accept(entityLike);
                }
            }
        }
    }

    /**
     * @author ishland
     * @reason use array for iteration & inline math
     */
    @Overwrite
    public <U extends T> void forEach(TypeFilter<T, U> type, Box box, Consumer<? super U> action) {
        Collection<? extends T> collection = this.collection.getAllOfType(type.getBaseClass());
        if (!collection.isEmpty()) {
            if (collection instanceof ObjectArrayList objectArrayList) { // use array for iteration
                for (Object _entityLike : objectArrayList.elements()) {
                    if (_entityLike != null) {
                        T entityLike = (T) _entityLike;
                        U entityLike2 = type.downcast(entityLike);
                        final Box boundingBox = entityLike.getBoundingBox();
                        if (entityLike2 != null && boundingBox.minX < box.maxX && boundingBox.maxX > box.minX && boundingBox.minY < box.maxY && boundingBox.maxY > box.minY && boundingBox.minZ < box.maxZ && boundingBox.maxZ > box.minZ) { // inline math
                            action.accept(entityLike2);
                        }
                    }
                }
            } else { // fallback
                for(T entityLike : collection) {
                    U entityLike2 = type.downcast(entityLike);
                    Box box1 = entityLike.getBoundingBox();
                    if (entityLike2 != null && box1.minX < box.maxX && box1.maxX > box.minX && box1.minY < box.maxY && box1.maxY > box.minY && box1.minZ < box.maxZ && box1.maxZ > box.minZ) { // inline math
                        action.accept(entityLike2);
                    }
                }
            }
        }
    }

}
