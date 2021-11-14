package com.ishland.leafticket.mixins.general.ingredient_matching;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(Ingredient.class)
public class MixinIngredient {

    @Shadow
    @Final
    private Ingredient.Entry[] entries;

    @Unique
    private Set<Item> matchingItems = Collections.emptySet();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        @SuppressWarnings("FuseStreamOperations") Set<Item> matchingItems = Arrays.stream(this.entries)
                .flatMap(entry -> entry.getStacks().stream())
                .filter(itemStack -> !itemStack.isEmpty())
                .map(ItemStack::getItem)
                .collect(Collectors.toCollection(HashSet::new));
        this.matchingItems = Collections.unmodifiableSet(matchingItems);
    }

    /**
     * @author ishland
     * @reason optimize test()
     */
    @Overwrite
    public boolean test(@Nullable ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        } else {
            if (itemStack.isEmpty()) {
                return this.matchingItems.isEmpty();
            }
            return this.matchingItems.contains(itemStack.getItem());
        }
    }

}
