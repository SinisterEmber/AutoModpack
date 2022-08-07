package pl.skidam.automodpack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModEnvironment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import org.apache.commons.io.FileUtils;
import pl.skidam.automodpack.config.Config;
import pl.skidam.automodpack.server.HostModpack;
import pl.skidam.automodpack.utils.UnZipper;
import pl.skidam.automodpack.utils.Zipper;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Scanner;

import static org.apache.commons.lang3.ArrayUtils.contains;
import static pl.skidam.automodpack.AutoModpackMain.*;
import static pl.skidam.automodpack.utils.GetIPV4Address.getIPV4Address;

public class AutoModpackServer implements DedicatedServerModInitializer {

    public static final File changelogsDir = new File("./AutoModpack/changelogs/");
    public static final File modpackDir = new File("./AutoModpack/modpack/");
    public static final File modpackZip = new File("./AutoModpack/modpack.zip");
    public static final File modpackClientModsDir = new File("./AutoModpack/modpack/[CLIENT] mods/");
    public static final File modpackModsDir = new File("./AutoModpack/modpack/mods/");
    // public static final File modpackConfDir = new File("./AutoModpack/modpack/config/");
    public static final File modpackDeleteTxt = new File("./AutoModpack/modpack/delmods.txt");
    public static final File serverModsDir = new File("./mods/");
    public static String publicServerIP;

    @Override
    public void onInitializeServer() {
        LOGGER.info("Welcome to AutoModpack on Server!");

        HostModpack.isRunning = false;

        publicServerIP = getIPV4Address();

        genModpack();

        // Packets
        ServerLoginNetworking.registerGlobalReceiver(AM_LINK, this::onSuccess);
        ServerLoginConnectionEvents.QUERY_START.register(this::onLoginStart);

        if (modpackZip.exists()) {
            ServerLifecycleEvents.SERVER_STARTED.register(server -> HostModpack.start());
            ServerLifecycleEvents.SERVER_STOPPING.register(server -> HostModpack.stop());
        }
    }

