package com.ishland.vmp.mixins.general.ingredient_matching;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.stream.Collectors;

@Mixin(Ingredient.class)
public abstract class MixinIngredient {

    @Shadow
    @Final
    private RegistryEntryList<Item> entries;

    @Shadow public abstract List<RegistryEntry<Item>> getMatchingItems();

    @Unique
    private Set<RegistryEntry<Item>> matchingItems = null;

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
            Set<RegistryEntry<Item>> matchingItems = this.matchingItems;
            boolean isEmptyMatch = this.isEmptyMatch;
            if (matchingItems == null) {
                matchingItems = this.matchingItems = new HashSet<>(this.getMatchingItems());
                isEmptyMatch = this.isEmptyMatch = this.matchingItems.isEmpty();
            }
            if (itemStack.isEmpty()) {
                return isEmptyMatch;
            }
            return matchingItems.contains(itemStack.getItem().getRegistryEntry());
        }
    }

}
