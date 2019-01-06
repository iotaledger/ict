package org.iota.ict.ixi;

import org.iota.ict.utils.Constants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class IxiModuleInfo {

    public final String repository;
    public final String name;
    public final String mainClass;
    public final String description;
    public boolean webGui ;
    private final String path;
    public final JSONArray supportedVersions;

    public IxiModuleInfo(JSONObject json, String path) throws JSONException {
        supportedVersions = json.getJSONArray("supported_versions");
        if(!supportsCurrentVersion())
            throw new RuntimeException("IXI module does not specify version '"+Constants.ICT_VERSION+"' as supported in module.json.");
        name = json.getString("name");
        description = json.getString("description");
        repository = json.getString("repository");
        mainClass = json.getString("main_class");
        if(!mainClass.startsWith("org.iota.ict.ixi."))
            throw new RuntimeException("Invalid module.json: Main class must be in package 'org.iota.ict.ixi'");
        webGui = json.getBoolean("web_gui");
        this.path = path;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("description", description);
        json.put("repository", repository);
        json.put("path", path);
        json.put("web_gui", webGui);
        json.put("main_class", mainClass);
        json.put("supported_versions", supportedVersions);
        return json;
    }

    private boolean supportsCurrentVersion() {
        for(int i = 0; i < supportedVersions.length(); i++)
            if(supportedVersions.getString(i).equals(Constants.ICT_VERSION))
                return true;
        return false;
    }
}