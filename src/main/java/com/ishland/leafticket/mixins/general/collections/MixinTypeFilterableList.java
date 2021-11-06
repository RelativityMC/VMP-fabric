package com.ishland.leafticket.mixins.general.collections;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.collection.TypeFilterableList;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(TypeFilterableList.class)
public abstract class MixinTypeFilterableList<T> extends AbstractCollection<T> {

    @Mutable
    @Shadow @Final private Map<Class<?>, List<T>> elementsByType;

    @Mutable
    @Shadow @Final private List<T> allElements;

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/util/collection/TypeFilterableList;elementsByType:Ljava/util/Map;", opcode = Opcodes.PUTFIELD))
    private void redirectSetElementsByType(TypeFilterableList<T> instance, Map<Class<?>, List<T>> value) {
        this.elementsByType = new Object2ObjectOpenHashMap<>();
    }

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/util/collection/TypeFilterableList;allElements:Ljava/util/List;", opcode = Opcodes.PUTFIELD))
    private void redirectSetAllElements(TypeFilterableList<T> instance, List<T> value) {
        this.allElements = new ObjectArrayList<>();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;"))
    private HashMap<?, ?> redirectNewHashMap() {
        return null; // avoid unnecessary alloc
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList()Ljava/util/ArrayList;"))
    private ArrayList<?> redirectNewArrayList() {
        return null;
    }

}
