package org.iota.ict;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.ErrorHandler;
import org.iota.ict.utils.Properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.BindException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * This class controls what happens when the program is run by a user. It is the entry point when starting this application and helps the user to set up a new
 * Ict node. As such it is a convenience and not technically required to create Ict nodes. A good example are the jUnit tests which work entirely independently
 * from this class.
 */
public class Main {
    private static final String DEFAULT_PROPERTY_FILE_PATH = "ict.cfg";
    public static final String DEFAULT_LOG_DIR_PATH = "logs/";
    private static final File DEFAULT_LOG_DIR = new File(DEFAULT_LOG_DIR_PATH);
    private static final boolean FAIL_IF_LOG_DIR_NOT_EXIST = false;

    /* Simple {@code ict}-cmdline parser to get a valid {@code ict.Properties} instance */
    final static class Cmdline {
        private static String HELP = ""
                + "# Usage: ict [OPTIONS]" + "\n"
                + "" + "\n"
                + "  Start a 'ict' instance by config." + "\n"
                + "" + "\n"
                + "# Options" + "\n"
                + "--help|-h           Print this help and exit" + "\n"
                + "--config|-c FILE    Use this config 'FILE' (default: ./ict.cfg;if-exist)" + "\n"
                + "                    - lookup first on environment for uppercase property keys" + "\n"
                + "                    - and as last in system properties" + "\n"
                + "--config-create     Create or overwrite './ict.cfg' file with parsed config" + "\n"
                + "--config-print      Print parsed config and exit" + "\n"
                + "" + "\n"
                + "--log-dir DIR       Write logs to existing DIR (default: logs/;currently unused)" + "\n"
                + "" + "\n"
                + "# Sample" + "\n"
                + "$ict --config-print                   # print out config" + "\n"
                + "$ict --config my-ict.cfg              # use my config" + "\n"
                + "$ict --config my-ict.cfg  -Dport=1234 # use my config but with port=1234" + "\n"
                + "$PORT=1234 && ict --config my-ict.cfg # use my config with port=1234 if not declared" + "\n"
                + "";

        private static final String ARG_CONFIG = "--config";
        private static final String ARG_CONFIG_SHORT = "-c";
        private static final String ARG_CONFIG_CREATE = "--config-create";
        private static final String ARG_CONFIG_PRINT = "--config-print";
        private static final String ARG_LOG_DIR = "--log-dir";
        private static final String ARG_HELP = "--help";
        private static final String ARG_HELP_SHORT = "-h";

        // Different properties configuration layers
        private final java.util.Properties hardcodedProperties = new Properties().toPropObject();
        private final java.util.Properties envProperties = new java.util.Properties();
        private final java.util.Properties defaultConfigProperties = new java.util.Properties();
        private final java.util.Properties configFileProperties = new java.util.Properties();
        private final java.util.Properties sysProperties = new java.util.Properties();

        // The cmdline options
        private boolean isHelp = false;
        private boolean isCreateConfig = false;
        private boolean isPrintConfig = false;
        private String logDir = DEFAULT_LOG_DIR_PATH;

        Cmdline() {/* prevent external construction */}

        /**
         * Parse the given cmdline args to init this.
         *
         * @param args not null arguments
         */
        public Cmdline parse(String[] args) {
            Objects.requireNonNull(args, "'args' must not be null.");

            isHelp = existArgument(args, ARG_HELP, ARG_HELP_SHORT);
            isCreateConfig = existArgument(args, ARG_CONFIG_CREATE);
            isPrintConfig = existArgument(args, ARG_CONFIG_PRINT);

            String logDirOrNull = parseValueOrNull(args, ARG_LOG_DIR);
            logDir = logDirOrNull != null ? logDirOrNull : DEFAULT_LOG_DIR_PATH;
            if (!Files.exists(Paths.get(logDir)) && FAIL_IF_LOG_DIR_NOT_EXIST) {
                throw new IllegalArgumentException("Configured log dir '" + logDir + "' not exist.");
            }

            String configFileOrNull = parseValueOrNull(args, ARG_CONFIG, ARG_CONFIG_SHORT);
            if (configFileOrNull != null) {
                try {
                    configFileProperties.load(new FileInputStream(configFileOrNull));
                } catch (IOException e) {
                    throw new IllegalArgumentException("Error while loading of ict config file '" + configFileOrNull + "'", e);
                }
            }
            return this;
        }

