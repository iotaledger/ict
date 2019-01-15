package org.iota.ict;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.api.GithubGateway;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Properties;
import org.iota.ict.utils.VersionComparator;

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

    public static final String DEFAULT_LOG_DIR_PATH = "logs/";
    private static final File DEFAULT_LOG_DIR = new File(DEFAULT_LOG_DIR_PATH);
    private static final String DEFAULT_LOG_FILE_NAME = "ict.log";
    private static final String DEFAULT_ENABLE_LOGFILE_CREATION_ON_STARTUP = "false";

    /* Simple {@code ict}-cmdline parser to get a valid {@code ict.Properties} instance */
    final static class Cmdline {
        private static String HELP = ""
                + "# Usage: ict [OPTIONS]" + "\n"
                + "" + "\n"
                + "  Start a 'ict' instance by config." + "\n"
                + "" + "\n"
                + "# Options" + "\n"
                + "--help|-h              Print this help and exit" + "\n"
                + "--config|-c FILE       Use this config 'FILE' (default: ./ict.cfg;if-exist)" + "\n"
                + "                       - lookup first on environment for uppercase property keys" + "\n"
                + "                       - and as last in system properties" + "\n"
                + "--config-create        Create or overwrite './ict.cfg' file with parsed config" + "\n"
                + "--config-print         Print parsed config and exit" + "\n"
                + "                       - on verbose print cmdline- and log4j-config too" + "\n"
                + "" + "\n"
                + "--logfile-enabled      Enable logging to 'logs/ict.log'" + "\n"
                + "--log-dir DIR          Write logs to existing 'DIR' (default: logs/)" + "\n"
                + "--log-file NAME        Write logs to 'FILE' (default: ict.log)" + "\n"
                + "-v|--verbose|--debug   Set log.level=DEBUG (default:INFO)" + "\n"
                + "-vv|--verbose2|--trace Set log.level=TRACE (default:INFO)" + "\n"
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
        private static final String ARG_VERBOSE = "--verbose";
        private static final String ARG_TRACE_AS_VERSOSE = "--verbose2";
        private static final String ARG_VERBOSE_SHORT = "-v";
        private static final String ARG_TRACE_SHORT = "-vv";
        private static final String ARG_VERBOSE_AS_DEBUG = "--debug";
        private static final String ARG_TRACE = "--trace";
        private static final String ARG_LOGFILE_ENABLED = "--logfile-enabled";
        private static final String ARG_LOG_DIR = "--log-dir";
        private static final String ARG_LOG_FILE = "--log-file";
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
        private boolean isDebugEnabled = false;
        private boolean isTraceEnabled = false;
        private boolean isLogFileEnabled = false;
        private String logDir = DEFAULT_LOG_DIR_PATH;
        private String logFilename = DEFAULT_LOG_FILE_NAME;

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
            isDebugEnabled = existArgument(args, ARG_VERBOSE_SHORT, ARG_VERBOSE, ARG_VERBOSE_AS_DEBUG);
            isTraceEnabled = existArgument(args, ARG_TRACE_SHORT, ARG_TRACE_AS_VERSOSE, ARG_TRACE);
            isLogFileEnabled = existArgument(args, ARG_LOGFILE_ENABLED);

            String logDirOrNull = parseValueOrNull(args, ARG_LOG_DIR);
            logDir = logDirOrNull != null ? logDirOrNull : DEFAULT_LOG_DIR_PATH;
            if (!Files.exists(Paths.get(logDir)) && this.isLogFileEnabled) {
                throw new IllegalArgumentException("Configured log dir '" + logDir + "' not exist.");
            }

            String logFileOrNull = parseValueOrNull(args, ARG_LOG_FILE);
            logFilename = logFileOrNull != null ? logFileOrNull : DEFAULT_LOG_FILE_NAME;

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
         * Try to read all {@link org.iota.ict.utils.Properties.Property} from default config file {@link Constants#DEFAULT_PROPERTY_FILE_PATH}. Ignore not existing
         * files. But fail if no read permission exist.
         */
        public Cmdline useDefaultConfigFile() {
            if (Files.exists(Paths.get(Constants.DEFAULT_PROPERTY_FILE_PATH))) {
                try {
                    defaultConfigProperties.load(new FileInputStream(Constants.DEFAULT_PROPERTY_FILE_PATH));
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

        @Override
        public String toString() {
            return "Cmdline{" + "\n" +
                    " hardcodedProperties=" + hardcodedProperties + ",\n" +
                    " envProperties=" + envProperties + ",\n" +
                    " defaultConfigProperties=" + defaultConfigProperties + ",\n" +
                    " configFileProperties=" + configFileProperties + ",\n" +
                    " sysProperties=" + sysProperties + ",\n" +
                    " isHelp=" + isHelp + ",\n" +
                    " isCreateConfig=" + isCreateConfig + ",\n" +
                    " isPrintConfig=" + isPrintConfig + ",\n" +
                    " isDebugEnabled=" + isDebugEnabled + ",\n" +
                    " isTraceEnabled=" + isTraceEnabled + ",\n" +
                    " isLogFileEnabled=" + isLogFileEnabled + ",\n" +
                    " logDir='" + logDir + '\'' + ",\n" +
                    " logFilename='" + logFilename + '\'' + "\n" +
                    '}';
        }
    }

    /* Log4jConfig enable dynamic init of 'classpath:/log4j2.xml' on startup */
    final static class Log4JConfig {
        private static final String CTX_KEY_LOG_FILE_ENABLED = "logFileEnabled";
        private static final String CTX_KEY_LOG_FILENAME = "logFilename";
        private static final String CTX_KEY_LOG_DIR = "logDir";

        private final boolean disableRootLevelSetting;
        private Level rootLevel = Level.INFO;

        static Log4JConfig getDefault() {
            return new Log4JConfig();
        }

        private Log4JConfig() {
            disableRootLevelSetting = System.getProperties().containsKey("log4j.configurationFile");
            ThreadContext.put(CTX_KEY_LOG_DIR, Main.DEFAULT_LOG_DIR_PATH);
            ThreadContext.put(CTX_KEY_LOG_FILENAME, Main.DEFAULT_LOG_FILE_NAME);
            ThreadContext.put(CTX_KEY_LOG_FILE_ENABLED, DEFAULT_ENABLE_LOGFILE_CREATION_ON_STARTUP);
        }

        public void enableDebug() {
            if (disableRootLevelSetting) {
                return;
            }
            rootLevel = Level.DEBUG;
            Configurator.setRootLevel(Level.DEBUG);
        }

        public void enableTrace() {
            if (disableRootLevelSetting) {
                return;
            }
            rootLevel = Level.TRACE;
            Configurator.setRootLevel(Level.TRACE);
        }

        public void enableLogToFile(String logDir, String logFilename) {
            ThreadContext.put(CTX_KEY_LOG_DIR, logDir);
            ThreadContext.put(CTX_KEY_LOG_FILENAME, logFilename);
            ThreadContext.put(CTX_KEY_LOG_FILE_ENABLED, "true");
        }

        @Override
        public String toString() {
            return "Log4jConfig{" + "\n" +
                    " logLevel=" + rootLevel + ",\n" +
                    " isEnabled(logLevel-setting)=" + disableRootLevelSetting + ",\n" +
                    " " + CTX_KEY_LOG_DIR + "=" + ThreadContext.get(CTX_KEY_LOG_DIR) + ",\n" +
                    " " + CTX_KEY_LOG_FILENAME + "=" + ThreadContext.get(CTX_KEY_LOG_FILENAME) + ",\n" +
                    " " + CTX_KEY_LOG_FILE_ENABLED + "=" + ThreadContext.get(CTX_KEY_LOG_FILE_ENABLED) + "\n" +
                    "}";
        }
    }


    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        Log4JConfig log4jConfig = Log4JConfig.getDefault();

        Constants.TESTING = false;

        checkForUpdates();

        Cmdline cmdline = new Cmdline()
                .useEnvironmentProperties()
                .useDefaultConfigFile()
                .useSystemProperties();

        cmdline.parse(args);

        if (cmdline.isHelp) {
            System.out.println(Cmdline.HELP);
            System.exit(0);
        }

        if (cmdline.isDebugEnabled) {
            log4jConfig.enableDebug();
        }

        if (cmdline.isTraceEnabled) {
            log4jConfig.enableTrace();
        }

        if (cmdline.isLogFileEnabled) {
            log4jConfig.enableLogToFile(cmdline.logDir, cmdline.logFilename);
        }

        Properties ictProperties = cmdline.getIctProperties();

        if (cmdline.isCreateConfig) {
            ictProperties.store(Constants.DEFAULT_PROPERTY_FILE_PATH);
        }

        if (cmdline.isPrintConfig) {
            if (cmdline.isDebugEnabled || cmdline.isTraceEnabled) {
                System.out.println(log4jConfig);
                System.out.println(cmdline);
            }
            System.out.println(ictProperties.toPropObject().toString().replace(", ", ",\n "));
            System.exit(0);
        }

        logger.info("Starting new Ict '" + ictProperties.name + "' (version: " + Constants.ICT_VERSION + ")");
        Ict ict;
        try {
            ict = new Ict(ictProperties);
        } catch (Throwable t) {
            if (t.getCause() instanceof BindException) {
                logger.error("Could not start Ict on " + ictProperties.host + ":" + ictProperties.port, t);
                logger.info("Make sure that the address is correct and you are not already running an Ict instance or any other service on that port. You can change the port in your properties file.");
            } else
                logger.error("Could not start Ict.", t);
            return;
        }

        ict.getModuleHolder().initAllModules();
        ict.getModuleHolder().start();

        final Ict finalRefToIct = ict;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                finalRefToIct.terminate();
            }
        });
    }

    private static void checkForUpdates() {
        System.out.println();
        logger.info("Checking for updates ...");

        try {
            String latestReleaseVersion = GithubGateway.getLatestReleaseLabel(Constants.ICT_REPOSITORY);
            if(VersionComparator.getInstance().compare(Constants.ICT_VERSION, latestReleaseVersion) < 0)
                logger.warn(">>>>> A new release of Ict is available. Please update to " + latestReleaseVersion + "! <<<<<");
            else
                logger.info("You are already up-to-date!");
        } catch (Throwable t) {
            logger.error("Failed checking for updates", t);
        }
        System.out.println();
    }
}
