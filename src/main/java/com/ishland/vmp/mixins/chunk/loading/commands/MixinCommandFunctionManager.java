package com.ishland.vmp.mixins.chunk.loading.commands;

import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunctionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CommandFunctionManager.class)
public class MixinCommandFunctionManager {

    @Inject(method = "getScheduledCommandSource", at = @At("RETURN"), cancellable = true)
    private void onGetScheduledCommandSource(CallbackInfoReturnable<ServerCommandSource> cir) {
        cir.setReturnValue(cir.getReturnValue().withOutput(CommandOutput.DUMMY));
    }

}
