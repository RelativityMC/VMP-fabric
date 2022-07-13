package com.ishland.vmp.common.logging;


import com.ishland.vmp.common.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.util.Arrays;

public class AsyncAppenderBootstrap {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void boot() {
        if (!Config.USE_ASYNC_LOGGING) return;
        try {
            if (LogManager.getContext(false) instanceof LoggerContext ctx) {
                final LoggerConfig config = ctx.getRootLogger().get();

                final AppenderRef[] appenderRefs = config.getAppenderRefs().toArray(AppenderRef[]::new);
//            final Map<String, Appender> appenders = new Object2ObjectOpenHashMap<>(config.getAppenders());

                final AsyncAppender asyncAppender = new AsyncAppender.Builder<>()
                        .setAppenderRefs(appenderRefs)
                        .setName("AsyncAppender")
                        .setConfiguration(ctx.getConfiguration())
                        .build();
                asyncAppender.start();
                config.addAppender(asyncAppender, null, null);

                for (AppenderRef appenderRef : appenderRefs) {
                    config.removeAppender(appenderRef.toString());
                }

                ctx.updateLoggers();

                LOGGER.info("Successfully started async appender with {}", Arrays.toString(appenderRefs));
            }
        } catch (Throwable t) {
            LOGGER.error("Error occurred while booting async appender", t);
        }
    }

}
