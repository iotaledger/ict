package org.iota.ict.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.Ict;
import org.iota.ict.utils.Constants;
import spark.*;

import java.util.HashSet;
import java.util.Set;

public class RestApi {

    protected static final Logger LOGGER = LogManager.getLogger(RestApi.class);
    protected Service service;
    protected final JsonIct jsonIct;
    protected boolean isRunning = false;
    protected Set<RouteImpl> routes = new HashSet<>();

    public RestApi(Ict ict) {
        this.jsonIct = new JsonIct(ict);

        routes.add(new RouteGetInfo(jsonIct));
        routes.add(new RouteGetLog(jsonIct));
        routes.add(new RouteUpdate(jsonIct));

        routes.add(new RouteSetConfig(jsonIct));
        routes.add(new RouteGetConfig(jsonIct));

        routes.add(new RouteGetNeighbors(jsonIct));
        routes.add(new RouteAddNeighbor(jsonIct));
        routes.add(new RouteRemoveNeighbor(jsonIct));

        routes.add(new RouteGetModules(jsonIct));
        routes.add(new RouteAddModule(jsonIct));
        routes.add(new RouteRemoveModule(jsonIct));
        routes.add(new RouteUpdateModule(jsonIct));
    }

    public void start(int port) {
        if (service != null)
            terminate();
        isRunning = true;
        service = Service.ignite();
        service.port(port);

        service.staticFiles.externalLocation(Constants.WEB_GUI_PATH);
        for(RouteImpl route : routes)
            service.post(route.getPath(), route);

        service.after(new Filter() {
            @Override
            public void handle(Request request, Response response) {
                response.header("Access-Control-Allow-Origin", "*");
                response.header("Access-Control-Allow-Methods", "GET");
            }
        });

        service.init();
        service.awaitInitialization();
        LOGGER.info("Started Web GUI on port " + port + ".");
    }

    public void terminate() {
        for(RouteImpl route : routes)
            service.delete(route.getPath(), route);
        service.stop();
        isRunning = false;
        service = null;
        LOGGER.info("Stopped Web GUI.");
    }

    public boolean isRunning() {
        return isRunning;
    }
}
