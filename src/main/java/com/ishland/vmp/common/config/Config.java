package com.ishland.vmp.common.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

public class Config {

    public static final int TARGET_CHUNK_SEND_RATE;
    public static final boolean USE_PACKET_PRIORITY_SYSTEM;
    public static final boolean USE_ASYNC_LOGGING;
    public static final boolean USE_OPTIMIZED_ENTITY_TRACKING;
    public static final boolean USE_MULTIPLE_NETTY_EVENT_LOOPS;

    static {
        final Properties properties = new Properties();
        final Path path = FabricLoader.getInstance().getConfigDir().resolve("vmp.properties");
        if (Files.isRegularFile(path)) {
            try (InputStream in = Files.newInputStream(path, StandardOpenOption.CREATE)) {
                properties.load(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        TARGET_CHUNK_SEND_RATE = getInt(properties, "target_chunk_send_rate", -1);
        USE_PACKET_PRIORITY_SYSTEM = getBoolean(properties, "use_packet_priority_system", true);
        USE_ASYNC_LOGGING = getBoolean(properties, "use_async_logging", true);
        USE_OPTIMIZED_ENTITY_TRACKING = getBoolean(properties, "use_optimized_entity_tracking", true);
        USE_MULTIPLE_NETTY_EVENT_LOOPS = getBoolean(properties, "experimental0_use_multiple_netty_event_loops", false);
        try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            properties.store(out, "Configuration file for VMP");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void init() {
    }

    private static int getInt(Properties properties, String key, int def) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (NumberFormatException e) {
            properties.setProperty(key, String.valueOf(def));
            return def;
        }
    }

    private static boolean getBoolean(Properties properties, String key, boolean def) {
        try {
            return parseBoolean(properties.getProperty(key));
        } catch (NumberFormatException e) {
            properties.setProperty(key, String.valueOf(def));
            return def;
        }
    }

    private static boolean parseBoolean(String string) {
        if (string == null) throw new NumberFormatException("null");
        if (string.trim().equalsIgnoreCase("true")) return true;
        if (string.trim().equalsIgnoreCase("false")) return false;
        throw new NumberFormatException(string);
    }

}
