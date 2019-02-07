package org.iota.ict.network;

import org.iota.ict.model.Transaction;
import org.iota.ict.network.gossip.GossipListener;
import org.iota.ict.utils.properties.PropertiesUser;
import org.iota.ict.utils.interfaces.Restartable;

public interface SenderInterface extends Restartable, PropertiesUser, GossipListener {

    void request(String transactionHash);

    void queue(Transaction transaction);

    int queueSize();
}
