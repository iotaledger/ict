package org.iota.ict.ixi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.Ict;
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
    public String update = null;
    public int guiPort;
    private final String path;
    public final JSONArray supportedVersions;

    private static final Logger LOGGER = LogManager.getLogger("ModuleInfo");

    public IxiModuleInfo(JSONObject json, String path) throws JSONException {
        version = json.getString("version");
        supportedVersions = json.getJSONArray("supported_versions");
        name = json.getString("name");
        description = json.getString("description");
        repository = json.getString("repository");
        if (!repository.matches("^[a-zA-Z0-9\\-_.]+/[a-zA-Z0-9\\-_.]+$"))
            throw new RuntimeException("Illegal repository declared in module.json: '" + repository + "'. Invalid format or unexpected characters.");
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

    public void checkForUpdate() {
        LOGGER.info("checking module '"+name+"' for updates ...");
        try {
            String versionsString = GithubGateway.getContents(repository, "master", "versions.json");
            JSONObject versions = new JSONObject(versionsString);
            String latestCompatibleVersion = versions.has(Constants.ICT_VERSION) ? versions.getString(Constants.ICT_VERSION) : "0";
            update = VersionComparator.getInstance().compare(latestCompatibleVersion, version) > 0 ? latestCompatibleVersion : null;
            LOGGER.info((update == null ? "No update" : "Update to version "+update) + " available for module '" + name + "'.");
        } catch (Throwable t) {
            LOGGER.error("Could not check repository '"+repository+"' for updates.", t);
        }
    }

    boolean supportsCurrentVersion() {
        for (int i = 0; i < supportedVersions.length(); i++)
            if (supportedVersions.getString(i).equals(Constants.ICT_VERSION))
                return true;
        return false;
    }
}