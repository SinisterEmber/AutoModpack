package pl.skidam.automodpack.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

import static pl.skidam.automodpack.AutoModpackMain.LOGGER;

public class Config {
    public static boolean DANGER_SCREEN;
    public static boolean CHECK_UPDATES_BUTTON;
    public static boolean DELETE_MODPACK_BUTTON;
    public static boolean MODPACK_HOST;
    public static boolean GENERATE_MODPACK_ON_LAUNCH;
    public static boolean SYNC_MODS;
    public static boolean ONLY_OPTIONAL_MODPACK;
    public static boolean AUTO_EXCLUDE_SERVER_SIDE_MODS;
//    public static boolean AUTO_EXCLUDE_CLIENT_SIDE_MODS;
//    public static boolean DISABLE_ALL_OTHER_MODS_ON_CLIENT;
    public static int HOST_PORT;
    public static int HOST_THREAD_COUNT;
    public static String HOST_EXTERNAL_IP;
    public static String HOST_EXTERNAL_IP_FOR_LOCAL_PLAYERS;
    public static String EXTERNAL_MODPACK_HOST;
    public static final Path path = FabricLoader.getInstance().getConfigDir().resolve("automodpack.properties");

    static {
        final Properties properties = new Properties();
        if (Files.isRegularFile(path)) {
            try (InputStream in = Files.newInputStream(path, StandardOpenOption.CREATE)) {
                properties.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        DANGER_SCREEN = getBoolean(properties, "danger_screen", true);
        CHECK_UPDATES_BUTTON = getBoolean(properties, "check_updates_button", true);
        DELETE_MODPACK_BUTTON = getBoolean(properties, "delete_modpack_button", true);
        MODPACK_HOST = getBoolean(properties, "modpack_host", true);
        GENERATE_MODPACK_ON_LAUNCH = getBoolean(properties, "generate_modpack_on_launch", true);
        SYNC_MODS = getBoolean(properties, "sync_mods", true);
        ONLY_OPTIONAL_MODPACK = getBoolean(properties, "only_optional_modpack", false);
        AUTO_EXCLUDE_SERVER_SIDE_MODS = getBoolean(properties, "auto_exclude_server_side_mods", true);
//        AUTO_EXCLUDE_CLIENT_SIDE_MODS = getBoolean(properties, "auto_exclude_client_side_mods", true);      - NON SENSE
//        DISABLE_ALL_OTHER_MODS_ON_CLIENT = getBoolean(properties, "disable_all_other_mods_on_client", false);
        HOST_PORT = getInt(properties, "host_port", 30037);
        HOST_THREAD_COUNT = getInt(properties, "host_thread_count", 2);
        HOST_EXTERNAL_IP = getString(properties, "host_external_ip");
        HOST_EXTERNAL_IP_FOR_LOCAL_PLAYERS = getString(properties, "host_external_ip_for_local_players");
        EXTERNAL_MODPACK_HOST = getString(properties, "external_modpack_host");

        try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            properties.store(out, "Configuration file for AutoModpack");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void init() {
    }

    public void save() {
        LOGGER.info("Saving AutoModpack config...");

        final Properties properties = new Properties();

        properties.setProperty("danger_screen", String.valueOf(DANGER_SCREEN));
        properties.setProperty("check_updates_button", String.valueOf(CHECK_UPDATES_BUTTON));
        properties.setProperty("delete_modpack_button", String.valueOf(DELETE_MODPACK_BUTTON));
        properties.setProperty("modpack_host", String.valueOf(MODPACK_HOST));
        properties.setProperty("generate_modpack_on_launch", String.valueOf(SYNC_MODS));
        properties.setProperty("sync_mods", String.valueOf(SYNC_MODS));
        properties.setProperty("only_optional_modpack", String.valueOf(ONLY_OPTIONAL_MODPACK));
        properties.setProperty("auto_exclude_server_side_mods", String.valueOf(AUTO_EXCLUDE_SERVER_SIDE_MODS));
//        properties.setProperty("auto_exclude_client_side_mods", String.valueOf(AUTO_EXCLUDE_CLIENT_SIDE_MODS));
//        properties.setProperty("disable_all_other_mods_on_client", String.valueOf(DISABLE_ALL_OTHER_MODS_ON_CLIENT)); // TODO make it work
        properties.setProperty("host_port", String.valueOf(HOST_PORT));
        properties.setProperty("host_thread_count", String.valueOf(HOST_THREAD_COUNT));
        properties.setProperty("host_external_ip", HOST_EXTERNAL_IP);
        properties.setProperty("host_external_ip_for_local_players", HOST_EXTERNAL_IP_FOR_LOCAL_PLAYERS);
        properties.setProperty("external_modpack_host", EXTERNAL_MODPACK_HOST);

        try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            properties.store(out, "Configuration file for AutoModpack");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static int getInt(Properties properties, String key, int def) {
        try {
            return Integer.parseUnsignedInt(properties.getProperty(key));
        } catch (NumberFormatException e) {
            if (properties.containsKey(key) && Files.isRegularFile(path)) {
                LOGGER.error("Invalid value for " + key + " in automodpack.properties. Value must be an integer. Value restarted to " + def);
            } else if (Files.isRegularFile(path) && !properties.containsKey(key)) {
                LOGGER.info("Added new option to automodpack config (automodpack.properties): " + key + " = " + def);
            }
            properties.setProperty(key, String.valueOf(def));
            return def;
        }
    }

    private static String getString(Properties properties, String key) {
        if (properties.getProperty(key) == null) {
            properties.setProperty(key, "");
            if (Files.isRegularFile(path) && !properties.containsKey(key)) {
                LOGGER.info("Added new option to automodpack config (automodpack.properties): " + key + " = \"\"");
            }
            return "";
        }
        return properties.getProperty(key);
    }

    private static boolean getBoolean(Properties properties, String key, boolean def) {
        String booleanValue = String.valueOf(Boolean.parseBoolean(properties.getProperty(key)));
        if (booleanValue.equals(properties.getProperty(key))) {
            return Boolean.parseBoolean(booleanValue);
        } else {
            if (properties.containsKey(key) && Files.isRegularFile(path)) {
                LOGGER.error("Invalid value for " + key + " in automodpack.properties. Value must be true or false. Value restarted to " + def);
            } else if (Files.isRegularFile(path) && !properties.containsKey(key)) {
                LOGGER.info("Added new option to automodpack config (automodpack.properties): " + key + " = " + def);
            }
            properties.setProperty(key, String.valueOf(def));
            return def;
        }
    }
}