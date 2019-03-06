package org.iota.ict.api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public final class GithubGateway {

    private final static Map<String, String> githubRequestProperties;

    static {
        githubRequestProperties = new HashMap<>();
        githubRequestProperties.put("Content-Type", "application/json");
        githubRequestProperties.put("Accept", "application/vnd.github.v3+json");
    }

    private GithubGateway() {
    }

    private static String BASE_URL = "https://api.github.com";

    public static JSONObject getRepoInfo(String userSlashRepo) {
        String response = send_API_POST_Request("/repos/" + userSlashRepo);
        return new JSONObject(response);
    }

    public static String getLatestReleaseLabel(String userSlashRepo) {
        JSONObject latestRelease = getLatestRelease(userSlashRepo);
        return latestRelease.getString("tag_name");
    }

    public static String getContents(String userSlashRepo, String branch, String path) {
        return HttpGateway.sendGetRequest("https://raw.githubusercontent.com/" + userSlashRepo + "/" + branch + "/" + path, new HashMap<String, String>(), githubRequestProperties);
    }

    public static URL getAssetDownloadUrl(String userSlashRepo, String label) {
        JSONObject release = getRelease(userSlashRepo, label);
        return getAssetDownloadUrlOfRelease(release);
    }

    private static JSONObject getLatestRelease(String userSlashRepo) {
        JSONArray releases = getReleases(userSlashRepo);
        if (releases.length() == 0)
            throw new RuntimeException("No releases in repository " + userSlashRepo);
        return releases.getJSONObject(0);
    }

    private static URL getAssetDownloadUrlOfRelease(JSONObject release) {
        JSONArray assets = release.getJSONArray("assets");
        if (assets.length() > 0) {
            JSONObject asset = assets.getJSONObject(0);
            return getDownloadURLOfAsset(asset);
        }
        throw new RuntimeException("No assets found in release '" + release.getString("tag_name") + "'");
    }

    private static URL getDownloadURLOfAsset(JSONObject asset) {
        String downloadUrl = asset.getString("browser_download_url");
        try {
            return new URL(downloadUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException("should never happen", e);
        }
    }

    private static JSONObject getRelease(String userSlashRepo, String label) {
        JSONArray releases = getReleases(userSlashRepo);
        for (int i = 0; i < releases.length(); i++) {
            JSONObject release = releases.getJSONObject(i);
            if (release.getString("tag_name").equals(label))
                return release;
        }
        throw new RuntimeException("No release with label '" + label + "' found in '" + userSlashRepo + "'.");
    }

    public static JSONArray getReleases(String userSlashRepo) {
        String response = send_API_POST_Request("/repos/" + userSlashRepo + "/releases");
        return new JSONArray(response);
    }

    private static String send_API_POST_Request(String path) {
        return HttpGateway.sendGetRequest(BASE_URL + path, new HashMap<String, String>(), githubRequestProperties);
    }
}
