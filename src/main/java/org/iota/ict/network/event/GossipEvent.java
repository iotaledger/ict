package org.iota.ict.network.event;

import org.iota.ict.model.Transaction;

import java.io.Serializable;

public class GossipEvent {

    private final Transaction transaction;
    private final boolean isOwnTransaction;

    public GossipEvent(Transaction transaction, boolean isOwnTransaction) {
        this.transaction = transaction;
        this.isOwnTransaction = isOwnTransaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public boolean isOwnTransaction() {
        return isOwnTransaction;
    }
}
