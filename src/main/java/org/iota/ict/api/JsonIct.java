package org.iota.ict.api;

import org.apache.logging.log4j.Logger;
import org.iota.ict.Ict;
import org.iota.ict.IctInterface;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.ixi.IxiModuleHolder;
import org.iota.ict.ixi.IxiModuleInfo;
import org.iota.ict.network.Neighbor;
import org.iota.ict.network.Node;
import org.iota.ict.utils.*;
import org.iota.ict.utils.properties.EditableProperties;
import org.iota.ict.utils.properties.FinalProperties;
import org.iota.ict.utils.properties.Properties;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonIct {

    protected int statsRound = -1;
    protected Map<Neighbor, JSONArray> statsByNeighbor = new HashMap<>();

    protected static final Logger LOGGER = Ict.LOGGER;
    protected final IctInterface ict;
    protected String nmoduleBeingCurrentlyInstalled = null;

    public JsonIct(IctInterface ict) {
        this.ict = ict;
    }

    public JSONObject getInfo() {
        JSONObject info = new JSONObject();
        String availableUpdate;  if((availableUpdate = Updater.getAvailableUpdate()) != null) info.put("update", availableUpdate);
        info.put("version", Constants.ICT_VERSION);
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
        if (newConfig.guiPassword().length() == 0)
            newConfig.guiPassword(currentConfig.guiPassword());
        ict.updateProperties(newConfig.toFinal());
        newConfig.store(Constants.DEFAULT_PROPERTY_FILE_PATH);
        return success();
    }

    public JSONObject getConfig() {
        return ict.getProperties().toJSON().put("gui_password", "");
    }

    public JSONArray getNeighbors() {
        JSONArray nbs = new JSONArray();
        for (Neighbor neighbor : ict.getNeighbors()) {
            JSONObject nb = new JSONObject();

            List<Node.Round> rounds = ict.getRounds();
            if(!statsByNeighbor.containsKey(neighbor))
                statsByNeighbor.put(neighbor, new JSONArray());
            JSONArray stats = statsByNeighbor.get(neighbor);

            if(rounds.size() == 0) {

            }
            updateStatsForNeighbor(neighbor, stats, rounds);
            nb.put("address", neighbor.getAddress());
            nb.put("stats", stats);
            nbs.put(nb);
        }
        return nbs;
    }

    private void updateStatsForNeighbor(Neighbor neighbor, JSONArray stats, List<Node.Round> rounds) {
        // remove all rounds from json which are no longer stored
        if(stats.length() > 0 && rounds.size() > 0)
        while (stats.getJSONObject(0).getLong("timestamp") < rounds.get(0).timestamp)
            stats.remove(0);

        // add all rounds to json which are new
        int firstNewRoundIndex;
        if(stats.length() > 0) {
            long lastSyncedTimestamp = stats.getJSONObject(stats.length()-1).getLong("timestamp");
            for(firstNewRoundIndex = rounds.size()-1; rounds.get(firstNewRoundIndex).timestamp > lastSyncedTimestamp && firstNewRoundIndex > 0; firstNewRoundIndex--);
        } else {
            firstNewRoundIndex = 0;
        }
        for(int i = firstNewRoundIndex; i < rounds.size(); i++) {
            JSONObject json = rounds.get(i).toJSON(neighbor);
            if(json != null)
                stats.put(json);
        }
    }

    public JSONObject addNeighbor(String address) {
        if (!address.matches("^[a-zA-Z0-9\\-.]+:[0-9]{1,5}$"))
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
        for (InetSocketAddress nb : neighbors) {
            if (nb.toString().equals(address)) {
                neighbors.remove(nb);
                properties.neighbors(neighbors);
                ict.updateProperties(properties.toFinal());
                LOGGER.info("removed neighbor: " + address);
                properties.store(Constants.DEFAULT_PROPERTY_FILE_PATH);
                return success();
            }
        }
        throw new IllegalArgumentException("No neighbor with address '" + address + "'.");
    }

    protected JSONArray getModules() {
        JSONArray modules = new JSONArray();
        IxiModuleHolder holder = ict.getModuleHolder();
        for (IxiModule module : holder.getModules()) {
            IxiModuleInfo info = holder.getInfo(module);
            String update = info.getUpdate();
            JSONObject moduleJSON = info.toJSON();
            if(update != null) moduleJSON.put("update", update);
            moduleJSON.put("configurable", module.getContext().getConfiguration() != null);
            modules.put(moduleJSON);
        }
        return modules;
    }

    protected JSONObject getModuleConfig(String path) {
        IxiModule module = ict.getModuleHolder().getModule(path);
        if(module == null)
            throw new RuntimeException("No module '"+path+"'.");

        return new JSONObject()
                .put("default_config", module.getContext().getDefaultConfiguration())
                .put("config", module.getContext().getConfiguration());
    }

    protected JSONObject setModuleConfig(String path, JSONObject config) {
        IxiModule module = ict.getModuleHolder().getModule(path);
        if(module == null)
            throw new RuntimeException("No module '"+path+"'.");

        module.getContext().tryToUpdateConfiguration(config);
        ict.getModuleHolder().storeModuleConfiguration(path);

        return success();
    }

    protected JSONObject addModule(String repository) {
        if (nmoduleBeingCurrentlyInstalled != null)
            throw new RuntimeException("Please wait for the installation of '" + nmoduleBeingCurrentlyInstalled + "' to complete.");

        try {
            nmoduleBeingCurrentlyInstalled = repository;
            String label = findRecommendedOrLatestLabel(repository);
            URL url = GithubGateway.getAssetDownloadUrl(repository, label);
            IxiModule module = ict.getModuleHolder().install(url);
            module.start();
            return success();
        } catch (Throwable t) {
            nmoduleBeingCurrentlyInstalled = null;
            throw new RuntimeException("Installation of module '" + repository + "' failed: " + t, t);
        } finally {
            nmoduleBeingCurrentlyInstalled = null;
        }

    }

    private static String findRecommendedOrLatestLabel(String repository) {
        try {
            String versionsString = GithubGateway.getContents(repository, "master", "versions.json");
            JSONObject versions = new JSONObject(versionsString);
            if(!versions.has(Constants.ICT_VERSION))
                LOGGER.warn("versions.json of repository '"+repository+"' does not specify recommended module version for Ict version " + Constants.ICT_VERSION);
            String label = versions.getString(Constants.ICT_VERSION);
            if (label != null)
                return label;
        } catch (Throwable t) {
        }
        return GithubGateway.getLatestReleaseLabel(repository);
    }

    protected JSONObject removeModule(String path) {
        ict.getModuleHolder().uninstall(path);
        return success();
    }

    protected JSONObject updateModule(String path, String version) throws Throwable {
        ict.getModuleHolder().update(path, version);
        return success();
    }

    protected static JSONObject success() {
        return new JSONObject();
    }
}