    public static void genModpack() {

        clientMods();

        // Sync mods and automatically generate delmods.txt
        if (Config.SYNC_MODS) {
            LOGGER.info("Synchronizing mods from server to modpack");

            // Make array of mods
            String[] oldMods = modpackModsDir.list();
            deleteAllMods();
            cloneMods();
            clientMods();
            onlyServerSideMods();
            String[] newMods = modpackModsDir.list();

            // Changelog generation
            SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd");
            String date = ft.format(new Date());

            // Check how many files is in the changelogsDir containing the date
            String[] changelogs = changelogsDir.list();
            int changelogsCount = 1;
            assert changelogs != null;
            for (String changelog : changelogs) {
                if (changelog.contains(date)) {
                    changelogsCount++;
                }
            }

            // Create changelog file
            File changelog = new File(changelogsDir + "/" + "changelog-" + date + "-" + changelogsCount + ".txt");
            try {
                changelog.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            assert newMods != null;
            for (String mod : newMods) {
                if (!contains(oldMods, mod)) {
                    // Added mod
                    try {
                        FileUtils.writeStringToFile(changelog, " + " + mod + "\n", Charset.defaultCharset(),true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Compare new to old mods and generate delmods.txt
            assert oldMods != null;
            for (String mod : oldMods) {
                if (!contains(newMods, mod) && !mod.equals(correctName)) { // fix to #34
                    try {
                        // Check if mod is not already in delmods.txt
                        if (!FileUtils.readLines(modpackDeleteTxt, Charset.defaultCharset()).contains(mod)) {
                            LOGGER.info("Writing " + mod + " to delmods.txt");
                            FileUtils.writeStringToFile(modpackDeleteTxt, mod + "\n", Charset.defaultCharset(), true);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Deleted mod
                    try {
                        FileUtils.writeStringToFile(changelog, " - " + mod + "\n", Charset.defaultCharset(), true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Check if changelog is empty
            try {
                if (FileUtils.readLines(changelog, Charset.defaultCharset()).isEmpty()) {
                    changelog.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Check if in delmods.txt there are not mods which are in serverModsDir
            try {
                for (String delMod : FileUtils.readLines(modpackDeleteTxt, Charset.defaultCharset())) {
                    if (Objects.requireNonNull(serverModsDir.listFiles()).length > 0) {
                        for (File file : Objects.requireNonNull(serverModsDir.listFiles())) {
                            String FNLC = file.getName().toLowerCase(); // FileNameLowerCase
                            if (FNLC.endsWith(".jar") && !FNLC.contains("automodpack")) {
                                if (file.getName().equals(delMod)) {
                                    LOGGER.error("Removing " + delMod + " from delmods.txt");
                                    Scanner sc = new Scanner(modpackDeleteTxt);
                                    StringBuilder sb = new StringBuilder();
                                    while (sc.hasNextLine()) {
                                        sb.append(sc.nextLine()).append("\n");
                                    }
                                    sc.close();
                                    String result = sb.toString();
                                    result = result.replace(delMod, "");
                                    PrintWriter writer = new PrintWriter(modpackDeleteTxt);
                                    writer.append(result);
                                    writer.flush();
                                    writer.close();
                                }
                            }
                        }
                    }

                    if (delMod.equals(correctName)) {
                        Scanner sc = new Scanner(modpackDeleteTxt);
                        StringBuilder sb = new StringBuilder();
                        while (sc.hasNextLine()) {
                            sb.append(sc.nextLine()).append("\n");
                        }
                        sc.close();
                        String result = sb.toString();
                        result = result.replace(delMod, "");
                        PrintWriter writer = new PrintWriter(modpackDeleteTxt);
                        writer.append(result);
                        writer.flush();
                        writer.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Remove blank lines from delmods.txt
        try {
            Scanner file = new Scanner(modpackDeleteTxt);
            PrintWriter writer = new PrintWriter(modpackDeleteTxt + ".tmp");

            while (file.hasNext()) {
                String line = file.nextLine();
                if (!line.isEmpty()) {
                    writer.write(line);
                    writer.write("\n");
                }
            }

            file.close();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        modpackDeleteTxt.delete();
        new File (modpackDeleteTxt + ".tmp").renameTo(modpackDeleteTxt);

        LOGGER.info("Creating modpack");
        if (modpackZip.exists()) {
            FileUtils.deleteQuietly(modpackZip);
        }

        try {
            new Zipper(modpackDir, modpackZip);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }

        LOGGER.info("Modpack created");

        // TODO GDRIVE UPLOAD
//        if (Config.EXTERNAL_MODPACK_HOST.startsWith("https://drive.google.com/drive/")) {
//            LOGGER.warn("Uploading modpack to Google Drive");
//            try {
//                GoogleDriveUpload.uploadModpack();
//            } catch (IOException | GeneralSecurityException e) {
//                LOGGER.error("Failed upload modpack to Google Drive\n" + e);
//            }
//        }
    }

    private static void cloneMods() {
        for (File file : Objects.requireNonNull(serverModsDir.listFiles())) {
            if (file.getName().endsWith(".jar") && !file.getName().toLowerCase().contains("automodpack")) {
                try {
                    FileUtils.copyFileToDirectory(file, modpackModsDir);
                } catch (IOException e) {
                    LOGGER.error("Error while cloning mods from server to modpack");
                    e.printStackTrace();
                }
            }
        }
    }

    private static void onlyServerSideMods() {
        if (!Config.AUTO_EXCLUDE_SERVER_SIDE_MODS) return;
        LOGGER.info("Excluding server side mods...");
        for (File file : Objects.requireNonNull(serverModsDir.listFiles())) {
            if (file.getName().endsWith(".jar") && !file.getName().toLowerCase().contains("automodpack")) {
                try {
                    new UnZipper(file, new File("./AutoModpack/temp/" + file.getName()), "fabric.mod.json");
                    LOGGER.info("Found fabric.mod.json for " + file.getName());
                } catch (IOException e) {
                    try {
                        new UnZipper(file, new File("./AutoModpack/temp/" + file.getName()), "quilt.mod.json");
                        LOGGER.info("Found quilt.mod.json for " + file.getName());
                    } catch (IOException ex) {
                        return;
                    }
                }

                // check if in the temp folder is a fabric.mod.json or quilt.mod.json file
                File[] tempFiles = new File("./AutoModpack/temp/").listFiles();
                if (tempFiles == null) return;
                for (File tempFile : tempFiles) {
                    File[] tempFiles2 = tempFile.listFiles();
                    if (tempFiles2 == null) return;
                    for (File tempFile2 : tempFiles2) {
                        if (tempFile2.getName().equals("fabric.mod.json") || tempFile2.getName().equals("quilt.mod.json")) {
                            try {
                                JsonObject jsonObject = JsonParser.parseReader(new FileReader(tempFile2)).getAsJsonObject();
                                if (jsonObject.has("\"environment\": \"server\"")) {
                                    File serverSideModInModpack = new File(modpackModsDir + file.getName());
                                    if (serverSideModInModpack.exists()) {
                                        FileUtils.deleteQuietly(serverSideModInModpack);
                                        LOGGER.info(file.getName() + " is server side mod and has been excluded from modpack");
                                    }
                                } else {
                                    LOGGER.info(file.getName() + " is not a server side mod");
                                }
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    FileUtils.deleteQuietly(tempFile);
                }
            }
        }
    }

    private static void clientMods() {
        for (File file : Objects.requireNonNull(modpackClientModsDir.listFiles())) {
            if (file.getName().endsWith(".jar") && !file.getName().toLowerCase().contains("automodpack")) {
                try {
                    FileUtils.copyFileToDirectory(file, modpackModsDir);
                } catch (IOException e) {
                    LOGGER.error("Error while cloning mods from client to modpack");
                    e.printStackTrace();
                }
            }
        }
    }

    private static void deleteAllMods() {
        for (File file : Objects.requireNonNull(modpackModsDir.listFiles())) {
            if (!file.delete()) {
                LOGGER.error("Error while deleting the file: " + file);
            }
        }
    }

    private void onSuccess(MinecraftServer minecraftServer, ServerLoginNetworkHandler serverLoginNetworkHandler, boolean understood, PacketByteBuf buf, ServerLoginNetworking.LoginSynchronizer loginSynchronizer, PacketSender sender) {
        // Successfully sent link to client, client can join and play on server.
        if (!understood || !buf.readString().equals("1")) {
            if (!Config.ONLY_OPTIONAL_MODPACK) { // Accept player to join while optional modpack is enabled // TODO make it better
                serverLoginNetworkHandler.disconnect(Text.of("You have to install \"AutoModpack\" mod to play on this server! https://modrinth.com/mod/automodpack/versions"));
                LOGGER.warn("Player " + serverLoginNetworkHandler.getConnectionInfo() + " has not installed \"AutoModpack\" mod");
            } else {
                LOGGER.warn("Player " + serverLoginNetworkHandler.getConnectionInfo() + " has not installed modpack");
                serverLoginNetworkHandler.acceptPlayer();
            }
        } else {
            LOGGER.warn("Player " + serverLoginNetworkHandler.getConnectionInfo() + " has installed modpack");
            serverLoginNetworkHandler.acceptPlayer();
        }
    }

    private void onLoginStart(ServerLoginNetworkHandler serverLoginNetworkHandler, MinecraftServer minecraftServer, PacketSender sender, ServerLoginNetworking.LoginSynchronizer loginSynchronizer) {
        // Get minecraft player ip if player is in local network give him local address to modpack
        String playerIp = serverLoginNetworkHandler.getConnection().getAddress().toString();

        PacketByteBuf outBuf = PacketByteBufs.create();

        if (playerIp.contains("127.0.0.1") || playerIp.contains(publicServerIP)) {
            outBuf.writeString(HostModpack.modpackHostIpForLocalPlayers);
        } else {
            outBuf.writeString(AutoModpackMain.link);
        }

        sender.sendPacket(AutoModpackMain.AM_LINK, outBuf);
    }
}