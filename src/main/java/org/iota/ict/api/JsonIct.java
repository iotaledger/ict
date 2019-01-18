package org.iota.ict.api;

import org.apache.logging.log4j.Logger;
import org.iota.ict.Ict;
import org.iota.ict.IctInterface;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.ixi.IxiModuleHolder;
import org.iota.ict.ixi.IxiModuleInfo;
import org.iota.ict.network.Neighbor;
import org.iota.ict.utils.*;
import org.iota.ict.utils.properties.EditableProperties;
import org.iota.ict.utils.properties.FinalProperties;
import org.iota.ict.utils.properties.Properties;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;

public class JsonIct {

    protected static final Logger LOGGER = Ict.LOGGER;
    protected final IctInterface ict;
    protected String nmoduleBeingCurrentlyInstalled = null;

    public JsonIct(IctInterface ict) {
        this.ict = ict;
    }

    public JSONObject getInfo() {
        JSONObject info = new JSONObject();
        info.put("update", Updater.getLabelOfAvailableUpdate());
        info.put("default_config", (new Properties()).toJSON());
        return info;
    }

    public JSONObject update(String version) throws IOException {
        Updater.update(version);
        return success();
    }

    public JSONObject setConfig(String configString) {
        JSONObject configJson = new JSONObject(configString);
        EditableProperties newConfig = Properties.fromJSON(configJson).toEditable();
        FinalProperties currentConfig = ict.getProperties();
        newConfig.neighbors(currentConfig.neighbors());
        if(newConfig.guiPassword().length() == 0)
            newConfig.guiPassword(currentConfig.guiPassword());
        ict.updateProperties(newConfig.toFinal());
        newConfig.store(Constants.DEFAULT_PROPERTY_FILE_PATH);
        return success();
    }

    public JSONObject getConfig() {
        return ict.getProperties().toJSON().put("gui_password","");
    }

    public JSONArray getNeighbors() {
        JSONArray nbs = new JSONArray();
        for(Neighbor neighbor : ict.getNeighbors()) {
            JSONObject nb = new JSONObject();
            nb.put("address", neighbor.getAddress());
            nb.put("all", neighbor.stats.prevReceivedAll);
            nb.put("new", neighbor.stats.prevReceivedNew);
            nb.put("requested", neighbor.stats.prevRequested);
            nb.put("invalid", neighbor.stats.prevReceivedInvalid);
            nb.put("ignored", neighbor.stats.prevIgnored);
            nbs.put(nb);
        }
        return nbs;
    }

    public JSONObject addNeighbor(String address) {
        if(!address.matches("^[a-zA-Z0-9\\-.]+:[0-9]{1,5}$"))
            throw new IllegalArgumentException("Address does not match required format 'host:port'.");
        String host = address.split(":")[0];
        int port = Integer.parseInt(address.split(":")[1]);
        EditableProperties properties = ict.getProperties().toEditable();

        List<InetSocketAddress> neighbors = properties.neighbors();
        neighbors.add(new InetSocketAddress(host, port));
        properties.neighbors(neighbors);
        ict.updateProperties(properties.toFinal());

        LOGGER.info("added neighbor: " + address);
        properties.store(Constants.DEFAULT_PROPERTY_FILE_PATH);
        return success();
    }

    public JSONObject removeNeighbor(String address) {
        EditableProperties properties = ict.getProperties().toEditable();
        List<InetSocketAddress> neighbors = properties.neighbors();
        for(InetSocketAddress nb : neighbors) {
            if(nb.toString().equals(address)) {
                neighbors.remove(nb);
                properties.neighbors(neighbors);
                ict.updateProperties(properties.toFinal());
                LOGGER.info("removed neighbor: " + address);
                properties.store(Constants.DEFAULT_PROPERTY_FILE_PATH);
                return success();
            }
        }
        throw new IllegalArgumentException("No neighbor with address '"+address+"'.");
    }

    protected JSONArray getModules() {
        JSONArray ixis = new JSONArray();
        IxiModuleHolder holder = ict.getModuleHolder();
        for(IxiModule module : holder.getModules()) {
            IxiModuleInfo info = holder.getInfo(module);
            ixis.put(info.toJSON());
        }
        return ixis;
    }

    protected JSONObject addModule(String repository) throws Throwable {
        if(nmoduleBeingCurrentlyInstalled != null)
            throw new RuntimeException("Please wait for the installation of '"+nmoduleBeingCurrentlyInstalled+"' to complete.");

        try {
            nmoduleBeingCurrentlyInstalled = repository;
            String label = findRecommendedOrLatestLabel(repository);
            URL url = GithubGateway.getAssetDownloadUrl(repository, label);
            ict.getModuleHolder().install(url);
            return success();
        } catch (Throwable t) {
            nmoduleBeingCurrentlyInstalled = null;
            throw new RuntimeException("Installation of module '"+repository+"' failed: " + t.getMessage(), t);
        } finally {
            nmoduleBeingCurrentlyInstalled = null;
        }

    }

    private static String findRecommendedOrLatestLabel(String repository) {
        try {
            String versionsString = GithubGateway.getContents(repository, "master", "versions.json");
            JSONObject versions = new JSONObject(versionsString);
            String label = versions.getString(Constants.ICT_VERSION);
            if(label != null)
                return label;
        } catch (Throwable t) {  }
        return GithubGateway.getLatestReleaseLabel(repository);
    }

    protected JSONObject removeModule(String path) {
        ict.getModuleHolder().uninstall(path);
        return success();
    }

    protected JSONObject updateModule(String path, String version) {
        throw new IllegalStateException("Feature not implemented yet.");
    }

    protected static JSONObject success() {
        return new JSONObject();
    }
}
