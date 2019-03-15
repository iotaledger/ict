package org.iota.ict.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.IctInterface;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.utils.*;
import org.iota.ict.utils.properties.FinalProperties;
import org.iota.ict.utils.properties.Properties;
import org.iota.ict.utils.properties.PropertiesUser;
import org.json.JSONObject;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.*;

public class Node extends RestartableThread implements PropertiesUser {

    protected final static Logger LOGGER = LogManager.getLogger("Node");
    protected final IctInterface ict;

    protected final List<Neighbor> neighbors = new LinkedList<>();
    protected final SenderInterface sender;
    protected final RestartableThread receiver;
    protected FinalProperties properties;

    protected InetSocketAddress address;
    protected DatagramSocket socket;

    protected int round;

    public Node(IctInterface ict) {
        super(LOGGER);
        this.ict = ict;
        this.properties = ict.getProperties();
        this.receiver = new Receiver(this);
        this.sender = new Sender(this, properties);

        ict.addGossipListener(sender);
        subWorkers.add(receiver);
        subWorkers.add(sender);

        for (String neighbor : properties.neighbors()) {
            neighbor(neighbor);
        }
    }

    public int getSenderQueueSize() {
        return sender.queueSize();
    }

    public void log() {
        int queueSize = sender.queueSize();
        LOGGER.debug("forwarding queue size: " + queueSize);
        if (queueSize > 200) {
            LOGGER.warn("There is a backlog of " + queueSize + " transactions to be forwarded. This might cause memory issues. You can monitor this metric via `--debug`.");
            IssueCollector.log();
        }
    }

    @Override
    public void onStart() {
        this.address = new InetSocketAddress(properties.host(), properties.port());
        this.socket = createDatagramSocket(address);
    }

    @Override
    public void onTerminated() {
        if (!socket.isClosed())
            socket.close();
        socket = null;
    }

    @Override
    public void run() {
    }

    @Override
    public void updateProperties(FinalProperties newProperties) {
        FinalProperties oldProperties = properties;
        this.properties = newProperties;
        updateNeighborsBecausePropertiesChanged(oldProperties, newProperties);

        if (address.getPort() != newProperties.port() || !address.getHostName().equals(newProperties.host()))
            updateHostAndPort(newProperties.host(), newProperties.port());

        sender.updateProperties(newProperties);
    }

    private void updateHostAndPort(String newHost, int newPort) {
        receiver.terminate(); // closes socket()
        sender.terminate();

        address = new InetSocketAddress(newHost, newPort);
        this.socket = createDatagramSocket(address);

        sender.start();
        receiver.start();
    }

    public void broadcast(Transaction transaction) {
        sender.queue(transaction);
    }

    public void request(String transactionHash) {
        sender.request(transactionHash);
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public List<Neighbor> getNeighbors() {
        return new LinkedList<>(neighbors);
    }

    private void neighbor(String neighbor) {
        if (neighbors.size() >= Constants.MAX_NEIGHBOR_COUNT)
            throw new IllegalStateException("Already reached maximum amount of neighbors.");
        neighbors.add(new Neighbor(neighbor, properties.antiSpamAbs()));
    }

    private void updateNeighborsBecausePropertiesChanged(Properties oldProp, Properties newProp) {
        // remove neighbors who are no longer neighbors
        removeNeighborsWhoAreNoLongerNeighbors(oldProp, newProp);
        addNeighborsWhoAreNew(oldProp, newProp);

        assert neighbors.size() == newProp.neighbors().size();
    }

    private void removeNeighborsWhoAreNoLongerNeighbors(Properties oldProp, Properties newProp) {
        List<String> toRemove = new LinkedList<>();
        for (String nb : oldProp.neighbors())
            if (!newProp.neighbors().contains(nb))
                toRemove.add(nb);

        List<Neighbor> toRemoveNeighbors = new LinkedList<>();
        for(Neighbor nb : neighbors) {
            if(toRemove.contains(nb.getAddress()))
                toRemoveNeighbors.add(nb);
        }
        neighbors.removeAll(toRemoveNeighbors);
    }

    private void addNeighborsWhoAreNew(Properties oldProp, Properties newProp) {
        // add neighbors who are new
        Set<String> newNeighbors = new HashSet<>();
        if(oldProp == null) {
            newNeighbors = newProp.neighbors();
        } else {
            for (String inetAddress : newProp.neighbors()) {
                if (!oldProp.neighbors().contains(inetAddress))
                    newNeighbors.add(inetAddress);
            }
        }

        for (String toAdd : newNeighbors)
            neighbor(toAdd);
    }

    private static DatagramSocket createDatagramSocket(InetSocketAddress address) {
        try {
            return new DatagramSocket(address);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public static void logHeader() {
        StringBuilder report = new StringBuilder();
        report.append(pad("ALL")).append('|');
        report.append(pad("NEW")).append('|');
        report.append(pad("REQ")).append('|');
        report.append(pad("INV")).append('|');
        report.append(pad("IGN"));
        report.append("   ").append("ADDRESS");
        LOGGER.info(report);
    }

    private static String pad(String str) {
        return String.format("%1$-5s", str);
    }

    public void newRound() {

        if (round++ % 10 == 0)
            logHeader();
        // two separate FOR-loops to prevent delays between newRound() calls
        for (Neighbor neighbor : ict.getNeighbors()) {
            long tolerance = ict.getProperties().antiSpamAbs();
            neighbor.newRound(tolerance, true);
        }
        for (Neighbor neighbor : ict.getNeighbors())
            neighbor.resolveHost();
    }
}
