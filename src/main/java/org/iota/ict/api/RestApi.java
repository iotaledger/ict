package org.iota.ict.api;

import com.iota.curl.IotaCurlHash;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.IctInterface;
import org.iota.ict.utils.*;
import org.iota.ict.utils.properties.FinalProperties;
import org.iota.ict.utils.properties.Properties;
import org.iota.ict.utils.properties.PropertiesUser;
import spark.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RestApi extends RestartableThread implements PropertiesUser {

    protected static final Logger LOGGER = LogManager.getLogger("RestAPI");
    protected static Map<String, String> passwordHashes = new HashMap<>();
    protected Service service;
    protected final JsonIct jsonIct;
    protected FinalProperties properties;
    protected Set<RouteImpl> routes = new HashSet<>();
    protected boolean initialized = false;

    private Map<String, List<Long>> timestampsOfFailedAuthenticationsByIP = new HashMap<>();

    static {
        try {
            if (Constants.RUN_MODUS == Constants.RunModus.MAIN && !new File(Constants.WEB_GUI_PATH).exists() && startedFromJar())
                IOHelper.extractDirectoryFromJarFile(RestApi.class, "web/", Constants.WEB_GUI_PATH);
        } catch (IOException e) {
            LOGGER.error("Failed to extract Web GUI into " + new File(Constants.WEB_GUI_PATH).getAbsolutePath(), e);
            throw new RuntimeException(e);
        }
    }

    private static boolean startedFromJar() {
        String path = RestApi.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        return path.endsWith(".jar");
    }

    public RestApi(IctInterface ict) {
        super(LOGGER);
        this.properties = ict.getProperties();
        this.jsonIct = new JsonIct(ict);
    }

    @Override
    public void run() {
        ;
    }

    private void initRoutes() {
        routes.add(new RouteGetInfo(jsonIct));
        routes.add(new RouteGetLogs(jsonIct));
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

        routes.add(new RouteGetModuleConfig(jsonIct));
        routes.add(new RouteSetModuleConfig(jsonIct));
        routes.add(new RouteGetModuleResponse(jsonIct));

        initialized = true;
    }

    @Override
    public void onStart() {
        if (!properties.guiEnabled())
            return;

        if (!initialized)
            initRoutes();

        service = Service.ignite();
        int guiPort = properties.guiPort();
        service.port(guiPort);

        service.staticFiles.externalLocation(Constants.WEB_GUI_PATH);
        for (RouteImpl route : routes)
            service.post(route.getPath(), route);

        service.before(new Filter() {
            @Override
            public void handle(Request request, Response response) {

                List<Long> timestampsOfFailedAuthentifications = getTimestampsOfFailedAuthenticationsFor(request.ip());
                long timeout = getTimeoutForFailedAuthentications(timestampsOfFailedAuthentifications);
                if(timeout > 0)
                    service.halt(429, "Too many authentication failed: Try again in "+(timeout/1000)+" seconds.");

                if(request.requestMethod().equals("GET")) {
                    if(!request.pathInfo().matches("^[/]?$") && !request.pathInfo().startsWith("/modules/"))
                        response.redirect("/");
                } else {
                    String queryPassword = hashPassword(request.queryParams("password"));
                    if (!queryPassword.equals(properties.guiPassword())) {
                        timestampsOfFailedAuthentifications.add(System.currentTimeMillis());
                        service.halt(401, "Access denied: password incorrect.");
                    }
                }
            }
        });

        service.after(new Filter() {
            @Override
            public void handle(Request request, Response response) {
                response.header("Access-Control-Allow-Origin", "*");
                response.header("Access-Control-Allow-Methods", "GET");
            }
        });

        service.init();
        service.awaitInitialization();
        LOGGER.info("Started Web GUI on port " + guiPort + ". Access it by visiting '{HOST}:" + guiPort + "' from your web browser.");
    }

    private long getTimeoutForFailedAuthentications(List<Long> timestampsOfFailedAuthentications) {
        if(countFailedAuthentications(timestampsOfFailedAuthentications, 3600000) >= 50)
            return timestampsOfFailedAuthentications.get(timestampsOfFailedAuthentications.size()-50)+3600000-System.currentTimeMillis();
        if(countFailedAuthentications(timestampsOfFailedAuthentications, 600000) >= 30)
            return timestampsOfFailedAuthentications.get(timestampsOfFailedAuthentications.size()-30)+600000-System.currentTimeMillis();
        if(countFailedAuthentications(timestampsOfFailedAuthentications, 180000) >= 20)
            return timestampsOfFailedAuthentications.get(timestampsOfFailedAuthentications.size()-20)+180000-System.currentTimeMillis();
        if(countFailedAuthentications(timestampsOfFailedAuthentications, 30000) >= 10)
            return timestampsOfFailedAuthentications.get(timestampsOfFailedAuthentications.size()-10)+30000-System.currentTimeMillis();
        if(countFailedAuthentications(timestampsOfFailedAuthentications, 5000) >= 5)
            return timestampsOfFailedAuthentications.get(timestampsOfFailedAuthentications.size()-5)+5000-System.currentTimeMillis();
        return 0;
    }

    private List<Long> getTimestampsOfFailedAuthenticationsFor(String ip) {
        List<Long> timestampsOfFailedAuthentications = timestampsOfFailedAuthenticationsByIP.get(ip);
        if(timestampsOfFailedAuthentications == null) {
            timestampsOfFailedAuthentications = new LinkedList<>();
            timestampsOfFailedAuthenticationsByIP.put(ip, timestampsOfFailedAuthentications);
        }
        while (timestampsOfFailedAuthentications.size() > 0 && timestampsOfFailedAuthentications.get(0) < System.currentTimeMillis() - 3600000)
            timestampsOfFailedAuthentications.remove(0);
        return timestampsOfFailedAuthentications;
    }

    private int countFailedAuthentications(List<Long> timestampsOfFailedAuthentifications, long intervalInMillis) {
        long minTimestamp = System.currentTimeMillis() - intervalInMillis;
        int i;
        for(i = timestampsOfFailedAuthentifications.size()-1; i >= 0 && timestampsOfFailedAuthentifications.get(i) > minTimestamp; i--) { }
        return timestampsOfFailedAuthentifications.size()-i-1;
    }

    @Override
    public void onTerminate() {
        if (service == null) // wasn't running
            return;
        for (RouteImpl route : routes)
            service.delete(route.getPath(), route);
        service.stop();
        service = null;
    }

    @Override
    public void updateProperties(FinalProperties newProp) {
        Properties oldProp = this.properties;
        this.properties = newProp;

        if (oldProp.guiEnabled() && !newProp.guiEnabled())
            terminate();
        else if (!oldProp.guiEnabled() && newProp.guiEnabled())
            start();
        else if (oldProp.guiEnabled() && newProp.guiEnabled() && oldProp.guiPort() != newProp.guiPort()) {
            terminate();
            start();
        }
    }

    public static String hashPassword(String plain) {
        if(plain == null)
            return "";
        if(!passwordHashes.containsKey(plain)) {
            String trytes = Trytes.fromAscii(plain);
            String hash = IotaCurlHash.iotaCurlHash(trytes, trytes.length(), 27);
            passwordHashes.put(plain, hash);
        }
        return passwordHashes.get(plain);
    }
}
