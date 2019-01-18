package org.iota.ict.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.IctInterface;
import org.iota.ict.model.Transaction;
import org.iota.ict.utils.*;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;

public class Node extends RestartableThread implements PropertiesUser {

    protected final static Logger LOGGER = LogManager.getLogger(Node.class);
    protected final IctInterface ict;

    protected final List<Neighbor> neighbors = new LinkedList<>();
    protected final SenderInterface sender;
    protected final Restartable receiver;
    protected Properties properties;

    protected InetSocketAddress address;
    protected DatagramSocket socket;

    public Node(IctInterface ict) {
        super(LOGGER);
        this.ict = ict;
        this.properties = ict.getCopyOfProperties();
        this.receiver = new Receiver(this);
        this.sender = new Sender(this, properties.clone());

        ict.addGossipListener(sender);
        subWorkers.add(receiver);
        subWorkers.add(sender);
    }

    @Override
    public void onStart() {
        this.address = new InetSocketAddress(properties.host, properties.port);
        this.socket = createDatagramSocket(address);
    }

    @Override
    public void onTerminated() {
        if(!socket.isClosed())
            socket.close();
        socket = null;
    }

    @Override
    public void run() { }

    @Override
    public void updateProperties(Properties newProperties) {
        Properties oldProperties = properties;
        this.properties = newProperties.clone();
        updateNeighborsBecausePropertiesChanged(oldProperties, newProperties);

        if(address.getPort() != newProperties.port || !address.getHostName().equals(newProperties.host))
            updateHostAndPort(newProperties.host, newProperties.port);

        sender.updateProperties(newProperties);
    }

    private void updateHostAndPort(String newHost, int newPort) {
        address = new InetSocketAddress(newHost, newPort);
        try {
            socket.close();
            socket.connect(address);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
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

    /**
     * Opens a new connection to a neighbor. Both nodes will directly gossip transactions.
     *
     * @param neighborAddress Address of neighbor to connect to.
     * @throws IllegalStateException If already has {@link Constants#MAX_NEIGHBOR_COUNT} neighbors.
     */
    public void neighbor(InetSocketAddress neighborAddress) {
        if (neighbors.size() >= Constants.MAX_NEIGHBOR_COUNT)
            throw new IllegalStateException("Already reached maximum amount of neighbors.");
        neighbors.add(new Neighbor(neighborAddress, properties.antiSpamAbs));
    }

    private void updateNeighborsBecausePropertiesChanged(Properties oldProp, Properties newProp) {
        // remove neighbors who are no longer neighbors
        List<Neighbor> toRemove = new LinkedList<>();
        for(Neighbor nb : neighbors)
            if(!newProp.neighbors.contains(nb.getAddress()))
                toRemove.add(nb);
        neighbors.removeAll(toRemove);

        // add neighbors who are new
        List<InetSocketAddress> newNeighbors = new LinkedList<>();
        for(InetSocketAddress inetAddress : newProp.neighbors) {
            if(oldProp == null  || !oldProp.neighbors.contains(inetAddress))
                newNeighbors.add(inetAddress);
        }
        for(InetSocketAddress toAdd : newNeighbors)
            neighbor(toAdd);

        assert neighbors.size() == newProp.neighbors.size();
    }

    private static DatagramSocket createDatagramSocket(InetSocketAddress address) {
        try {
            return new DatagramSocket(address);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }
}
