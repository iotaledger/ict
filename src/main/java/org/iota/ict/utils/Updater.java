package org.iota.ict.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.api.GithubGateway;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class Updater {

    private static final Logger LOGGER = LogManager.getLogger("Updater");

    public static String getLabelOfAvailableUpdate() {
        String latestReleaseLabel = GithubGateway.getLatestReleaseLabel(Constants.ICT_REPOSITORY);
        return VersionComparator.getInstance().compare(latestReleaseLabel, Constants.ICT_VERSION) > 0 ? latestReleaseLabel : null;
    }

    public static void update(String version) throws IOException {
        URL url = GithubGateway.getAssetDownloadUrl(Constants.ICT_REPOSITORY, version);
        Path target = Constants.WORKING_DIRECTORY.resolve("ict-"+version+".jar");
        LOGGER.info("Ict Update: downloading precompiled .jar file from " + url + " into "+target+" ...");
        try (InputStream in = url.openStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        LOGGER.info("Download complete. Please run the new version.");
    }
}