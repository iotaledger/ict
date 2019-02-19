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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Node extends RestartableThread implements PropertiesUser {

    protected final static Logger LOGGER = LogManager.getLogger("Node");
    protected final IctInterface ict;

    protected final List<Neighbor> neighbors = new LinkedList<>();
    protected final SenderInterface sender;
    protected final RestartableThread receiver;
    protected FinalProperties properties;

    protected InetSocketAddress address;
    protected DatagramSocket socket;

    protected List<Round> rounds = new ArrayList<>();
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
            String host = neighbor.split(":")[0];
            int port = Integer.parseInt(neighbor.split(":")[1]);
            neighbor(new InetSocketAddress(host, port));
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
        String host = neighbor.split(":")[0];
        int port = Integer.parseInt(neighbor.split(":")[1]);
        neighbor(new InetSocketAddress(host, port));
    }

    /**
     * Opens a new connection to a neighbor. Both nodes will directly gossip transactions.
     *
     * @param neighborAddress Address of neighbor to connect to.
     * @throws IllegalStateException If already has {@link Constants#MAX_NEIGHBOR_COUNT} neighbors.
     */
    public void neighbor(InetSocketAddress neighborAddress) {
        if (neighbors.size() >= Constants.MAX_NEIGHBOR_COUNT)
            throw new IllegalStateException("Already reached maximum amount of neighbors.");
        neighbors.add(new Neighbor(neighborAddress, properties.antiSpamAbs()));
    }

    private void updateNeighborsBecausePropertiesChanged(Properties oldProp, Properties newProp) {
        // remove neighbors who are no longer neighbors
        List<Neighbor> toRemove = new LinkedList<>();
        for (Neighbor nb : neighbors)
            if (!newProp.neighbors().contains(nb.getAddress()))
                toRemove.add(nb);
        neighbors.removeAll(toRemove);

        // add neighbors who are new
        List<String> newNeighbors = new LinkedList<>();
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

        assert neighbors.size() == newProp.neighbors().size();
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
        round++;
        rounds.add(new Round());
        if (rounds.size() > Constants.MAX_AMOUNT_OF_ROUNDS_STORED)
            rounds.remove(0);

        if (round % 10 == 0)
            logHeader();
        // two separate FOR-loops to prevent delays between newRound() calls
        for (Neighbor neighbor : ict.getNeighbors()) {
            long tolerance = ict.getProperties().antiSpamAbs();
            neighbor.newRound(tolerance);
        }
        for (Neighbor neighbor : ict.getNeighbors())
            neighbor.resolveHost();
    }

    public class Round {
        public final int round = Node.this.round;
        public final long timestamp = System.currentTimeMillis();
        public final Stats[] stats;

        Round() {
            stats = new Stats[neighbors.size()];
            for(int i = 0; i < neighbors.size(); i++)
                stats[i] = new Stats(neighbors.get(i));
        }

        public JSONObject toJSON(Neighbor neighbor) {
            for(Stats s : stats)
                if(s.neighbor == neighbor)
                    return new JSONObject()
                        .put("timestamp", timestamp)
                            .put("requested", s.requestedTxs)
                            .put("ignored", s.ignoredTxs)
                            .put("invalid", s.invalidTxs)
                            .put("new", s.newTxs)
                            .put("all", s.allTxs);
            return null;
        }

        public class Stats {
            final Neighbor neighbor;
            final long ignoredTxs, newTxs, allTxs, invalidTxs, requestedTxs;

            Stats(Neighbor neighbor) {
                this.neighbor = neighbor;
                ignoredTxs = neighbor.stats.ignored;
                newTxs = neighbor.stats.receivedNew;
                allTxs = neighbor.stats.receivedAll;
                invalidTxs = neighbor.stats.receivedInvalid;
                requestedTxs = neighbor.stats.requested;
            }
        }
    }

    public List<Round> getRounds() {
        return rounds;
    }
}
