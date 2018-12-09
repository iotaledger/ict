package org.iota.ict.network.event;

import org.iota.ict.model.Transaction;

public class GossipSentEvent extends GossipEvent {
    private final Transaction transaction;

    public GossipSentEvent(Transaction transaction) {
        this.transaction = transaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }
}
