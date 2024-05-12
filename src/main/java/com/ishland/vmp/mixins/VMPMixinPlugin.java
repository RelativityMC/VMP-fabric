package com.ishland.vmp.mixins;

import com.ishland.vmp.common.config.Config;
import com.ishland.vmp.common.logging.AsyncAppenderBootstrap;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class VMPMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
        Config.init();
        AsyncAppenderBootstrap.boot();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith("com.ishland.vmp.mixins.carpet."))
            return FabricLoader.getInstance().isModLoaded("carpet");

        if (mixinClassName.startsWith("com.ishland.vmp.mixins.playerwatching.optimize_nearby_entity_tracking_lookups"))
            return Config.USE_OPTIMIZED_ENTITY_TRACKING;
        if (mixinClassName.startsWith("com.ishland.vmp.mixins.networking.eventloops."))
            return Config.USE_MULTIPLE_NETTY_EVENT_LOOPS;
        if (mixinClassName.startsWith("com.ishland.vmp.mixins.chunk.loading.portals."))
            return Config.USE_ASYNC_PORTALS;
        if (mixinClassName.startsWith("com.ishland.vmp.mixins.chunk.iteration."))
            return Config.USE_OPTIMIZED_CHUNK_TICKING_ITERATION;
        if (mixinClassName.startsWith("com.ishland.vmp.mixins.chunk.loading.async_chunk_on_player_login"))
            return Config.USE_ASYNC_CHUNKS_ON_LOGIN && !isClassExist("com.ishland.c2me.opts.chunkio.common.async_chunk_on_player_login.IAsyncChunkPlayer");
        if (mixinClassName.startsWith("com.ishland.vmp.mixins.chunk.loading.command"))
            return Config.USE_ASYNC_CHUNKS_ON_SOME_COMMANDS;
        if (mixinClassName.equals("com.ishland.vmp.mixins.playerwatching.MixinTACSCancelSendingKrypton"))
            return FabricLoader.getInstance().isModLoaded("krypton");
        if (mixinClassName.startsWith("com.ishland.vmp.mixins.networking.avoid_deadlocks"))
            return !FabricLoader.getInstance().isModLoaded("raknetify");
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    private static boolean isClassExist(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
