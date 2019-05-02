package org.iota.ict.api;

import com.iota.curl.IotaCurlHash;
import org.apache.logging.log4j.Logger;
import org.iota.ict.Ict;
import org.iota.ict.IctInterface;
import org.iota.ict.Main;
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
import java.net.URL;
import java.util.List;
import java.util.Set;

public class JsonIct {

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
        newConfig.guiPassword(newConfig.guiPassword().length() == 0 ? currentConfig.guiPassword() : RestApi.hashPassword(newConfig.guiPassword()));
        ict.updateProperties(newConfig.toFinal());
        newConfig.store(Main.getConfigFilePath());
        return success();
    }

    public JSONObject getConfig() {
        return ict.getProperties().toEditable().guiPassword("").toJSON();
    }

    public JSONArray getNeighbors(long timestampMin, long timestampMax) {
        JSONArray nbs = new JSONArray();
        for (Neighbor neighbor : ict.getNeighbors()) {
            JSONObject nbJSON = new JSONObject();
            nbJSON.put("address", neighbor.getAddress());
            nbJSON.put("stats", neighborStatsToScaledJSON(neighbor, timestampMin, timestampMax));
            nbs.put(nbJSON);
        }
        return nbs;
    }

    protected static JSONArray neighborStatsToScaledJSON(Neighbor neighbor, long timestampMin, long timestampMax) {

        List<Stats> statsHistory = neighbor.getStatsHistory();
        statsHistory = subListByTimestampInterval(statsHistory, timestampMin, timestampMax);

        Stats[] statsHistoryScaled = statsHistory.size() <= Constants.API_MAX_STATS_PER_NEIGHBOR
                ? neighborStatsNoScalingRequired(statsHistory)
                : scaleNeighborStats(statsHistory, neighbor);

        JSONArray neighborStats = new JSONArray();
        for(Stats stats : statsHistoryScaled)
            neighborStats.put(stats.toJSON());
        return neighborStats;
    }

    protected static List<Stats> subListByTimestampInterval(List<Stats> statsAscendingTimestamp, long timestampMin, long timestampMax) {
        int indexMin, indexMax;
        for(indexMin = 0; indexMin < statsAscendingTimestamp.size() && statsAscendingTimestamp.get(indexMin).timestamp < timestampMin; indexMin++);
        for(indexMax = indexMin; indexMax < statsAscendingTimestamp.size()-1 && statsAscendingTimestamp.get(indexMax+1).timestamp < timestampMax; indexMax++);
        return statsAscendingTimestamp.subList(indexMin, indexMax);
    }

    protected static Stats[] neighborStatsNoScalingRequired(List<Stats> statsHistory) {
        Stats[] statsHistoryScaled;
        statsHistoryScaled = new Stats[statsHistory.size()];
        for(int i = 0; i < statsHistory.size(); i++)
            statsHistoryScaled[i] = statsHistory.get(i);
        return statsHistoryScaled;
    }

    protected static Stats[] scaleNeighborStats(List<Stats> statsHistory, Neighbor neighbor) {

        long timestampMin = statsHistory.get(0).timestamp;
        long timestampMax = statsHistory.get(statsHistory.size()-1).timestamp+1;

        Stats[] statsHistoryScaled;
        statsHistoryScaled = new Stats[Constants.API_MAX_STATS_PER_NEIGHBOR];
        for(int i = 0; i < statsHistoryScaled.length; i++) {
            statsHistoryScaled[i] = new Stats(neighbor);
            statsHistoryScaled[i].timestamp = timestampMin + (timestampMax - timestampMin) / Constants.API_MAX_STATS_PER_NEIGHBOR * i;
        }

        for(Stats stats : statsHistory) {
            int index = (int)((Constants.API_MAX_STATS_PER_NEIGHBOR * (stats.timestamp - timestampMin)) / (timestampMax - timestampMin));
            statsHistoryScaled[index].accumulate(stats);
        }
        return statsHistoryScaled;
    }

    public JSONObject addNeighbor(String address) {
        if (!address.matches("^(.*/)?[a-zA-Z0-9\\-.:]+:[0-9]{1,5}$"))
            throw new IllegalArgumentException("Address does not match required format 'host:port'.");
        EditableProperties properties = ict.getProperties().toEditable();

        Set<String> neighbors = properties.neighbors();
        neighbors.add(address);
        if(neighbors.size() > Constants.MAX_NEIGHBOR_COUNT)
            throw new IllegalStateException("Already reached maximum amount of neighbors. Delete others first before adding new.");
        properties.neighbors(neighbors);
        ict.updateProperties(properties.toFinal());

        LOGGER.info("added neighbor: " + address);
        properties.store(Main.getConfigFilePath());
        return success();
    }

    public JSONObject removeNeighbor(String address) {
        EditableProperties properties = ict.getProperties().toEditable();
        Set<String> neighbors = properties.neighbors();
        for (String nb : neighbors) {
            if (nb.equals(address)) {
                neighbors.remove(nb);
                properties.neighbors(neighbors);
                ict.updateProperties(properties.toFinal());
                LOGGER.info("removed neighbor: " + address);
                properties.store(Main.getConfigFilePath());
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

    protected IxiModule getModule(String path) {
        IxiModule module = ict.getModuleHolder().getModule(path);
        if(module == null)
            throw new RuntimeException("No module '"+path+"'.");
        return module;
    }

    protected JSONObject getModuleConfig(String path) {

        IxiModule module = getModule(path);
        return new JSONObject()
                .put("default_config", module.getContext().getDefaultConfiguration())
                .put("config", module.getContext().getConfiguration());
    }

    protected JSONObject getModuleResponse(String path, String request) {
        IxiModule module = getModule(path);
        return new JSONObject().put("response", module.getContext().respondToRequest(request));
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
