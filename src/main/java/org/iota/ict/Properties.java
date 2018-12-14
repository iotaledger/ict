package org.iota.ict;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Properties {

    private static final Properties DEFAULT_PROPERTIES = new Properties();

    public long minForwardDelay = 0;
    public long maxForwardDelay = 200;
    public String host = "localhost";
    public int port = 1337;
    public long logRoundDuration = 60000;

    public static Properties fromFile(String path) {
        java.util.Properties propObject = new java.util.Properties();
        try {
            propObject.load(new FileInputStream(path));
        } catch (IOException e) {
            // TODO use logger
            System.err.println("Failed loading properties from file: " + path);
            e.printStackTrace();
        }
        return new Properties(propObject);
    }

    public Properties() {

    }

    Properties(java.util.Properties propObject) {
        minForwardDelay = readLongProperty(propObject, Property.min_forward_delay, 0, 10000, DEFAULT_PROPERTIES.minForwardDelay);
        maxForwardDelay = readLongProperty(propObject, Property.max_forward_delay, 0, 10000, DEFAULT_PROPERTIES.maxForwardDelay);
        host = propObject.getProperty(Property.host.name(), DEFAULT_PROPERTIES.host);
        port = (int) readLongProperty(propObject, Property.port, 1, 65535, DEFAULT_PROPERTIES.port);
        logRoundDuration = readLongProperty(propObject, Property.log_round_duration, 100, Long.MAX_VALUE, DEFAULT_PROPERTIES.logRoundDuration);
    }

    private long readLongProperty(java.util.Properties properties, Property property, long min, long max, long defaultValue) {
        long value = defaultValue;
        try {
            String string = properties.getProperty(property.name());
            if(string != null)
                value = Integer.parseInt(string);
        } catch (NumberFormatException e) {
            // TODO use logger
            System.err.println("Property '"+property.name()+"' must be an integer.");
        }

        if(value < min || value > max) {
            // TODO use logger
            System.err.println("Property '"+property.name()+"' must be in range "+min+"-"+max);
            value = Math.min(max, Math.max(min, value));
        }

        return value;
    }

    public void store(String path) {
        try {
            toPropObject().store(new FileOutputStream(path), null);
        } catch (IOException e) {
            // TODO use logger
            System.err.println("Failed storing properties in file: " + path);
            e.printStackTrace();
        }
    }

    java.util.Properties toPropObject() {
        java.util.Properties propObject = new java.util.Properties();
        propObject.setProperty(Property.min_forward_delay.name(), minForwardDelay+"");
        propObject.setProperty(Property.max_forward_delay.name(), maxForwardDelay+"");
        propObject.setProperty(Property.host.name(), host);
        propObject.setProperty(Property.port.name(), port+"");
        propObject.setProperty(Property.log_round_duration.name(), logRoundDuration+"");
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
        min_forward_delay, max_forward_delay, port, host, log_round_duration;
    }
}
