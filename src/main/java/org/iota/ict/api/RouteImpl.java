package org.iota.ict.api;

import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.IOException;

public abstract class RouteImpl implements Route {

    protected JsonIct jsonIct;

    protected RouteImpl(JsonIct jsonIct) {
        this.jsonIct = jsonIct;
    }

    @Override
    public Object handle(Request request, Response response) {
        try {
            return execute(request);
        } catch (Throwable t) {
            return error(t);
        }
    }

    public abstract Object execute(Request request) throws Throwable;

    protected static JSONObject error(Throwable t) {
        return error(t.getMessage());
    }

    protected static JSONObject error(String message) {
        return new JSONObject().put("error", message);
    }
}

class RouteGetInfo extends RouteImpl {

    protected RouteGetInfo(JsonIct jsonIct) { super(jsonIct); }

    @Override
    public JSONObject execute(Request request) {
        return jsonIct.getInfo();
    }
}

class RouteUpdate extends RouteImpl {

    protected RouteUpdate(JsonIct jsonIct) { super(jsonIct); }

    @Override
    public JSONObject execute(Request request) throws IOException {
        String version = request.queryParams("version");
        return jsonIct.update(version);
    }
}

class RouteGetConfig extends RouteImpl {

    protected RouteGetConfig(JsonIct jsonIct) { super(jsonIct); }

    @Override
    public JSONObject execute(Request request) {
        return jsonIct.getConfig();
    }
}

class RouteSetConfig extends RouteImpl {

    protected RouteSetConfig(JsonIct jsonIct) { super(jsonIct); }

    public JSONObject execute(Request request) {
        String configString = request.queryParams("config");
        return jsonIct.setConfig(configString);
    }
}

class RouteGetNeighbors extends RouteImpl {

    protected RouteGetNeighbors(JsonIct jsonIct) { super(jsonIct); }

    public JSONArray execute(Request request) {
        return jsonIct.getNeighbors();
    }
}

class RouteAddNeighbor extends RouteImpl {

    protected RouteAddNeighbor(JsonIct jsonIct) { super(jsonIct); }

    public JSONObject execute(Request request) {
        String address = request.queryParams("address");
        return jsonIct.addNeighbor(address);
    }
}

class RouteRemoveNeighbor extends RouteImpl {

    protected RouteRemoveNeighbor(JsonIct jsonIct) { super(jsonIct); }

    public JSONObject execute(Request request) {
        String address = request.queryParams("address");
        return jsonIct.removeNeighbor(address);
    }
}

class RouteAddModule extends RouteImpl {

    protected RouteAddModule(JsonIct jsonIct) { super(jsonIct); }

    public JSONObject execute(Request request) throws Throwable {
        String repository = request.queryParams("repository");
        return jsonIct.addModule(repository);
    }
}

class RouteRemoveModule extends RouteImpl {

    protected RouteRemoveModule(JsonIct jsonIct) { super(jsonIct); }

    public JSONObject execute(Request request) {
        String path = request.queryParams("path");
        return jsonIct.removeModule(path);
    }
}

class RouteUpdateModule extends RouteImpl {

    protected RouteUpdateModule(JsonIct jsonIct) { super(jsonIct); }

    public JSONObject execute(Request request) {
        String path = request.queryParams("path");
        String version = request.queryParams("version");
        return jsonIct.updateModule(path, version);
    }
}

class RouteGetModules extends RouteImpl {

    protected RouteGetModules(JsonIct jsonIct) { super(jsonIct); }

    public JSONArray execute(Request request) {
        return jsonIct.getModules();
    }
}

class RouteGetLog extends RouteImpl {

    protected RouteGetLog(JsonIct jsonIct) { super(jsonIct); }

    public JSONArray execute(Request request) {
        throw new IllegalStateException("Feature not implemented yet.");
    }
}