package com.ishland.vmp.mixins.chunk.sending.c2me_notickvd_compat;

import com.ishland.vmp.common.chunk.sending.PlayerChunkSendingSystem;
import com.ishland.vmp.common.chunkwatching.AreaPlayerChunkWatchingManager;
import com.ishland.vmp.mixins.access.IThreadedAnvilChunkStorage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.PlayerChunkWatchingManager;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.ishland.c2me.notickvd.common.NoTickChunkSendingInterceptor")
public class MixinNoTickChunkSendingInterceptor {

    @Dynamic
    @Inject(method = "onChunkSending", at = @At("RETURN"), remap = false, cancellable = true)
    private static void onChunkSending(ServerPlayerEntity player, long pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            final PlayerChunkWatchingManager playerChunkWatchingManager = ((IThreadedAnvilChunkStorage) player.getServerWorld().getChunkManager().threadedAnvilChunkStorage).getPlayerChunkWatchingManager();
            if (playerChunkWatchingManager instanceof AreaPlayerChunkWatchingManager manager && PlayerChunkSendingSystem.ENABLED) {
                manager.onChunkLoaded(pos);
                cir.setReturnValue(false);
            }
        }
    }

}
