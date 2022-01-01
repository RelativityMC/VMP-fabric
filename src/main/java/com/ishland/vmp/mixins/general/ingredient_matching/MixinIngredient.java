package com.ishland.vmp.mixins.general.ingredient_matching;

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
    private Set<Item> matchingItems = null;

    @Unique
    private boolean isEmptyMatch = false;

    /**
     * @author ishland
     * @reason optimize test()
     */
    @Overwrite
    public boolean test(@Nullable ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        } else {
            Set<Item> matchingItems = this.matchingItems;
            boolean isEmptyMatch = this.isEmptyMatch;
            if (matchingItems == null) {
                matchingItems = this.matchingItems = Arrays.stream(this.entries)
                        .flatMap(entry -> entry.getStacks().stream())
                        .filter(itemStack1 -> !itemStack1.isEmpty())
                        .map(ItemStack::getItem)
                        .collect(Collectors.toCollection(HashSet::new));
                isEmptyMatch = this.isEmptyMatch = this.matchingItems.isEmpty();
            }
            if (itemStack.isEmpty()) {
                return isEmptyMatch;
            }
            return matchingItems.contains(itemStack.getItem());
        }
    }

}
