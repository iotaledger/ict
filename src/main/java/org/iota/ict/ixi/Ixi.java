package org.iota.ict.ixi;

import org.iota.ict.model.Transaction;
import org.iota.ict.network.gossip.GossipListener;
import org.iota.ict.network.gossip.GossipPreprocessor;

import java.util.Set;

public interface Ixi {

    Set<Transaction> findTransactionsByAddress(String address);

    Set<Transaction> findTransactionsByTag(String tag);

    Transaction findTransactionByHash(String hash);

    void submit(Transaction transaction);

    void addGossipListener(GossipListener gossipListener);

    void removeGossipListener(GossipListener gossipListener);

    void addGossipPreprocessor(GossipPreprocessor gossipPreprocessor);

    void removeGossipPreprocessor(GossipPreprocessor gossipPreprocessor);

    double determineApprovalConfidence(Transaction transaction);
}
