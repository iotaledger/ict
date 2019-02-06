package org.iota.ict;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.api.RestApi;
import org.iota.ict.ixi.IxiModuleHolder;
import org.iota.ict.model.RingTangle;
import org.iota.ict.model.Tangle;
import org.iota.ict.network.Neighbor;
import org.iota.ict.network.Node;
import org.iota.ict.network.event.GossipEvent;
import org.iota.ict.network.event.GossipEventDispatcher;
import org.iota.ict.network.event.GossipListener;
import org.iota.ict.model.Transaction;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Updater;
import org.iota.ict.utils.properties.FinalProperties;
import org.iota.ict.utils.RestartableThread;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * This class is the central component of the project. Each instance is an independent Ict node that can communicate with
 * other Icts. This class is not supposed to perform complex tasks but to delegate them to the correct submodule. It can
 * therefore be seen as a hub of all those components which, when working together, form an Ict node.
 */
public class Ict extends RestartableThread implements IctInterface {

    // services
    protected final GossipEventDispatcher eventDispatcher = new GossipEventDispatcher();
    protected final IxiModuleHolder moduleHolder = new IxiModuleHolder(Ict.this);
    protected final RestApi restApi;
    protected final Tangle tangle;

    // network
    protected final Node node;

    // inner state
    protected FinalProperties properties;
    public final static Logger LOGGER = LogManager.getLogger("Ict");
    protected long roundStart = System.currentTimeMillis();
    protected static long lastCheckedForUpdate = System.currentTimeMillis();

    protected Object notifySyncObject = new Object();

    /**
     * @param properties The properties to use for this Ict. To change or replace them, use {@link #updateProperties(FinalProperties)}.
     */
    public Ict(FinalProperties properties) {
        super(LOGGER);

        this.properties = properties;
        this.node = new Node(this);
        this.tangle = new RingTangle(this);
        this.restApi = new RestApi(this);

        subWorkers.add(eventDispatcher);
        subWorkers.add(node);
        subWorkers.add(moduleHolder);
        subWorkers.add(restApi);

        start();
    }

    @Override
    public void run() {
        while (isRunning()) {
            synchronized (notifySyncObject) {
                try {
                    notifySyncObject.wait(Math.max(1, Math.min(roundStart + properties.roundDuration() - System.currentTimeMillis(), 30000)));
                } catch (InterruptedException e) {
                }
            }
            if (roundStart + properties.roundDuration() < System.currentTimeMillis()) {
                node.newRound();
                LOGGER.debug("memory: " + Runtime.getRuntime().totalMemory() / 1024 / 1024 + "MB / " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "MB (total/max)");
                LOGGER.debug("tangle size: " + tangle.size() + " (" + Transaction.getAmountOfInstances() + " transaction instances alive)");
                node.log();
                eventDispatcher.log();
                roundStart = System.currentTimeMillis();
            }
            Updater.checkForUpdatesIfYouHaveNotDoneSoInALongTime();
        }
    }

    @Override
    public void onTerminate() {
        synchronized (notifySyncObject) {
            notifySyncObject.notify(); /* stop run() */
        }
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
        return node.getAddress();
    }

    /**
     * @return A list containing all neighbors. This list is a copy: manipulating it directly will have no effects.
     */
    public List<Neighbor> getNeighbors() {
        return new LinkedList<>(node.getNeighbors());
    }

    public FinalProperties getProperties() {
        return properties;
    }


    @Override
    public synchronized void updateProperties(FinalProperties newProp) {
        this.properties = newProp;
        restApi.updateProperties(this.properties);
        tangle.updateProperties(this.properties);
        node.updateProperties(this.properties);
        synchronized (notifySyncObject) {
            notifySyncObject.notify(); /* apply new round duration */
        }
    }

    /**
     * Submits a new transaction to the protocol. It will be sent to all neighbors.
     *
     * @param transaction Transaction to submit.
     */
    public void submit(Transaction transaction) {
        tangle.createTransactionLogIfAbsent(transaction);
        onGossipEvent(new GossipEvent(transaction, true));
    }

    public void request(String requestedHash) {
        node.request(requestedHash);
    }

    public void broadcast(Transaction transaction) {
        node.broadcast(transaction);
    }

    public IxiModuleHolder getModuleHolder() {
        return moduleHolder;
    }

    @Override
    public Set<Transaction> findTransactionsByAddress(String address) {
        return tangle.findTransactionsByAddress(address);
    }

    @Override
    public Set<Transaction> findTransactionsByTag(String tag) {
        return tangle.findTransactionsByTag(tag);
    }

    @Override
    public Transaction findTransactionByHash(String hash) {
        return tangle.findTransactionByHash(hash);
    }

    @Override
    public Tangle getTangle() {
        return tangle;
    }

    @Override
    public void onGossipEvent(GossipEvent event) {
        eventDispatcher.notifyListeners(event);
    }

    @Override
    public List<Node.Round> getRounds() {
        return node.getRounds();
    }
}
