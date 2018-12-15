package org.iota.ict.network.event;

import org.iota.ict.model.Transaction;

import java.io.Serializable;

public class GossipSubmitEvent extends GossipEvent implements Serializable {
    private final Transaction transaction;

    public GossipSubmitEvent(Transaction transaction) {
        this.transaction = transaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }
}
