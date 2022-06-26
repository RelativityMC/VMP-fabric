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
        TARGET_CHUNK_SEND_RATE = getInt(properties, "target_chunk_send_rate", 120);
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

}
