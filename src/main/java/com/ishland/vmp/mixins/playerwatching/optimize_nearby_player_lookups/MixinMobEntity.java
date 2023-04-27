package com.ishland.vmp.mixins.playerwatching.optimize_nearby_player_lookups;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(MobEntity.class)
public abstract class MixinMobEntity extends LivingEntity {

    protected MixinMobEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Redirect(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getClosestPlayer(Lnet/minecraft/entity/Entity;D)Lnet/minecraft/entity/player/PlayerEntity;"))
    private PlayerEntity redirectGetClosestPlayer(World instance, Entity entity, double maxDistance) {
        final PlayerEntity closestPlayer = instance.getClosestPlayer(entity, this.getType().getSpawnGroup().getImmediateDespawnRange());
        if (closestPlayer != null) {
            return closestPlayer;
        } else {
            final List<? extends PlayerEntity> players = this.getWorld().getPlayers();
            if (players.isEmpty()) return null;
            return players.get(0);
        }
    }

}
