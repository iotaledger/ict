package org.iota.ict.ixi;

import org.iota.ict.model.Transaction;
import org.iota.ict.network.event.GossipListener;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class IxiModule implements Runnable, Ixi {

    private IctProxy proxy;
    private Set<GossipListener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<GossipListener, Boolean>());

    protected IxiModule(IctProxy proxy) {
        this.proxy = proxy;
        new Thread(this).start();
    }

    @Override
    public Set<Transaction> findTransactionsByAddress(String address) {
        return proxy.findTransactionsByAddress(address);
    }

    @Override
    public Set<Transaction> findTransactionsByTag(String tag) {
        return proxy.findTransactionsByTag(tag);
    }

    @Override
    public Transaction findTransactionByHash(String hash) {
        return proxy.findTransactionByHash(hash);
    }

    @Override
    public Transaction submit(String asciiMessage) {
        return proxy.submit(asciiMessage);
    }

    @Override
    public void submit(Transaction transaction) {
        proxy.submit(transaction);
    }

    @Override
    public void addGossipListener(GossipListener gossipListener) {
        listeners.add(gossipListener);
        proxy.addGossipListener(gossipListener);
    }

    @Override
    public void removeGossipListener(GossipListener gossipListener) {
        listeners.remove(gossipListener);
        proxy.removeGossipListener(gossipListener);
    }

    public void terminate() {
        for(GossipListener listener : listeners)
            proxy.removeGossipListener(listener);
    }
}