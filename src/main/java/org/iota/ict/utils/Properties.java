package org.iota.ict.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * With instances of this class, the Ict and its sub-components can be easily configured. The properties can be read from files or defined during runtime. Some
 * properties might not be changeable yet after passing them to the Ict.
 *
 * @see org.iota.ict.Ict#Ict(Properties)
 */
public class Properties implements Cloneable {

    private static final String LIST_DELIMITER = ",";
    private static final Properties DEFAULT_PROPERTIES = new Properties();
    private static final Logger logger = LogManager.getLogger(Properties.class);

    public double antiSpamRel = 5;
    public long antiSpamAbs = 1000;
    public boolean guiEnabled = true;
    public boolean spamEnabled = false;
    public long tangleCapacity = 10000;
    public long minForwardDelay = 0;
    public long maxForwardDelay = 200;
    public String name = "ict";
    public String host = "0.0.0.0";
    public int port = 1337;
    public int guiPort = 2187;
    public long roundDuration = 60000;
    public List<InetSocketAddress> neighbors = new LinkedList<>();

    public static Properties fromFile(String path) {
        java.util.Properties propObject = new java.util.Properties();
        try {
            propObject.load(new FileInputStream(path));
        } catch (IOException e) {
            ErrorHandler.handleError(logger, e, "Failed loading properties from file");
        }
        return new Properties(propObject);
    }

    public static Properties fromJavaProperties(java.util.Properties javaProperties) {
        return new Properties(javaProperties);
    }

    public static Properties fromJSON(JSONObject json) {
        java.util.Properties propObject = new java.util.Properties();
        for(String key : json.keySet()) {
            Object value = json.get(key);
            if(value == null)
                value = "";
            if(value instanceof JSONArray) {
                value = value.toString().replace("[", "").replace("]", "").replace("\"", "");
                System.out.println(value);
            }
            propObject.put(key, value.toString());
        }
        return new Properties(propObject);
    }

    public Properties() {

    }

    private Properties(java.util.Properties propObject) {
        tangleCapacity = readLongProperty(propObject, Property.tangle_capacity, 10, Long.MAX_VALUE, DEFAULT_PROPERTIES.tangleCapacity);
        antiSpamRel = readDoublePorperty(propObject, Property.anti_spam_rel, 1, 1000, DEFAULT_PROPERTIES.antiSpamRel);
        antiSpamAbs = readLongProperty(propObject, Property.anti_spam_abs, 1, Long.MAX_VALUE, DEFAULT_PROPERTIES.antiSpamAbs);
        minForwardDelay = readLongProperty(propObject, Property.min_forward_delay, 0, 10000, DEFAULT_PROPERTIES.minForwardDelay);
        maxForwardDelay = readLongProperty(propObject, Property.max_forward_delay, 0, 10000, DEFAULT_PROPERTIES.maxForwardDelay);
        host = propObject.getProperty(Property.host.name(), DEFAULT_PROPERTIES.host);
        name = propObject.getProperty(Property.name.name(), DEFAULT_PROPERTIES.name);
        port = (int) readLongProperty(propObject, Property.port, 1, 65535, DEFAULT_PROPERTIES.port);
        roundDuration = readLongProperty(propObject, Property.round_duration, 100, Long.MAX_VALUE, DEFAULT_PROPERTIES.roundDuration);
        neighbors = neighborsFromString(propObject.getProperty(Property.neighbors.name(), ""));
        guiEnabled = propObject.getProperty(Property.gui_enabled.name(), DEFAULT_PROPERTIES.guiEnabled + "").toLowerCase().equals("true");
        guiPort = (int) readLongProperty(propObject, Property.gui_port, 1, 65535, DEFAULT_PROPERTIES.guiPort);
        spamEnabled = propObject.getProperty(Property.spam_enabled.name(), DEFAULT_PROPERTIES.spamEnabled + "").toLowerCase().equals("true");
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
                ErrorHandler.handleError(logger, t, "Invalid neighbor address: '" + address + "'");
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
            ErrorHandler.handleWarning(logger, e, "Property '" + property.name() + "' must be an integer.");
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
            ErrorHandler.handleWarning(logger, e, "Property '" + property.name() + "' must be an integer.");
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
            ErrorHandler.handleWarning(logger, e, "Failed storing properties in file: " + path);
        }
    }

    public java.util.Properties toPropObject() {
        java.util.Properties propObject = new java.util.Properties();
        propObject.setProperty(Property.tangle_capacity.name(), tangleCapacity + "");
        propObject.setProperty(Property.anti_spam_rel.name(), antiSpamRel + "");
        propObject.setProperty(Property.anti_spam_abs.name(), antiSpamAbs + "");
        propObject.setProperty(Property.min_forward_delay.name(), minForwardDelay + "");
        propObject.setProperty(Property.max_forward_delay.name(), maxForwardDelay + "");
        propObject.setProperty(Property.name.name(), name);
        propObject.setProperty(Property.host.name(), host);
        propObject.setProperty(Property.port.name(), port + "");
        propObject.setProperty(Property.round_duration.name(), roundDuration + "");
        propObject.setProperty(Property.neighbors.name(), neighborsToString());
        propObject.setProperty(Property.gui_enabled.name(), guiEnabled + "");
        propObject.setProperty(Property.gui_port.name(), guiPort + "");
        propObject.setProperty(Property.spam_enabled.name(), spamEnabled + "");
        return propObject;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put(Property.tangle_capacity.name(), tangleCapacity);
        json.put(Property.anti_spam_rel.name(), antiSpamRel);
        json.put(Property.anti_spam_abs.name(), antiSpamAbs);
        json.put(Property.min_forward_delay.name(), minForwardDelay);
        json.put(Property.max_forward_delay.name(), maxForwardDelay);
        json.put(Property.name.name(), name);
        json.put(Property.host.name(), host);
        json.put(Property.port.name(), port);
        json.put(Property.round_duration.name(), roundDuration);
        json.put(Property.neighbors.name(), new JSONArray(neighborsToString().split(",")));
        json.put(Property.gui_enabled.name(), guiEnabled);
        json.put(Property.gui_port.name(), guiPort);
        json.put(Property.spam_enabled.name(), spamEnabled);
        return json;
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
        gui_enabled,
        gui_port,
        spam_enabled,
        name;
    }

    @Override
    public Properties clone() {
        try {
            Properties clone = (Properties) super.clone();
            clone.neighbors = new LinkedList<>(neighbors);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Properties && this.toPropObject().equals(((Properties) o).toPropObject());
    }

    @Override
    public int hashCode() {
        return toPropObject().entrySet().hashCode();
    }
}