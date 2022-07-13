package com.ishland.vmp.mixins;

import com.ishland.vmp.common.chunksending.PlayerChunkSendingSystem;
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
        if (mixinClassName.startsWith("com.ishland.vmp.mixins.chunksending."))
            return PlayerChunkSendingSystem.ENABLED;
        if (mixinClassName.startsWith("com.ishland.vmp.mixins.chunkloading.async_chunk_on_player_login"))
            return !isClassExist("com.ishland.c2me.opts.chunkio.common.async_chunk_on_player_login.IAsyncChunkPlayer");
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
