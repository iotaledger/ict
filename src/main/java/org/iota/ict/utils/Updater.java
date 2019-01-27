package org.iota.ict.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.Main;
import org.iota.ict.api.GithubGateway;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class Updater {

    private static long lastTimeChckecForUpdates = 0;
    private static String availableUpdate = null;
    private static final Logger LOGGER = LogManager.getLogger("Updater");

    public static void update(String version) throws IOException {
        URL url = GithubGateway.getAssetDownloadUrl(Constants.ICT_REPOSITORY, version);
        String targetFileName = "ict-" + version + ".jar";
        Path target = Constants.WORKING_DIRECTORY.resolve(targetFileName);
        LOGGER.info("Ict Update: downloading precompiled .jar file from " + url + " into " + target + " ...");
        try (InputStream in = url.openStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        LOGGER.info("Download complete. Please run the new version ("+targetFileName+").");
    }

    public static void checkForUpdatesIfYouHaveNotDoneSoInALongTime() {
        if(lastTimeChckecForUpdates + Constants.CHECK_FOR_UPDATES_INTERVAL_MS < System.currentTimeMillis())
            checkForUpdates();
    }

    public static void checkForUpdates() {
        LOGGER.info("Checking for updates ...");
        lastTimeChckecForUpdates = System.currentTimeMillis();

        try {
            String latestReleaseVersion = GithubGateway.getLatestReleaseLabel(Constants.ICT_REPOSITORY);
            if (VersionComparator.getInstance().compare(Constants.ICT_VERSION, latestReleaseVersion) < 0) {
                LOGGER.warn(">>>>> A new release of Ict is available. Please update to " + latestReleaseVersion + "! <<<<<");
                availableUpdate = latestReleaseVersion;
            }
            else {
                LOGGER.info("You are already up-to-date!");
                availableUpdate = null;
            }
        } catch (Throwable t) {
            LOGGER.error("Failed checking for updates", t);
        }
    }

    public static String getAvailableUpdate() {
        return availableUpdate;
    }
}