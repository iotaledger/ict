package org.iota.ict.network.event;

import org.iota.ict.model.Transaction;

import java.io.Serializable;

public class GossipSubmitEvent extends GossipEvent {

    public GossipSubmitEvent(Transaction transaction) {
        super(transaction);
    }

}
