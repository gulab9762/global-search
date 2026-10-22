package com.example.testmod;

import net.fabricmc.api.ModInitializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Super lightweight standalone Fabric mod.
 * Has ZERO dependency on Global Search or external loggers!
 * Manages a single boolean config "enableMagicMode" saved in config/testmod.properties.
 */
public class TestMod implements ModInitializer {

    public static final String MOD_ID = "testmod";
    private static final File CONFIG_FILE = new File("config/testmod.properties");
    public static boolean enableMagicMode = true;

    @Override
    public void onInitialize() {
        System.out.println("[TestMod] Initializing standalone test mod...");
        loadConfig();
    }

    public static void loadConfig() {
        try {
            if (!CONFIG_FILE.getParentFile().exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
            }
            Properties props = new Properties();
            if (CONFIG_FILE.exists()) {
                try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
                    props.load(in);
                }
                enableMagicMode = Boolean.parseBoolean(props.getProperty("enableMagicMode", "true"));
            } else {
                saveConfig();
            }
            LOGGER_LOG("Loaded enableMagicMode = " + enableMagicMode);
        } catch (Throwable t) {
            System.err.println("[TestMod] Failed loading config: " + t.getMessage());
        }
    }

    public static void saveConfig() {
        try {
            Properties props = new Properties();
            props.setProperty("enableMagicMode", String.valueOf(enableMagicMode));
            try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
                props.store(out, "TestMod Standalone Configuration");
            }
            LOGGER_LOG("Saved enableMagicMode = " + enableMagicMode);
        } catch (Throwable t) {
            System.err.println("[TestMod] Failed saving config: " + t.getMessage());
        }
    }

    private static void LOGGER_LOG(String msg) {
        System.out.println("[TestMod] " + msg);
    }
}
