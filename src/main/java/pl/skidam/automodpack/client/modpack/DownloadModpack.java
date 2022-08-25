package pl.skidam.automodpack.client.modpack;

import net.minecraft.client.MinecraftClient;

import org.apache.commons.io.FileUtils;
import pl.skidam.automodpack.client.ui.DangerScreen;
import pl.skidam.automodpack.client.ui.LoadingScreen;
import pl.skidam.automodpack.config.Config;
import pl.skidam.automodpack.server.HostModpack;
import pl.skidam.automodpack.ui.ScreenBox;
import pl.skidam.automodpack.utils.Download;
import pl.skidam.automodpack.utils.generateContentList;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

import static pl.skidam.automodpack.AutoModpackClient.isOnServer;
import static pl.skidam.automodpack.AutoModpackMain.*;

public class DownloadModpack {

    private static boolean preload;

    public DownloadModpack() {

        if (CheckModpack.update && link.endsWith("/modpack")) {
            LOGGER.info("Updating modpack");
            String baseLink = link.substring(0, link.lastIndexOf("/"));
            String contentLink = baseLink + "/content";
            LOGGER.warn(contentLink);
            if (Download.Download(contentLink, HostModpack.MODPACK_CONTENT_FILE.toFile())) {
                LOGGER.info("Failed to download content file!");
                return;
            }

            LOGGER.info("Successfully downloaded content file!");

            try {
                Scanner  serverContentList = new Scanner(HostModpack.MODPACK_CONTENT_FILE.toFile());
                List<String> clientContentList = generateContentList.generateContentList(new File("./AutoModpack/modpacks/modpack.zip"));

                while (serverContentList.hasNextLine()) {
                    String serverLine = serverContentList.nextLine();
                    String serverLineOfName = serverLine.substring(0, serverLine.indexOf(" =+= "));
                    long serverLineOfSize = Long.parseLong(serverLine.substring(serverLine.lastIndexOf(" ")+1));
                    String updateLink = baseLink + "/" + serverLineOfName;
                    File file = new File("./" + serverLineOfName); // TODO check in the zip file
                    LOGGER.warn("{} {} {} {}", serverLine, serverLineOfName, serverLineOfSize, updateLink);
                    if (serverLineOfName.endsWith("/")) {
                        if (!file.exists()) {
                            file.mkdirs();
                        }
                    } else if (clientContentList.contains(serverLineOfName)) {
                        if (file.length() != serverLineOfSize) {
                            LOGGER.warn("{} exists but doesn't match the size, downloading from {}!", serverLineOfName, updateLink);
                            if (Download.Download(updateLink, new File("./AutoModpack/updateTemp/" + serverLineOfName))) { // TODO Download to the zip file
                                LOGGER.info("Failed to download {} from {}!", serverLineOfName, updateLink);
                                return;
                            }
                        }
                    } else {
                        LOGGER.warn("{} doesn't exists, downloading from {}!", serverLineOfName, updateLink);
                        if (Download.Download(updateLink, new File("./AutoModpack/updateTemp/" + serverLineOfName))) { // TODO Download to the zip file
                            LOGGER.info("Failed to download {} from {}!", serverLineOfName, updateLink);
                            return;
                        }
                    }
                }

                LOGGER.info("Successfully updated modpack!");

            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            new UnZip(out, "true");

            if (!modsPath.getFileName().toString().equals("mods")) {
                try {
                    FileUtils.moveDirectory(new File("./mods/"), new File(modsPath.toFile() + File.separator));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } else {
            LOGGER.info("Downloading modpack from {}...", link);

            // Download and check if download is successful *magic*

            if (Download.Download(link, out)) {
                LOGGER.info("Failed to download modpack!");
                return;
            }

            LOGGER.info("Successfully downloaded modpack!");

            new UnZip(out, "true");

            if (!modsPath.getFileName().toString().equals("mods")) {
                try {
                    FileUtils.moveDirectory(new File("./mods/"), new File(modsPath.toFile() + File.separator));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
//        try {
//            new Relaunch();
//        } catch (Throwable e) {
//            LOGGER.error("Failed to relaunch minecraft! " + e);
//            e.printStackTrace();
//            new ScreenBox("Updated modpack, restart your game!");
//        }
        if (preload) {
            new ScreenBox("Updated modpack, restart your game!");
        }
    }

    public static class prepare {

        public static boolean DangerScreenWasShown = false;

        public prepare(boolean preload) {

            DownloadModpack.preload = preload;

            if (preload) {
                new DownloadModpack();
                return;
            }

            while (true) {
                if (MinecraftClient.getInstance().currentScreen != null) {
                    if (!isOnServer) {
                        DangerScreenWasShown = false;
                        break;
                    }
                }

                if (isOnServer) {
                    if (MinecraftClient.getInstance().world != null) {
                        MinecraftClient.getInstance().world.disconnect();
                    }

                    assert MinecraftClient.getInstance().currentScreen != null;
                    if (MinecraftClient.getInstance().currentScreen.toString().toLowerCase().contains("419")) {
                        DangerScreenWasShown = false;
                        isOnServer = false;
                        break;
                    }
                }
            }

            if (isVelocity) {
                while (true) {
                    if (MinecraftClient.getInstance().currentScreen != null) {
                        if (MinecraftClient.getInstance().currentScreen.toString().toLowerCase().contains("disconnected") || MinecraftClient.getInstance().currentScreen.toString().toLowerCase().contains("419")) {
                            break;
                        }
                    }
                }
            }

            if (Config.DANGER_SCREEN) {
                MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().setScreen(new DangerScreen()));
            }
            if (!Config.DANGER_SCREEN) {
                CompletableFuture.runAsync(DownloadModpack::new);
                MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().setScreen(new LoadingScreen()));
            }
        }
    }
}