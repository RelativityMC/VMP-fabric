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
    public static final boolean USE_PACKET_PRIORITY_SYSTEM_BLOCK_UPDATE_CONSOLIDATION;
    public static final boolean USE_ASYNC_LOGGING;
    public static final boolean USE_OPTIMIZED_ENTITY_TRACKING;
    public static final boolean USE_MULTIPLE_NETTY_EVENT_LOOPS;
    public static final boolean USE_ASYNC_PORTALS;
    public static final boolean USE_ASYNC_CHUNKS_ON_LOGIN;
    public static final boolean PRECACHE_BIOME_LOOKUP;
    public static final boolean SHOW_ASYNC_LOADING_MESSAGES;

    static {
        final Properties properties = new Properties();
        final Properties newProperties = new Properties();
        final Path path = FabricLoader.getInstance().getConfigDir().resolve("vmp.properties");
        if (Files.isRegularFile(path)) {
            try (InputStream in = Files.newInputStream(path, StandardOpenOption.CREATE)) {
                properties.load(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        TARGET_CHUNK_SEND_RATE = getInt(properties, newProperties, "target_chunk_send_rate", -1);
        USE_PACKET_PRIORITY_SYSTEM = getBoolean(properties, newProperties, "use_packet_priority_system", true);
        USE_PACKET_PRIORITY_SYSTEM_BLOCK_UPDATE_CONSOLIDATION = getBoolean(properties, newProperties, "exp_use_packet_priority_system_block_update_consolidation", false);
        USE_ASYNC_LOGGING = getBoolean(properties, newProperties, "use_async_logging", true);
        USE_OPTIMIZED_ENTITY_TRACKING = getBoolean(properties, newProperties, "use_optimized_entity_tracking", true);
        USE_MULTIPLE_NETTY_EVENT_LOOPS = getBoolean(properties, newProperties, "use_multiple_netty_event_loops", true);
        USE_ASYNC_PORTALS = getBoolean(properties, newProperties, "use_async_portals", true);
        USE_ASYNC_CHUNKS_ON_LOGIN = getBoolean(properties, newProperties, "use_async_chunks_on_login", true);
        PRECACHE_BIOME_LOOKUP = getBoolean(properties, newProperties, "precache_biome_lookup", true);
        SHOW_ASYNC_LOADING_MESSAGES = getBoolean(properties, newProperties, "show_async_loading_messages", true);
        try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            newProperties.store(out, "Configuration file for VMP");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void init() {
    }

    private static int getInt(Properties properties, Properties newProperties, String key, int def) {
        try {
            final int i = Integer.parseInt(properties.getProperty(key));
            newProperties.setProperty(key, String.valueOf(i));
            return i;
        } catch (NumberFormatException e) {
            newProperties.setProperty(key, String.valueOf(def));
            return def;
        }
    }

    private static boolean getBoolean(Properties properties, Properties newProperties, String key, boolean def) {
        try {
            final boolean b = parseBoolean(properties.getProperty(key));
            newProperties.setProperty(key, String.valueOf(b));
            return b;
        } catch (NumberFormatException e) {
            newProperties.setProperty(key, String.valueOf(def));
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
