package org.iota.ict.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

public class Properties {

    private static final String LIST_DELIMITER = ",";
    private static final Properties DEFAULT_PROPERTIES = new Properties();
    private static final Logger logger = LogManager.getLogger();

    public boolean ixiEnabled = false;
    public long minForwardDelay = 0;
    public long maxForwardDelay = 200;
    public String name = "ict";
    public String host = "localhost";
    public int port = 1337;
    public long logRoundDuration = 60000;
    public List<InetSocketAddress> neighbors = new LinkedList<>();
    public List<String> ixis = new LinkedList<>();

    public static Properties fromFile(String path) {
        java.util.Properties propObject = new java.util.Properties();
        try {
            propObject.load(new FileInputStream(path));
        } catch (IOException e) {
            logger.error("Failed loading properties from file", e);
        }
        return new Properties(propObject);
    }

    public Properties() {

    }

    Properties(java.util.Properties propObject) {
        minForwardDelay = readLongProperty(propObject, Property.min_forward_delay, 0, 10000, DEFAULT_PROPERTIES.minForwardDelay);
        maxForwardDelay = readLongProperty(propObject, Property.max_forward_delay, 0, 10000, DEFAULT_PROPERTIES.maxForwardDelay);
        host = propObject.getProperty(Property.host.name(), DEFAULT_PROPERTIES.host);
        name = propObject.getProperty(Property.name.name(), DEFAULT_PROPERTIES.name);
        port = (int) readLongProperty(propObject, Property.port, 1, 65535, DEFAULT_PROPERTIES.port);
        logRoundDuration = readLongProperty(propObject, Property.log_round_duration, 100, Long.MAX_VALUE, DEFAULT_PROPERTIES.logRoundDuration);
        neighbors = neighborsFromString(propObject.getProperty(Property.neighbors.name(), ""));
        ixiEnabled = propObject.getProperty(Property.ixi_enabled.name(), DEFAULT_PROPERTIES.ixiEnabled + "").toLowerCase().equals("true");
        ixis = stringListFromString(propObject.getProperty(Property.ixis.name(), ""));
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

    private long readLongProperty(java.util.Properties properties, Property property, long min, long max, long defaultValue) {
        long value = defaultValue;
        try {
            String string = properties.getProperty(property.name());
            if (string != null)
                value = Integer.parseInt(string);
        } catch (NumberFormatException e) {
            logger.error("Property '" + property.name() + "' must be an integer.");
        }

        if (value < min || value > max) {
            logger.error("Property '" + property.name() + "' must be in range: " + min + "-" + max);
            value = Math.min(max, Math.max(min, value));
        }

        return value;
    }

    public void store(String path) {
        try {
            toPropObject().store(new FileOutputStream(path), null);
        } catch (IOException e) {
            logger.error("Failed storing properties in file: " + path, e);
        }
    }

    java.util.Properties toPropObject() {
        java.util.Properties propObject = new java.util.Properties();
        propObject.setProperty(Property.min_forward_delay.name(), minForwardDelay + "");
        propObject.setProperty(Property.max_forward_delay.name(), maxForwardDelay + "");
        propObject.setProperty(Property.name.name(), name);
        propObject.setProperty(Property.host.name(), host);
        propObject.setProperty(Property.port.name(), port + "");
        propObject.setProperty(Property.log_round_duration.name(), logRoundDuration + "");
        propObject.setProperty(Property.neighbors.name(), neighborsToString());
        propObject.setProperty(Property.ixi_enabled.name(), ixiEnabled + "");
        propObject.setProperty(Property.ixis.name(), stringListToString(ixis));
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

    private static enum Property {
        min_forward_delay, max_forward_delay, port, host, log_round_duration, neighbors, ixi_enabled, ixis, name;
    }
}
