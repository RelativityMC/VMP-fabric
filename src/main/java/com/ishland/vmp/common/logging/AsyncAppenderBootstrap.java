package com.ishland.vmp.common.logging;


import com.ishland.vmp.common.config.Config;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.async.AsyncLoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.builder.impl.DefaultConfigurationBuilder;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AsyncAppenderBootstrap {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ObjectOpenHashSet<String> appenderInCompatibilityMode = new ObjectOpenHashSet<>(new String[]{
            "SysOut"
    });

    public static void boot() {
        if (!Config.USE_ASYNC_LOGGING) return;
        try {
            if (LogManager.getContext(false) instanceof LoggerContext ctx &&
                ctx.getConfiguration() instanceof AbstractConfiguration configuration) {

                if (ctx instanceof AsyncLoggerContext) {
                    LOGGER.info("Logger is already async, skipping init async appender");
                    return;
                }

                final Object2ObjectOpenHashMap<String, Appender> original = new Object2ObjectOpenHashMap<>(configuration.getAppenders());
                final Object2ObjectOpenHashMap<String, Appender> newMap = new Object2ObjectOpenHashMap<>();

                final LoggerConfig config = ctx.getRootLogger().get();
                for (AppenderRef appenderRef : config.getAppenderRefs()) {
                    final AsyncAppender asyncAppender = new AsyncAppender.Builder<>()
                            .setAppenderRefs(new AppenderRef[]{appenderRef})
                            .setName(appenderRef.getRef())
                            .setConfiguration(configuration)
                            .build();
                    asyncAppender.start();
                    config.removeAppender(appenderRef.getRef());
                    config.addAppender(asyncAppender, null, null);
                    newMap.put(appenderRef.getRef(), asyncAppender);
                }

                ctx.updateLoggers();

                for (AppenderRef appenderRef : config.getAppenderRefs()) {
                    for (LoggerConfig loggerConfig : configuration.getLoggers().values()) {
                        if (loggerConfig.getAppenders().containsKey(appenderRef.getRef())) {
                            loggerConfig.removeAppender(appenderRef.getRef());
                            loggerConfig.addAppender(newMap.get(appenderRef.getRef()), null, null);
                        }
                    }
                    if (config.getAppenders().containsKey(appenderRef.getRef())) {
                        config.getAppenders().remove(appenderRef.getRef());
                        config.addAppender(newMap.get(appenderRef.getRef()), null, null);
                    }
                }

                LOGGER.info("Successfully started async appender with {}", original.keySet());
            } else {
                LOGGER.error("Unsupported logger settings for async appender");
            }
        } catch (Throwable t) {
            LOGGER.error("Error occurred while booting async appender", t);
        }
    }

}
