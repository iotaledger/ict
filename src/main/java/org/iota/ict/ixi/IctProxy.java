package org.iota.ict.ixi;

import org.iota.ict.Ict;
import org.iota.ict.model.Transaction;
import org.iota.ict.network.event.GossipListener;

import java.util.Set;

public class IctProxy implements Ixi {

    private Ict ict;

    public IctProxy(Ict ict) {
        this.ict = ict;
    }

    @Override
    public Set<Transaction> findTransactionsByAddress(String address) {
        return ict.getTangle().findTransactionsByAddress(address);
    }

    @Override
    public Set<Transaction> findTransactionsByTag(String tag) {
        return ict.getTangle().findTransactionsByTag(tag);
    }

    @Override
    public Transaction findTransactionByHash(String hash) {
        return ict.getTangle().findTransactionByHash(hash);
    }

    @Override
    public Transaction submit(String asciiMessage) {
        return ict.submit(asciiMessage);
    }

    @Override
    public void submit(Transaction transaction) {
        ict.submit(transaction);
    }

    @Override
    public void addGossipListener(GossipListener gossipListener) {
        ict.addGossipListener(gossipListener);
    }

    @Override
    public void removeGossipListener(GossipListener gossipListener) {
        ict.removeGossipListener(gossipListener);
    }
}
