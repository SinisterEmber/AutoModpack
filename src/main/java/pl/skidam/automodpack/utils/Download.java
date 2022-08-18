package pl.skidam.automodpack.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

import static pl.skidam.automodpack.AutoModpackMain.LOGGER;

public class Download {

    public static float progress;
    public static String averageInternetConnectionSpeed;
    public static boolean Download(String link, File output) {

        if (InternetConnectionCheck.InternetConnectionCheck(link)) {
            try {
                URL url = new URL(link);

                progress = 0;
                averageInternetConnectionSpeed = "";
                long startTime = System.currentTimeMillis();

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("X-Minecraft-Username", GetMinecraftUserName.getMinecraftUserName());
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000); // 10 seconds
                connection.setReadTimeout(10000); // 10 seconds as well
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    double fileSize = (double) connection.getContentLengthLong();
                    BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                    FileOutputStream fos = new FileOutputStream(output);
                    BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
                    byte[] buffer = new byte[1024];
                    double downloaded = 0.00;
                    int read;
                    double percentDownloaded;
                    String lastPercent = null;
                    String percent = "0";

                    while ((read = in.read(buffer, 0, 1024)) >= 0) { // TODO make it real time, not average!
                        bout.write(buffer, 0, read);
                        downloaded += read;
                        percentDownloaded = (downloaded * 100) / fileSize;

                        long endTime = System.currentTimeMillis();
                        double rate = (((downloaded / 1024) / ((endTime - startTime) / 1000.0)) * 8);
                        rate = Math.round(rate * 100.0) / 100.0;
                        if (rate > 1000) {
                            averageInternetConnectionSpeed = String.format("%.1f", (rate / 1024)).concat(" Mb/s");
                        } else {
                            averageInternetConnectionSpeed = String.format("%.1f", rate).concat(" Kb/s");
                        }

                        // if lastPercent != percent
                        if (!Objects.equals(lastPercent, percent)) {
                            percent = (String.format("%.1f", percentDownloaded));
                            percent.replaceAll(",", ".");
                            try {
                                progress = Float.parseFloat(percent);
                            } catch (NumberFormatException e) {
                                LOGGER.error("Error while parsing progress to float: " + percent);
                            }
                            if (percent.contains("0.0") && !percent.equals("0.0")) {
                                LOGGER.info("Downloaded " + percent.split("\\.")[0] + "%" + " with average internet connection speed of " + averageInternetConnectionSpeed);
                            }
                            lastPercent = percent;

                            // if lastPercent == percent
                        } else {
                            percent = (String.format("%.1f", percentDownloaded));
                        }
                    }
                    bout.close();
                    in.close();
                }

            } catch (IOException ex) {
                new Error();
                ex.printStackTrace();
                return true;
            }
            return false;
        } else {
            return true;
        }
    }
}