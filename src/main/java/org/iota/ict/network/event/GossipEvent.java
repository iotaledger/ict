package org.iota.ict.network.event;

import org.iota.ict.model.Transaction;

import java.io.Serializable;

public abstract class GossipEvent implements Serializable {

    private final Transaction transaction;

    public GossipEvent(Transaction transaction) {
        this.transaction = transaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }

}
