package org.iota.ict.network.event;

import org.iota.ict.model.Transaction;

import java.io.Serializable;

public class GossipReceiveEvent extends GossipEvent {

    public GossipReceiveEvent(Transaction transaction) {
        super(transaction);
    }

}
