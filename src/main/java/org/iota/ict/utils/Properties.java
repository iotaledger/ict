package org.iota.ict.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * With instances of this class, the Ict and its sub-components can be easily configured. The properties can be read from files or defined during runtime. Some
 * properties might not be changeable yet after passing them to the Ict.
 *
 * @see org.iota.ict.Ict#Ict(Properties)
 */
public class Properties {

    private static final String LIST_DELIMITER = ",";
    private static final Properties DEFAULT_PROPERTIES = new Properties();
    private static final Logger logger = LogManager.getLogger(Properties.class);

    public double antiSpamRel = 5;
    public long amtiSpamAbs = 1000;
    public boolean spamEnabled = false;
    public boolean ixiEnabled = false;
    public long tangleCapacity = 10000;
    public long minForwardDelay = 0;
    public long maxForwardDelay = 200;
    public String name = "ict";
    public String host = "0.0.0.0";
    public int port = 1337;
    public long roundDuration = 60000;
    public List<InetSocketAddress> neighbors = new LinkedList<>();

    public static Properties fromFile(String path) {
        java.util.Properties propObject = new java.util.Properties();
        try {
            propObject.load(new FileInputStream(path));
        } catch (IOException e) {
            logger.error("Failed loading properties from file", e);
        }
        return new Properties(propObject);
    }

    public static Properties fromJavaProperties(java.util.Properties javaProperties) {
        return new Properties(javaProperties);
    }

    public Properties() {

    }

    private Properties(java.util.Properties propObject) {
        tangleCapacity = readLongProperty(propObject, Property.tangle_capacity, 10, Long.MAX_VALUE, DEFAULT_PROPERTIES.tangleCapacity);
        antiSpamRel = readDoublePorperty(propObject, Property.anti_spam_rel, 1, 1000, DEFAULT_PROPERTIES.antiSpamRel);
        amtiSpamAbs = readLongProperty(propObject, Property.anti_spam_abs, 1, Long.MAX_VALUE, DEFAULT_PROPERTIES.amtiSpamAbs);
        minForwardDelay = readLongProperty(propObject, Property.min_forward_delay, 0, 10000, DEFAULT_PROPERTIES.minForwardDelay);
        maxForwardDelay = readLongProperty(propObject, Property.max_forward_delay, 0, 10000, DEFAULT_PROPERTIES.maxForwardDelay);
        host = propObject.getProperty(Property.host.name(), DEFAULT_PROPERTIES.host);
        name = propObject.getProperty(Property.name.name(), DEFAULT_PROPERTIES.name);
        port = (int) readLongProperty(propObject, Property.port, 1, 65535, DEFAULT_PROPERTIES.port);
        roundDuration = readLongProperty(propObject, Property.round_duration, 100, Long.MAX_VALUE, DEFAULT_PROPERTIES.roundDuration);
        neighbors = neighborsFromString(propObject.getProperty(Property.neighbors.name(), ""));
        spamEnabled = propObject.getProperty(Property.spam_enabled.name(), DEFAULT_PROPERTIES.spamEnabled + "").toLowerCase().equals("true");
        ixiEnabled = propObject.getProperty(Property.ixi_enabled.name(), DEFAULT_PROPERTIES.ixiEnabled + "").toLowerCase().equals("true");
    }

    private static List<String> stringListFromString(String string) {
        List<String> stringList = new LinkedList<>();
        for (String element : string.split(LIST_DELIMITER)) {
            if (element.length() == 0)
                continue;
            stringList.add(element);
        }
        return stringList;
    }

    private static List<InetSocketAddress> neighborsFromString(String string) {
        List<String> addresses = stringListFromString(string);

        List<InetSocketAddress> neighbors = new LinkedList<>();
        for (String address : addresses) {
            try {
                neighbors.add(inetSocketAddressFromString(address));
            } catch (Throwable t) {
                logger.error("Invalid neighbor address: '" + address + "'", t);
            }
        }
        return neighbors;
    }

    private static InetSocketAddress inetSocketAddressFromString(String address) {
        int portColonIndex;
        for (portColonIndex = address.length() - 1; address.charAt(portColonIndex) != ':'; portColonIndex--) ;
        String hostString = address.substring(0, portColonIndex);
        int port = Integer.parseInt(address.substring(portColonIndex + 1, address.length()));
        return new InetSocketAddress(hostString, port);
    }

