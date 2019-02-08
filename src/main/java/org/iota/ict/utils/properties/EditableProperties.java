package org.iota.ict.utils.properties;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

public class EditableProperties extends Properties {

    public EditableProperties() {
        super();
    }

    EditableProperties(java.util.Properties propObject) {
        super(propObject);
    }

    // === SETTERS ===

    public EditableProperties port(int port) {
        this.port = port;
        return this;
    }

    public EditableProperties host(String host) {
        this.host = host;
        return this;
    }

    public EditableProperties guiPort(int guiPort) {
        this.guiPort = guiPort;
        return this;
    }

    public EditableProperties guiEnabled(boolean guiEnabled) {
        this.guiEnabled = guiEnabled;
        return this;
    }

    public EditableProperties neighbors(List<InetSocketAddress> neighbors) {
        this.neighbors = new LinkedList<>(neighbors);
        return this;
    }

    public EditableProperties economicCluster(List<String> economicCluster) {
        this.economicCluster = economicCluster;
        return this;
    }

    public EditableProperties antiSpamAbs(long antiSpamAbs) {
        this.antiSpamAbs = antiSpamAbs;
        return this;
    }

    public EditableProperties maxForwardDelay(long maxForwardDelay) {
        this.maxForwardDelay = maxForwardDelay;
        return this;
    }

    public EditableProperties minForwardDelay(long minForwardDelay) {
        this.minForwardDelay = minForwardDelay;
        return this;
    }

    public EditableProperties roundDuration(long roundDuration) {
        this.roundDuration = roundDuration;
        return this;
    }

    public EditableProperties tangleCapacity(long tangleCapacity) {
        this.tangleCapacity = tangleCapacity;
        return this;
    }

    public EditableProperties guiPassword(String guiPassword) {
        this.guiPassword = guiPassword;
        return this;
    }

    public EditableProperties name(String name) {
        this.name = name;
        return this;
    }

    public EditableProperties maxHeapSize(double maxHeapSize) {
        this.maxHeapSize = maxHeapSize;
        return this;
    }
}
