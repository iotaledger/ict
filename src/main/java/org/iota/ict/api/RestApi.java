package org.iota.ict.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.Ict;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.ixi.IxiModuleInfo;
import org.iota.ict.network.Neighbor;
import org.iota.ict.utils.Constants;
import org.iota.ict.ixi.IxiModuleHolder;
import org.iota.ict.utils.Properties;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.*;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RestApi {

    protected static final Logger LOGGER = LogManager.getLogger(RestApi.class);

    protected Ict ict;
    protected boolean isRunning = false;
    protected Map<String, Route> routes = new HashMap<>();
    protected Service service;

    public RestApi(Ict ict) {
        this.ict = ict;
    }

    public void start(int port) {
        if(service != null)
            terminate();

        isRunning = true;
        service = Service.ignite();
        service.port(port);

        service.staticFiles.externalLocation("web/");

        post("/setConfig", new Route() {
            @Override
            public Object handle(Request request, Response response) {
                try {
                    String cfgStr = request.queryParams("config");
                    JSONObject cfgJSON = new JSONObject(cfgStr);
                    Properties properties = Properties.fromJSON(cfgJSON);
                    properties.neighbors = ict.getProperties().neighbors;
                    ict.changeProperties(properties);
                    return "{}";
                } catch (Throwable t) {
                    t.printStackTrace();
                    return error(t.getMessage());
                }
            }
        });

        post("/getConfig", new Route() {
            @Override
            public Object handle(Request request, Response response) {
                return ict.getProperties().toJSON().toString();
            }
        });

        post("/getModules", new Route() {
            @Override
            public Object handle(Request request, Response response) {
                JSONArray ixis = new JSONArray();
                IxiModuleHolder holder = ict.getModuleHolder();
                for(IxiModule module : holder.getModules()) {
                    IxiModuleInfo info = holder.getInfo(module);
                    ixis.put(info.toJSON());
                }
                return ixis.toString();
            }
        });

        post("/getNeighborStats", new Route() {
            @Override
            public Object handle(Request request, Response response) {
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
                return nbs.toString();
            }
        });

        post("/addNeighbor", new Route() {
            @Override
            public Object handle(Request request, Response response) {
                if(ict.getNeighbors().size() >= Constants.MAX_NEIGHBOR_COUNT)
                    return error("Already reached maximum amount of neighbors ("+Constants.MAX_NEIGHBOR_COUNT+").");
                String address = request.queryParams("address");
                if(!address.contains(":"))
                    address += ":1337";
                try {
                    String host = address.split(":")[0];
                    int port = Integer.parseInt(address.split(":")[1]);
                    System.out.println("added neighbor: " + address);
                    ict.neighbor(new InetSocketAddress(host, port));
                    return "{}";
                } catch (Throwable t) {
                    return error("Failed adding '"+address+"': " + t.getMessage());
                }
            }
        });

        post("/removeNeighbor", new Route() {
            @Override
            public Object handle(Request request, Response response) {
                String address = request.queryParams("address");
                for(Neighbor nb : ict.getNeighbors()) {
                    String nbAddress = nb.getAddress().getHostName() + ":" + nb.getAddress().getPort();
                    if(nbAddress.equals(address)) {
                        ict.unneighbor(nb);
                        return "{}";
                    }
                }
                return error("No neighbor with address '"+address+"'.");
            }
        });

        post("/addModule", new Route() {
            @Override
            public Object handle(Request request, Response response) {
                String user_slash_repo = request.queryParams("user_slash_repo");
                try {
                    URL url = GithubGateway.getDownloadURLOfMostRecentAsset(user_slash_repo);
                    ict.getModuleHolder().install(url);
                    return "{}";
                } catch (Throwable t) {
                    return error(t.getMessage());
                }
            }
        });

        post("/removeModule", new Route() {
            @Override
            public Object handle(Request request, Response response) {
                String path = request.queryParams("path");
                boolean success = ict.getModuleHolder().uninstall(path);
                return success ? "{}" : error("failed uninstalling module '"+path+"'");
            }
        });

        service.after(new Filter() {
            @Override
            public void handle(Request request, Response response) {
                response.header("Access-Control-Allow-Origin", "*");
                response.header("Access-Control-Allow-Methods", "GET");
            }
        });

        LOGGER.info("Started Web GUI on port " + port + ".");
    }

    private void post(String path, Route route) {
        routes.put(path, route);
        service.post(path, route);
    }

    public void terminate() {
        Set<String> paths = new HashSet<>(routes.keySet());
        for(String path : paths) {
            service.delete(path, routes.get(path));
            routes.remove(path);
        }
        service.stop();
        isRunning = false;
        service = null;
        LOGGER.info("Stopped Web GUI.");
    }

    public boolean isRunning() {
        return isRunning;
    }

    private String error(String message) {
        return new JSONObject().put("error", message).toString();
    }
}
