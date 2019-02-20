package org.iota.ict.api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

public final class GithubGateway {

    private GithubGateway() {
    }

    protected static String BASE_URL = "https://api.github.com";

    public static JSONObject getRepoInfo(String userSlashRepo) {
        String response = send_API_POST_Request("/repos/" + userSlashRepo);
        return new JSONObject(response);
    }

    public static String getLatestReleaseLabel(String userSlashRepo) {
        JSONObject latestRelease = getLatestRelease(userSlashRepo);
        return latestRelease.getString("tag_name");
    }

    public static String getContents(String userSlashRepo, String branch, String path) {
        return HttpGateway.sendPostRequest("https://raw.githubusercontent.com/" + userSlashRepo + "/" + branch + "/" + path);
    }

    public static URL getAssetDownloadUrl(String userSlashRepo, String label) {
        JSONObject release = getRelease(userSlashRepo, label);
        return getAssetDownloadUrlOfRelease(release);
    }

    protected static JSONObject getLatestRelease(String userSlashRepo) {
        JSONArray releases = getReleases(userSlashRepo);
        if (releases.length() == 0)
            throw new RuntimeException("No releases in repository " + userSlashRepo);
        return releases.getJSONObject(0);
    }

    protected static URL getAssetDownloadUrlOfRelease(JSONObject release) {
        JSONArray assets = release.getJSONArray("assets");
        if (assets.length() > 0) {
            JSONObject asset = assets.getJSONObject(0);
            return getDownloadURLOfAsset(asset);
        }
        throw new RuntimeException("No assets found in release '" + release.getString("tag_name") + "'");
    }

    protected static URL getDownloadURLOfAsset(JSONObject asset) {
        String downloadUrl = asset.getString("browser_download_url");
        try {
            return new URL(downloadUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException("should never happen", e);
        }
    }

    protected static JSONObject getRelease(String userSlashRepo, String label) {
        JSONArray releases = getReleases(userSlashRepo);
        for (int i = 0; i < releases.length(); i++) {
            JSONObject release = releases.getJSONObject(i);
            if (release.getString("tag_name").equals(label))
                return release;
        }
        throw new RuntimeException("No release with label '" + label + "' found in '" + userSlashRepo + "'.");
    }

    protected static JSONArray getReleases(String userSlashRepo) {
        String response = send_API_POST_Request("/repos/" + userSlashRepo + "/releases");
        return new JSONArray(response);
    }

    protected static String send_API_POST_Request(String path) {
        return HttpGateway.sendPostRequest(BASE_URL + path);
    }
}
