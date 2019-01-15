package org.iota.ict.ixi;

import org.iota.ict.api.GithubGateway;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.VersionComparator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class IxiModuleInfo {

    public final String version;
    public final String repository;
    public final String name;
    public final String mainClass;
    public final String description;
    public int guiPort ;
    private final String path;
    public final JSONArray supportedVersions;

    public IxiModuleInfo(JSONObject json, String path) throws JSONException {
        version = json.getString("version");
        supportedVersions = json.getJSONArray("supported_versions");
        name = json.getString("name");
        description = json.getString("description");
        repository = json.getString("repository");
        if(!repository.matches("^[a-zA-Z0-9\\-_.]+/[a-zA-Z0-9\\-_.]+$"))
            throw new RuntimeException("Illegal repository declared in module.json: '"+repository+"'. Invalid format or unexpected characters.");
        mainClass = json.getString("main_class");
        guiPort = json.getInt("gui_port");
        this.path = path;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("version", version);
        json.put("description", description);
        json.put("repository", repository);
        json.put("path", path);
        json.put("gui_port", guiPort);
        json.put("main_class", mainClass);
        json.put("supported_versions", supportedVersions);
        return json;
    }

    public String checkForUpdate() {
        try {
            String versionsString = GithubGateway.getContents(repository, "master", "versions.json");
            JSONObject versions = new JSONObject(versionsString);
            String latestCompatibleVersion = versions.getString(Constants.ICT_VERSION);
            return VersionComparator.getInstance().compare(latestCompatibleVersion, version) > 0 ? latestCompatibleVersion : null;
        } catch(Throwable t) {
            return null;
        }
    }

    boolean supportsCurrentVersion() {
        for(int i = 0; i < supportedVersions.length(); i++)
            if(supportedVersions.getString(i).equals(Constants.ICT_VERSION))
                return true;
        return false;
    }
}