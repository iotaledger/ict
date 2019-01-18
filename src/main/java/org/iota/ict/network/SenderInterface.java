package org.iota.ict.network;

import org.iota.ict.model.Transaction;
import org.iota.ict.network.event.GossipListener;
import org.iota.ict.utils.PropertiesUser;
import org.iota.ict.utils.Restartable;

public interface SenderInterface extends Restartable, PropertiesUser, GossipListener {

    void request(String transactionHash);
    void queue(Transaction transaction);
}
