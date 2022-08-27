package pl.skidam.automodpack.client.modpack;

import pl.skidam.automodpack.utils.UnZipper;

import java.io.File;
import java.io.IOException;

import static pl.skidam.automodpack.AutoModpackMain.*;
import static pl.skidam.automodpack.client.modpack.DownloadModpack.preload;

public class UnZip {

    public static boolean alreadySetupShutdownHook;

    public UnZip(File out, String ModpackUpdated) {

        alreadySetupShutdownHook = false;

        // Add this to shut down hook
        if (ModpackUpdated.equals("false")) {
            if (!alreadySetupShutdownHook) { // ignore it or me if I am stupid
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        if (out.exists()) {
                            // Start unzip
                            LOGGER.info("Unzipping!");
                            try {
                                new UnZipper(out, new File("./"), "");
                            } catch (IOException e) {
                                LOGGER.error("Error while unzipping!\n" + e);
                                e.printStackTrace();
                            }
                            LOGGER.info("Successfully unzipped!");
                        }
                    }));
                alreadySetupShutdownHook = true;
            }
        } else if (ModpackUpdated.equals("true")) {
            if (out.exists()) {
                // Start unzip
                LOGGER.info("Unzipping!");
                try {
                    new UnZipper(out, new File("./"), "");
                } catch (IOException e) {
                    LOGGER.error("Error while unzipping!\n" + e);
                    e.printStackTrace();
                }
                LOGGER.info("Successfully unzipped!");
            }
        }

        // delete old mods
        new DeleteMods(preload, ModpackUpdated);
    }
}

