package org.iota.ict.network.gossip;

import org.iota.ict.model.transaction.Transaction;

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
