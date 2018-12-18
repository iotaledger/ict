package org.iota.ict;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Properties;

import java.io.File;
import java.net.BindException;
import java.util.HashMap;
import java.util.Map;

public class Main {

    private static final String DEFAULT_PROPERTY_FILE_PATH = "ict.cfg";
    private static final Map<String, String> ARG_NAME_BY_ARG_ALIAS = new HashMap<>();

    private static final Logger logger = LogManager.getLogger();

    public static final String ARG_CONFIG = "-config";

    static {
        ARG_NAME_BY_ARG_ALIAS.put("-c", ARG_CONFIG);
        ARG_NAME_BY_ARG_ALIAS.put(ARG_CONFIG, ARG_CONFIG);
    }

    public static void main(String[] args) {

        Map<String, String> argMap = mapArgs(args);

        Properties properties = loadOrCreatedProperties(argMap);
        logger.info("Starting new Ict '" + properties.name + "' (version: " + Constants.ICT_VERSION + ")");

        if (!properties.ixiEnabled && properties.ixis.size() > 0)
            logger.warn("Not running any IXI modules because IXI is disabled. To enable IXI, set 'ixi_enabled = true' in your config file.");

        Ict ict;
        try {
            ict = new Ict(properties);
        } catch (Throwable t) {
            if (t.getCause() instanceof BindException)
                logger.error("Could not start Ict on " + properties.host + ":" + properties.port + " (" + t.getCause().getMessage() + "). Make sure that the address is correct and you are not already running an Ict instance or any other service on that port. You can change the port in your properties file.");
            else
                logger.error(t);
            return;
        }
        logger.info("Ict started on " + ict.getAddress() + ".\n");

        Ict b = new Ict(new Properties().port(1338));
        ict.neighbor(b.getAddress());
        ict.logRound();

        final Ict finalRefToIct = ict;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("Terminating Ict ...");
                finalRefToIct.terminate();
            }
        });
    }

    private static Properties loadOrCreatedProperties(Map<String, String> argMap) {
        Properties properties = new Properties();
        try {
            properties = tryToLoadOrCreateProperties(argMap);
        } catch (Throwable t) {
            logger.error("Failed loading properties", t);
        }
        return properties;
    }

    private static Properties tryToLoadOrCreateProperties(Map<String, String> argMap) {
        if (argMap.containsKey(ARG_CONFIG)) {
            return Properties.fromFile(argMap.get(ARG_CONFIG));
        } else if (new File(DEFAULT_PROPERTY_FILE_PATH).exists()) {
            return Properties.fromFile(DEFAULT_PROPERTY_FILE_PATH);
        } else {
            logger.warn("No property file found, creating new: '" + DEFAULT_PROPERTY_FILE_PATH + "'.");
            Properties properties = new Properties();
            properties.store(DEFAULT_PROPERTY_FILE_PATH);
            return properties;
        }
    }

    private static final Map<String, String> mapArgs(String[] args) {

        Map<String, String> argMap = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            if (arg.charAt(0) == '-') {
                if (i == args.length - 1 || args[i + 1].charAt(0) == '-') {
                    logger.warn("no value for option " + arg);
                    break;
                }
                if (argMap.containsKey(arg))
                    logger.warn("multiple uses of option " + arg);
                if (!ARG_NAME_BY_ARG_ALIAS.containsKey(arg))
                    logger.warn("unknown option " + arg);
                String value = args[i + 1];
                argMap.put(ARG_NAME_BY_ARG_ALIAS.get(arg), value);
            }
        }

        return argMap;
    }
}
