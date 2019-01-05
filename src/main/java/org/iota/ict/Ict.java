package org.iota.ict;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.api.RestApi;
import org.iota.ict.ixi.IxiModuleHolder;
import org.iota.ict.model.RingTangle;
import org.iota.ict.model.Tangle;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.network.Neighbor;
import org.iota.ict.network.event.GossipEvent;
import org.iota.ict.network.event.GossipEventDispatcher;
import org.iota.ict.network.event.GossipListener;
import org.iota.ict.network.Receiver;
import org.iota.ict.network.Sender;
import org.iota.ict.model.Transaction;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.ErrorHandler;
import org.iota.ict.utils.Properties;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is the central component of the project. Each instance is an independent Ict node that can communicate with
 * other Icts. This class is not supposed to perform complex tasks but to delegate them to the correct submodule. It can
 * therefore be seen as a hub of all those components which, when working together, form an Ict node.
 */
public class Ict {

    protected final IxiModuleHolder moduleHolder;
    protected final List<Neighbor> neighbors = new LinkedList<>();
    protected final Sender sender;
    protected final Receiver receiver;
    protected State state;
    protected final Tangle tangle;
    protected final Properties properties;
    protected final DatagramSocket socket;
    protected final InetSocketAddress address;
    protected final GossipEventDispatcher eventDispatcher = new GossipEventDispatcher();
    public final static Logger LOGGER = LogManager.getLogger(Ict.class);
    protected int round;

    /**
     * @param properties The properties to use for this Ict. Changing them afterwards might or might not work for some properties.
     *                   TODO allow them to be configured afterwards.
     */
    public Ict(Properties properties) {

        this.properties = properties;
        this.tangle = new RingTangle(this, properties.tangleCapacity);
        this.address = new InetSocketAddress(properties.host, properties.port);

        for (InetSocketAddress neighborAddress : properties.neighbors)
            neighbor(neighborAddress);

        try {
            this.socket = new DatagramSocket(address);
        } catch (SocketException socketException) {
            ErrorHandler.handleError(LOGGER, socketException, "could not create socket for Ict");
            throw new RuntimeException(socketException);
        }

        this.sender = new Sender(this, properties, tangle, socket);
        this.receiver = new Receiver(this, tangle, socket);
        this.moduleHolder = new IxiModuleHolder(this);

        state = new StateRunning();
        eventDispatcher.start();
        sender.start();
        receiver.start();

        if(properties.guiEnabled)
            new RestApi(this);
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

    public void unneighbor(Neighbor neighbor) {
        neighbors.remove(neighbor);
    }

    /**
     * Adds a listener to this object. Every {@link GossipEvent} will be passed on to the listener.
     *
     * @param gossipListener The listener to add.
     */
    public void addGossipListener(GossipListener gossipListener) {
        eventDispatcher.listeners.add(gossipListener);
    }

    public void removeGossipListener(GossipListener gossipListener) {
        eventDispatcher.listeners.remove(gossipListener);
    }

    /**
     * @return The address of this node. Required by other nodes to neighbor.
     */
    public InetSocketAddress getAddress() {
        return address;
    }

    /**
     * @return A list containing all neighbors. This list is a copy: manipulating it directly will have no effects.
     */
    public List<Neighbor> getNeighbors() {
        return new LinkedList<>(neighbors);
    }

    public Properties getProperties() {
        return properties;
    }

    public Tangle getTangle() {
        return tangle;
    }

    /**
     * Submits a new message to the protocol. The message will be packaged as a Transaction and sent to all neighbors.
     *
     * @param asciiMessage ASCII encoded message which will be encoded to trytes and used as transaction message.
     * @return Hash of sent transaction.
     */
    public Transaction submit(String asciiMessage) {
        TransactionBuilder builder = new TransactionBuilder();
        builder.asciiMessage(asciiMessage);
        Transaction transaction = builder.build();
        submit(transaction);
        return transaction;
    }

    /**
     * Submits a new transaction to the protocol. It will be sent to all neighbors.
     *
     * @param transaction Transaction to submit.
     */
    public void submit(Transaction transaction) {
        tangle.createTransactionLogIfAbsent(transaction);
        sender.queueTransaction(transaction);
        notifyListeners(new GossipEvent(transaction, true));
    }

    public void broadcast(Transaction transaction) {
        sender.queueTransaction(transaction);
    }

    public void notifyListeners(GossipEvent event) {
        eventDispatcher.notifyListeners(event);
    }

    public void request(String requestedHash) {
        sender.request(requestedHash);
    }

    /**
     * @return Whether the Ict node is currently active/running.
     */
    public boolean isRunning() {
        return state instanceof StateRunning;
    }

    public void newRound() {
        Neighbor.newRound(this, round);
        if (properties.spamEnabled) {
            String spamHash = submit("spam transaction from node '" + properties.name + "'").hash;
            LOGGER.info("submitted spam transaction: " + spamHash);
        }
        round++;
    }

    public IxiModuleHolder getModuleHolder() {
        return moduleHolder;
    }

    public void terminate() {
        state.terminate();
        // TODO block until terminated
    }

    private class State {
        protected final String name;

        private State(String name) {
            this.name = name;
        }

        private void throwIllegalStateException(String actionName) {
            throw new IllegalStateException("Action '" + actionName + "' cannot be performed from state '" + name + "'.");
        }

        void terminate() {
            throwIllegalStateException("terminate");
        }
    }

    private class StateRunning extends State {
        private StateRunning() {
            super("running");
        }

        @Override
        void terminate() {
            state = new StateTerminating();
            socket.close();
            sender.terminate();
            receiver.interrupt();
            eventDispatcher.terminate();
            moduleHolder.terminate();
        }
    }

    private class StateTerminating extends State {
        private StateTerminating() {
            super("terminating");
        }
    }
}
