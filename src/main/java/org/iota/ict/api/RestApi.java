package org.iota.ict.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.Ict;
import spark.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RestApi {

    protected static final String WEB_GUI_PATH = "web/";

    protected static final Logger LOGGER = LogManager.getLogger(RestApi.class);
    protected Service service;
    protected final JsonIct jsonIct;
    protected boolean isRunning = false;
    protected Map<String, Route> routes = new HashMap<>();

    public RestApi(Ict ict) {
        this.jsonIct = new JsonIct(ict);
    }

    public void start(int port) {
        if (service != null)
            terminate();
        isRunning = true;
        service = Service.ignite();
        service.port(port);

        service.staticFiles.externalLocation(WEB_GUI_PATH);
        registerRoutes();

        service.after(new Filter() {
            @Override
            public void handle(Request request, Response response) {
                response.header("Access-Control-Allow-Origin", "*");
                response.header("Access-Control-Allow-Methods", "GET");
            }
        });

        LOGGER.info("Started Web GUI on port " + port + ".");
    }

    private void registerRoutes() {
        post("/getInfo", new RouteGetInfo(jsonIct));
        post("/getLog", new RouteGetLog(jsonIct));
        post("/update", new RouteUpdate(jsonIct));

        post("/setConfig", new RouteSetConfig(jsonIct));
        post("/getConfig", new RouteGetConfig(jsonIct));

        post("/getNeighbors", new RouteGetNeighbors(jsonIct));
        post("/addNeighbor", new RouteAddNeighbor(jsonIct));
        post("/removeNeighbor", new RouteRemoveNeighbor(jsonIct));

        post("/getModules", new RouteGetModules(jsonIct));
        post("/addModule", new RouteAddModule(jsonIct));
        post("/removeModule", new RouteRemoveModule(jsonIct));
        post("/updateModule", new RouteUpdateModule(jsonIct));
    }

    private void post(String path, Route route) {
        routes.put(path, route);
        service.post(path, route);
    }

    public void terminate() {
        deleteAllRoutes();
        service.stop();
        isRunning = false;
        service = null;
        LOGGER.info("Stopped Web GUI.");
    }

    private void deleteAllRoutes() {
        Set<String> paths = new HashSet<>(routes.keySet());
        for(String path : paths) {
            service.delete(path, routes.get(path));
            routes.remove(path);
        }
    }

    public boolean isRunning() {
        return isRunning;
    }
}