        /**
         * Create a {@link Properties} instance by using of different property configuration layers (environment, file, system).
         *
         * @return an valid instance or fail
         * @see #useEnvironmentProperties()
         * @see #useDefaultConfigFile()
         * @see #useSystemProperties()
         */
        public Properties getIctProperties() {
            java.util.Properties properties = new java.util.Properties();
            for (Properties.Property property : Properties.Property.values()) {
                String key = property.name();
                String value = null;

                if (hardcodedProperties.containsKey(key)) {
                    value = hardcodedProperties.getProperty(key);
                }

                if (envProperties.containsKey(key)) {
                    value = envProperties.getProperty(key);
                }

                if (defaultConfigProperties.containsKey(key)) {
                    value = defaultConfigProperties.getProperty(key);
                }

                if (configFileProperties.containsKey(key)) {
                    value = configFileProperties.getProperty(key);
                }

                if (sysProperties.containsKey(key)) {
                    value = sysProperties.getProperty(key);
                }

                Objects.requireNonNull(value, "'" + key + "' must be set via environment, config file or system property");
                properties.put(key, value);
            }
            return Properties.fromJavaProperties(properties);
        }

        /**
         * Try to read all {@link org.iota.ict.utils.Properties.Property} from {@code system properties}. Ignore not existing property keys.
         */
        public Cmdline useSystemProperties() {
            try {
                sysProperties.putAll(Properties.fromSystemProperties());
            } catch (Exception e) {
                throw new RuntimeException("Error while loading of ict system properties.", e);
            }
            return this;
        }

        /**
         * Try to read all {@link org.iota.ict.utils.Properties.Property} from {@code environment}. Looking for {@code upper case} keys only. Ignore not
         * existing one.
         */
        public Cmdline useEnvironmentProperties() {
            try {
                envProperties.putAll(Properties.fromEnvironment());
            } catch (Exception e) {
                throw new RuntimeException("Error while loading of ict environment properties.", e);
            }
            return this;
        }

        /**
         * Try to read all {@link org.iota.ict.utils.Properties.Property} from default config file {@link #DEFAULT_PROPERTY_FILE_PATH}. Ignore not existing
         * files. But fail if no read permission exist.
         */
        public Cmdline useDefaultConfigFile() {
            if (Files.exists(Paths.get(DEFAULT_PROPERTY_FILE_PATH))) {
                try {
                    defaultConfigProperties.load(new FileInputStream(DEFAULT_PROPERTY_FILE_PATH));
                } catch (Exception e) {
                    throw new RuntimeException("Error while loading of ict default config properties.", e);
                }
            }
            return this;
        }

        private boolean existArgument(String[] args, String... argsFilter) {
            List<String> argsList = Arrays.asList(args);
            for (String arg : argsFilter) {
                if (argsList.contains(arg)) {
                    return true;
                }
            }
            return false;
        }


        private String parseValueOrNull(String[] args, String... argsFilter) {
            for (int index = 0; index < args.length; index++) {
                String currentArg = args[index];
                for (String argFilter : argsFilter) {
                    if (currentArg.equals(argFilter)) {
                        int nextIndex = index + 1;
                        if (nextIndex < args.length) {
                            String argValue = args[nextIndex];
                            return argValue;
                        } else {
                            throw new IllegalArgumentException("No argument value exist for key '" + argFilter + "'");
                        }
                    }
                }
            }
            return null;
        }


    }

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {

        Cmdline cmdline = new Cmdline()
                .useEnvironmentProperties()
                .useDefaultConfigFile()
                .useSystemProperties();

        cmdline.parse(args);

        if (cmdline.isHelp) {
            System.out.println(Cmdline.HELP);
            System.exit(0);
        }

        Properties ictProperties = cmdline.getIctProperties();

        if (cmdline.isCreateConfig) {
            ictProperties.store(DEFAULT_PROPERTY_FILE_PATH);
        }

        if (cmdline.isPrintConfig) {
            System.out.println(ictProperties.toPropObject().toString().replace(", ", ",\n "));
            System.exit(0);
        }


        if (ictProperties.roundDuration < 30000 && ictProperties.spamEnabled) {
            logger.warn("Disabling spam because of low round duration.");
            ictProperties.spamEnabled = false;
        }

        logger.info("Starting new Ict '" + ictProperties.name + "' (version: " + Constants.ICT_VERSION + ")");

        Ict ict;
        try {
            ict = new Ict(ictProperties);
        } catch (Throwable t) {
            if (t.getCause() instanceof BindException) {
                ErrorHandler.handleError(logger, t, "\"Could not start Ict on \" + properties.host + \":\" + properties.port.");
                logger.info("Make sure that the address is correct and you are not already running an Ict instance or any other service on that port. You can change the port in your properties file.");
            } else
                ErrorHandler.handleError(logger, t, "Could not start Ict.");
            return;
        }
        logger.info("Ict started on " + ict.getAddress() + ".\n");

        final Ict finalRefToIct = ict;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("Terminating Ict ...");
                ErrorHandler.dump(DEFAULT_LOG_DIR);
                finalRefToIct.terminate();
            }
        });
    }
}
