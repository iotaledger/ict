package org.iota.ict.api;

import org.iota.ict.Ict;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.ixi.IxiModuleHolder;
import org.iota.ict.ixi.IxiModuleInfo;
import org.iota.ict.network.Neighbor;
import org.iota.ict.utils.Properties;
import org.iota.ict.utils.Updater;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;

public class JsonIct {

    protected final Ict ict;

    public JsonIct(Ict ict) {
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
        ict.changeProperties(Properties.fromJSON(configJson));
        return success();
    }

    public JSONObject getConfig() {
        return ict.getProperties().toJSON();
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
        if(!address.matches("[a-zA-Z0-9\\-.]+:[0-9]{1,5}$"))
            throw new IllegalArgumentException("Address does not match required format 'host:port'.");
        String host = address.split(":")[0];
        int port = Integer.parseInt(address.split(":")[1]);
        System.out.println("added neighbor: " + address);
        ict.neighbor(new InetSocketAddress(host, port));
        return success();
    }

    public JSONObject removeNeighbor(String address) {
        for(Neighbor nb : ict.getNeighbors()) {
            String nbAddress = nb.getAddress().getHostName() + ":" + nb.getAddress().getPort();
            if(nbAddress.equals(address)) {
                ict.unneighbor(nb);
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
        URL url = GithubGateway.getAssetDownloadUrl(repository, GithubGateway.getLatestReleaseLabel(repository));
        ict.getModuleHolder().install(url);
        return success();
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
