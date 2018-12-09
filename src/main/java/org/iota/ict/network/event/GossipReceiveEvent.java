package org.iota.ict.network.event;

import org.iota.ict.model.Transaction;

public class GossipReceiveEvent extends GossipEvent {
    private final Transaction transaction;

    public GossipReceiveEvent(Transaction transaction) {
        this.transaction = transaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }
}
