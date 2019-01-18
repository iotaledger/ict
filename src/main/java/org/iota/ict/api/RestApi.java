package org.iota.ict.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.Ict;
import org.iota.ict.IctInterface;
import org.iota.ict.utils.*;
import spark.*;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class RestApi extends RestartableThread implements PropertiesUser {

    protected static final Logger LOGGER = LogManager.getLogger(RestApi.class);
    protected Service service;
    protected final JsonIct jsonIct;
    protected Properties properties;
    protected Set<RouteImpl> routes = new HashSet<>();

    public RestApi(IctInterface ict) {
        super(LOGGER);

        this.properties = ict.getCopyOfProperties();
        this.jsonIct = new JsonIct(ict);

        try {
            if(!new File(Constants.WEB_GUI_PATH).exists())
                extractWebDirectory();
        } catch (IOException e) {
            LOGGER.error("Failed to extract Web GUI into " + new File(Constants.WEB_GUI_PATH).getAbsolutePath(), e);
            throw new RuntimeException(e);
        }

        initRoutes();
    }

    @Override
    public void run() { ; }

    private void initRoutes() {
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

    @Override
    public void onStart() {
        if(!properties.guiEnabled)
            return;

        service = Service.ignite();
        int port = properties.port;
        service.port(port);

        service.staticFiles.externalLocation(Constants.WEB_GUI_PATH);
        for(RouteImpl route : routes)
            service.post(route.getPath(), route);

        service.before(new Filter() {
            @Override
            public void handle(Request request, Response response) {
                String queryPassword = request.queryParams("password");
                if (!queryPassword.equals(properties.guiPassword))
                    spark.Spark.halt(401, "Access denied: password incorrect.");
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
        LOGGER.info("Started Web GUI on port " + port + ".");
    }

    @Override
    public void onTerminate() {
        if(service == null) // wasn't running
            return;
        for(RouteImpl route : routes)
            service.delete(route.getPath(), route);
        service.stop();
        service = null;
        LOGGER.info("Stopped Web GUI.");
    }

    @Override
    public void updateProperties(Properties properties) {
        Properties oldProp = this.properties;
        Properties newProp = properties.clone();
        this.properties = newProp;

        if(oldProp.guiEnabled && !newProp.guiEnabled)
            terminate();
        else if(!oldProp.guiEnabled && newProp.guiEnabled)
            start();
        else if(oldProp.guiEnabled && newProp.guiEnabled && oldProp.port != newProp.port) {
            terminate();
            start();
        }
    }

    /**
     * CREDITS: https://stackoverflow.com/questions/1529611/how-to-write-a-java-program-which-can-extract-a-jar-file-and-store-its-data-in-s/1529707#1529707
     * */
    private void extractWebDirectory() throws IOException {
        File jarFile = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
        java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile);
        java.util.Enumeration enumEntries = jar.entries();
        LOGGER.info("extracting web gui ...");
        while (enumEntries.hasMoreElements()) {
            java.util.jar.JarEntry file = (java.util.jar.JarEntry) enumEntries.nextElement();
            if(!file.getName().startsWith("web/"))
                continue;
            LOGGER.info("extracting file: " + file.getName() + " ...");
            java.io.File f = new java.io.File(Constants.WEB_GUI_PATH + java.io.File.separator + file.getName().replaceAll("^web/", ""));
            if (file.isDirectory()) {
                f.mkdirs();
                continue;
            }
            f.createNewFile();
            java.io.InputStream is = jar.getInputStream(file); // get the input stream
            java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
            while (is.available() > 0) {
                fos.write(is.read());
            }
            fos.close();
            is.close();
        }
        jar.close();
    }
}
