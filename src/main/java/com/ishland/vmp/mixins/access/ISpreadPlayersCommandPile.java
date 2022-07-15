package com.ishland.vmp.mixins.access;

import net.minecraft.server.command.SpreadPlayersCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SpreadPlayersCommand.Pile.class)
public interface ISpreadPlayersCommandPile {

    @Accessor("x")
    double getX();

    @Accessor("z")
    double getZ();

    @Accessor("x")
    void setX(double x);

    @Accessor("z")
    void setZ(double z);

    @Invoker
    double invokeGetDistance(SpreadPlayersCommand.Pile other);

    @Invoker
    void invokeNormalize();

    @Invoker
    double invokeAbsolute();


}
