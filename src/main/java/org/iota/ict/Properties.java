package org.iota.ict;

public class Properties {
    public long minForwardDelay = 0; // TODO
    public long maxForwardDelay = 10;
    public int port;
    public String host = "localhost";
    public int roundDuration = 60000;

    public Properties port(int port) {
        this.port = port;
        return this;
    }

    public Properties host(String host) {
        this.host = host;
        return this;
    }
}