    public static java.util.Properties fromSystemProperties() {
        java.util.Properties systemProperties = new java.util.Properties();
        for (Property property : Property.values()) {
            String valueOrNull = System.getProperty(property.name());
            if(valueOrNull != null){
                systemProperties.put(property.name(), valueOrNull);
            }
        }
        return systemProperties;
    }

    public static java.util.Properties fromEnvironment() {
        java.util.Properties environmentProperties = new java.util.Properties();
        Map<String, String> envMap = System.getenv();
        for (Property property : Property.values()) {
            String upperCaseName = property.name();
            String valueOrNull = envMap.get(upperCaseName);
            if(valueOrNull != null){
                environmentProperties.put(property.name(), valueOrNull);
            }
        }
        return environmentProperties;
    }

    private String neighborsToString() {
        List<String> addresses = new LinkedList<>();
        for (InetSocketAddress address : neighbors)
            addresses.add(address.getHostString() + ":" + address.getPort());
        return stringListToString(addresses);
    }

    private String stringListToString(List<String> stringList) {
        StringBuilder sb = new StringBuilder();
        for (String element : stringList)
            sb.append(element).append(LIST_DELIMITER);
        return sb.length() == 0 ? "" : sb.deleteCharAt(sb.length() - 1).toString();
    }

    private double readDoublePorperty(java.util.Properties properties, Property property, double min, double max, double defaultValue) {
        double value = defaultValue;
        try {
            String string = properties.getProperty(property.name());
            if (string != null)
                value = Double.parseDouble(string);
        } catch (NumberFormatException e) {
            logger.warn(("Property '" + property.name() + "' must be an integer.") + " (" + e.getMessage() + ")");
        }

        if (value < min || value > max) {
            logger.warn("Property '" + property.name() + "' must be in range: " + min + "-" + max);
            value = Math.min(max, Math.max(min, value));
        }

        return value;
    }


    private long readLongProperty(java.util.Properties properties, Property property, long min, long max, long defaultValue) {
        long value = defaultValue;
        try {
            String string = properties.getProperty(property.name());
            if (string != null)
                value = Long.parseLong(string);
        } catch (NumberFormatException e) {
            logger.warn(("Property '" + property.name() + "' must be an integer.") + " (" + e.getMessage() + ")");
        }

        if (value < min || value > max) {
            logger.warn("Property '" + property.name() + "' must be in range: " + min + "-" + max);
            value = Math.min(max, Math.max(min, value));
        }

        return value;
    }

    public void store(String path) {
        try {
            toPropObject().store(new FileOutputStream(path), null);
        } catch (IOException e) {
            logger.warn(("Failed storing properties in file: " + path) + " (" + e.getMessage() + ")");
        }
    }

    public java.util.Properties toPropObject() {
        java.util.Properties propObject = new java.util.Properties();
        propObject.setProperty(Property.tangle_capacity.name(), tangleCapacity + "");
        propObject.setProperty(Property.anti_spam_rel.name(), antiSpamRel + "");
        propObject.setProperty(Property.anti_spam_abs.name(), amtiSpamAbs + "");
        propObject.setProperty(Property.min_forward_delay.name(), minForwardDelay + "");
        propObject.setProperty(Property.max_forward_delay.name(), maxForwardDelay + "");
        propObject.setProperty(Property.name.name(), name);
        propObject.setProperty(Property.host.name(), host);
        propObject.setProperty(Property.port.name(), port + "");
        propObject.setProperty(Property.round_duration.name(), roundDuration + "");
        propObject.setProperty(Property.neighbors.name(), neighborsToString());
        propObject.setProperty(Property.spam_enabled.name(), spamEnabled + "");
        propObject.setProperty(Property.ixi_enabled.name(), ixiEnabled + "");
        return propObject;
    }

    public Properties port(int port) {
        this.port = port;
        return this;
    }

    public Properties host(String host) {
        this.host = host;
        return this;
    }

    public enum Property {
        anti_spam_rel,
        anti_spam_abs,
        tangle_capacity,
        min_forward_delay,
        max_forward_delay,
        port,
        host,
        round_duration,
        neighbors,
        spam_enabled,
        ixi_enabled,
        name;
    }

}
